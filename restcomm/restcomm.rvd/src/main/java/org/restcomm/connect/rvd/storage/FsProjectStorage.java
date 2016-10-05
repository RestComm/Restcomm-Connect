/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.rvd.storage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.exceptions.project.ProjectException;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.model.client.Node;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.model.client.StateHeader;
import org.restcomm.connect.rvd.model.client.WavItem;
import org.restcomm.connect.rvd.storage.exceptions.BadProjectHeader;
import org.restcomm.connect.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.restcomm.connect.rvd.storage.exceptions.ProjectAlreadyExists;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;
import org.restcomm.connect.rvd.storage.exceptions.WavItemDoesNotExist;
import org.restcomm.connect.rvd.utils.Zipper;
import org.restcomm.connect.rvd.utils.exceptions.ZipperException;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.packaging.Rapp;
import org.restcomm.connect.rvd.model.server.ProjectOptions;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.restcomm.connect.rvd.model.ProjectSettings;
import org.restcomm.connect.rvd.model.RappItem;

/**
 * @author Orestis Tsakiridis
 */
public class FsProjectStorage {
    static final Logger logger = Logger.getLogger(FsProjectStorage.class.getName());

    public static List<String> listProjectNames(WorkspaceStorage workspaceStorage) throws BadWorkspaceDirectoryStructure {
        List<String> items = new ArrayList<String>();

        File workspaceDir = new File(workspaceStorage.rootPath );
        if (workspaceDir.exists()) {

            File[] entries = workspaceDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File anyfile) {
                    if (anyfile.isDirectory() && !anyfile.getName().startsWith(RvdConfiguration.PROTO_DIRECTORY_PREFIX) && !anyfile.getName().equals("@users") )
                        return true;
                    return false;
                }
            });
            Arrays.sort(entries, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    File statefile1 = new File(f1.getAbsolutePath() + File.separator + "state");
                    File statefile2 = new File(f2.getAbsolutePath() + File.separator + "state");
                    if ( statefile1.exists() && statefile2.exists() )
                        return Long.valueOf(statefile2.lastModified()).compareTo(statefile1.lastModified());
                    else
                        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                }
            });

            for (File entry : entries) {
                items.add(entry.getName());
            }
        } else
            throw new BadWorkspaceDirectoryStructure();

        return items;
    }

    public static InputStream getWav(String projectName, String filename, WorkspaceStorage workspaceStorage) throws StorageException {
        try {
            return workspaceStorage.loadStream(RvdConfiguration.WAVS_DIRECTORY_NAME + File.separator + filename, projectName);
        } catch (StorageEntityNotFound e) {
            throw new WavItemDoesNotExist("Wav file does not exist - " + filename, e);
        }
    }

    public static String loadBootstrapInfo(String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        return workspaceStorage.loadEntityString("bootstrap", projectName);
    }

    public static void storeBootstrapInfo(String bootstrapInfo, String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        workspaceStorage.storeEntityString(bootstrapInfo, "bootstrap", projectName);
    }

    public static boolean hasBootstrapInfo(String projectName, WorkspaceStorage workspaceStorage) {
        return workspaceStorage.entityExists("bootstrap", projectName);
    }

    public static boolean hasRasInfo(String projectName, WorkspaceStorage workspaceStorage) {
        return workspaceStorage.entityExists("ras", projectName);
    }

    public static boolean hasPackagingInfo(String projectName, WorkspaceStorage workspaceStorage) {
        return workspaceStorage.entityExists("packaging", projectName);
    }

    public static Rapp loadRappFromPackaging(String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        return workspaceStorage.loadEntity("rapp", projectName+"/packaging", Rapp.class);
    }

    /**
     * Creates a list of rapp info objects out of a set of projects
     * @param projectNames
     * @return
     * @throws StorageException
     * @throws ProjectException
     */
    public static List<RappItem> listRapps(List<String> projectNames, WorkspaceStorage workspaceStorage, ProjectService projectService) throws StorageException, ProjectException {
        List<RappItem> rapps = new ArrayList<RappItem>();
        for (String projectName : projectNames) {
            RappItem item = new RappItem();
            item.setProjectName(projectName);

            if ( FsProjectStorage.hasRasInfo(projectName, workspaceStorage) ) {
                item.setWasImported(true);

                // load info from rapp file
                Rapp rapp = workspaceStorage.loadEntity("rapp", projectName+"/ras", Rapp.class);
                item.setRappInfo(rapp.getInfo());

                // app status
                boolean installedStatus = true;
                boolean configuredStatus = FsProjectStorage.hasBootstrapInfo(projectName, workspaceStorage);
                boolean activeStatus = installedStatus && configuredStatus;
                RappItem.RappStatus[] statuses = new RappItem.RappStatus[3];
                statuses[0] = RappItem.RappStatus.Installed; // always set
                statuses[1] = configuredStatus ? RappItem.RappStatus.Configured : RappItem.RappStatus.Unconfigured;
                statuses[2] = activeStatus ? RappItem.RappStatus.Active : RappItem.RappStatus.Inactive;
                item.setStatus(statuses);
            } else
                item.setWasImported(false);

            if ( FsProjectStorage.hasPackagingInfo(projectName, workspaceStorage) ) {
                item.setHasPackaging(true);

                // load info from rapp file
                Rapp rapp = workspaceStorage.loadEntity("rapp", projectName+"/packaging", Rapp.class);
                item.setRappInfo(rapp.getInfo());

                // app status
                /*boolean installedStatus = true;
                boolean configuredStatus = FsProjectStorage.hasBootstrapInfo(projectName, workspaceStorage);
                boolean activeStatus = installedStatus && configuredStatus;
                RappStatus[] statuses = new RappStatus[3];
                statuses[0] = RappStatus.Installed; // always set
                statuses[1] = configuredStatus ? RappStatus.Configured : RappStatus.Unconfigured;
                statuses[2] = activeStatus ? RappStatus.Active : RappStatus.Inactive;
                item.setStatus(statuses);
                */
            } else
                item.setHasPackaging(false);

            item.setHasBootstrap(FsProjectStorage.hasBootstrapInfo(projectName, workspaceStorage));
            item.setStartUrl(projectService.buildStartUrl(projectName));

            rapps.add(item);
        }
        return rapps;
    }

    public static ProjectOptions loadProjectOptions(String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        ProjectOptions projectOptions = workspaceStorage.loadEntity("project", projectName+"/data", ProjectOptions.class);
        return projectOptions;
    }

    public static void storeProjectOptions(ProjectOptions projectOptions, String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        workspaceStorage.storeEntity(projectOptions, ProjectOptions.class, "project", projectName+"/data");
    }

    public static void storeNodeStepnames(Node node, String projectName, WorkspaceStorage storage) throws StorageException {
        List<String> stepnames = new ArrayList<String>();
        for ( Step step : node.getSteps() ) {
            stepnames.add(step.getName());
        }
        storage.storeEntity(stepnames, node.getName()+".node", projectName+"/data");
    }

    public static List<String> loadNodeStepnames(String projectName, String nodeName, WorkspaceStorage storage) throws StorageException {
        List<String> stepnames = storage.loadEntity(nodeName+".node", projectName+"/data", new TypeToken<List<String>>(){}.getType());
        return stepnames;
    }

    public static void storeNodeStep(Step step, Node node, String projectName, WorkspaceStorage storage) throws StorageException {
        storage.storeEntity(step, node.getName()+"."+step.getName(), projectName+"/data/");
    }

    public static ProjectSettings loadProjectSettings(String projectName, WorkspaceStorage storage) throws StorageException {
        return storage.loadEntity("settings", projectName, ProjectSettings.class);
    }

    public static void storeProjectSettings(ProjectSettings projectSettings, String projectName, WorkspaceStorage storage) throws StorageException {
        storage.storeEntity(projectSettings, "settings", projectName);
    }

    /*
    @Override
    public
    ProjectState loadProject(String name) throws StorageException {
        String stateData = storageBase.loadProjectFile(name, ".", "state");
        return marshaler.toModel(stateData, ProjectState.class);
    }
    */
    public static ProjectState loadProject(String projectName, WorkspaceStorage storage) throws StorageException {
        return storage.loadEntity("state", projectName, ProjectState.class);
    }

    public static String loadProjectString(String projectName, WorkspaceStorage storage) throws StorageException {
        return storage.loadEntityString("state", projectName);
    }

    private static void buildDirStructure(ProjectState state, String name, WorkspaceStorage storage) {
        if ("voice".equals(state.getHeader().getProjectKind()) ) {
            File wavsDir = new File(  storage.rootPath + "/" + name + "/" + "wavs" );
            wavsDir.mkdir();
        }
    }

    public static void storeProject(boolean firstTime, ProjectState state, String projectName, WorkspaceStorage storage) throws StorageException {
        storage.storeEntity(state, "state", projectName);
        if (firstTime)
            buildDirStructure(state, projectName, storage);

    }

    public static StateHeader loadStateHeader(String projectName, WorkspaceStorage storage) throws StorageException {
        String stateData = storage.loadEntityString("state", projectName);
        JsonParser parser = new JsonParser();
        JsonElement header_element = parser.parse(stateData).getAsJsonObject().get("header");
        if ( header_element == null )
            throw new BadProjectHeader("No header found. This is probably an old project");

        Gson gson = new Gson();
        StateHeader header = gson.fromJson(header_element, StateHeader.class);

        return header;
    }

    public static boolean projectExists(String projectName, WorkspaceStorage workspaceStorage) {
        return workspaceStorage.entityExists(projectName, "");
    }

    public static void createProjectSlot(String projectName, WorkspaceStorage storage) throws StorageException {
        if ( projectExists(projectName, storage) )
            throw new ProjectAlreadyExists("Project '" + projectName + "' already exists");

        //String projectPath = storageBase.getWorkspaceBasePath()  +  File.separator + projectName;
        String projectPath = storage.rootPath  +  File.separator + projectName;
        File projectDirectory = new File(projectPath);
        if ( !projectDirectory.mkdir() )
            throw new StorageException("Cannot create project directory. Don't know why - " + projectDirectory );

    }

    public static void renameProject(String projectName, String newProjectName, WorkspaceStorage storage) throws StorageException {
        try {
            File sourceDir = new File(storage.rootPath  + File.separator + projectName);
            File destDir = new File(storage.rootPath  + File.separator + newProjectName);
            FileUtils.moveDirectory(sourceDir, destDir);
        } catch (IOException e) {
            throw new StorageException("Error renaming directory '" + projectName + "' to '" + newProjectName + "'");
        }
    }

    public static void deleteProject(String projectName, WorkspaceStorage storage) throws StorageException {
        try {
            File projectDir = new File(storage.rootPath  + File.separator + projectName);
            FileUtils.deleteDirectory(projectDir);
        } catch (IOException e) {
            throw new StorageException("Error removing directory '" + projectName + "'", e);
        }
    }

    public static InputStream archiveProject(String projectName, WorkspaceStorage storage) throws StorageException {
        String path = storage.rootPath + File.separator + projectName; //storageBase.getProjectBasePath(projectName);
        File tempFile;
        try {
            tempFile = File.createTempFile("RVDprojectArchive",".zip");
        } catch (IOException e1) {
            throw new StorageException("Error creating temp file for archiving project " + projectName, e1);
        }

        InputStream archiveStream;
        try {
            Zipper zipper = new Zipper(tempFile);
            zipper.addDirectoryRecursively(path, false);
            zipper.finish();

            // open a stream on this file
            archiveStream = new FileInputStream(tempFile);
            return archiveStream;
        } catch (ZipperException e) {
            throw new StorageException( "Error archiving " + projectName, e);
        } catch (FileNotFoundException e) {
            throw new StorageException("This is weird. Can't find the temp file i just created for archiving project " + projectName, e);
        } finally {
            // Always delete the file. The underlying file content still exists because the archiveStream refers to it (for Linux only). It will be deleted when the stream is closed
            tempFile.delete();
        }
    }

    /**
     * Returns an non-existing project name based on the given one. Ideally it returns the same name. If null or blank
     * project name given the 'Untitled' name is tried.
     * @throws StorageException in case the first 50 project names tried are already occupied
     */
    public static String getAvailableProjectName(String projectName, WorkspaceStorage storage) throws StorageException {
        if ( projectName == null || "".equals(projectName) )
            projectName = "Unititled";

        String baseProjectName = projectName;
        int counter = 1;
        while (true && counter < 50) { // try up to 50 times, no more
            if ( ! projectExists(projectName,storage) )
                return projectName;
            projectName = baseProjectName + " " +  counter;
            counter ++;
        }

        throw new StorageException("Can't find an available project name for base name '" + projectName + "'");
    }

    public static void importProjectFromDirectory(File sourceProjectDirectory, String projectName, boolean overwrite, WorkspaceStorage storage) throws StorageException {
        try {
            createProjectSlot(projectName, storage);
        } catch (ProjectAlreadyExists e) {
            if ( !overwrite )
                throw e;
            else {
                File destProjectDirectory = new File(storage.rootPath + File.separator + projectName);
                try {
                    FileUtils.cleanDirectory(destProjectDirectory);
                    FileUtils.copyDirectory(sourceProjectDirectory, destProjectDirectory);
                } catch (IOException e1) {
                    throw new StorageException("Error importing project '" + projectName + "' from directory: " + sourceProjectDirectory);
                }
            }
        }
    }

    private static String getProjectBasePath(String projectName, WorkspaceStorage storage) {
        return storage.rootPath + File.separator + projectName;
    }

    private static String getProjectWavsPath( String projectName, WorkspaceStorage storage ) {
        return getProjectBasePath(projectName,storage) + File.separator + RvdConfiguration.WAVS_DIRECTORY_NAME;
    }

    public static void storeWav(String projectName, String wavname, File sourceWavFile, WorkspaceStorage storage) throws StorageException {
        String destWavPathname = getProjectWavsPath(projectName,storage) + File.separator + wavname;
        try {
            FileUtils.copyFile(sourceWavFile, new File(destWavPathname));
        } catch (IOException e) {
            throw new StorageException( "Error coping wav file into project " + projectName + ": " + sourceWavFile + " -> " + destWavPathname, e );
        }
    }

    public static void storeWav(String projectName, String wavname, InputStream wavStream, WorkspaceStorage storage) throws StorageException {
        String wavPathname = getProjectWavsPath(projectName, storage) + File.separator + wavname;
        if(logger.isDebugEnabled()) {
            logger.debug( "Writing wav file to " + wavPathname);
        }
        try {
            FileUtils.copyInputStreamToFile(wavStream, new File(wavPathname) );
        } catch (IOException e) {
            throw new StorageException("Error writing to " + wavPathname, e);
        }
    }

    public static List<WavItem> listWavs(String projectName, WorkspaceStorage storage) throws StorageException {
        List<WavItem> items = new ArrayList<WavItem>();

        //File workspaceDir = new File(workspaceBasePath + File.separator + appName + File.separator + "wavs");
        File wavsDir = new File(getProjectWavsPath(projectName,storage));
        if (wavsDir.exists()) {

            File[] entries = wavsDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File anyfile) {
                    if (anyfile.isFile())
                        return true;
                    return false;
                }
            });
            Arrays.sort(entries, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()) ;
                }
            });

            for (File entry : entries) {
                WavItem item = new WavItem();
                item.setFilename(entry.getName());
                items.add(item);
            }
        }// else
            //throw new BadWorkspaceDirectoryStructure();

        return items;
    }

    /**
     * Returns a WavItem list for all .wav files insude the /audio RVD directory. No project is involved here.
     * @param rvdContext
     * @return
     */
    public static List<WavItem> listBundledWavs( RvdContext rvdContext ) {
        List<WavItem> items = new ArrayList<WavItem>();

        String contextRealPath = rvdContext.getServletContext().getRealPath("/");
        String audioRealPath = contextRealPath + "audio";
        String contextPath = rvdContext.getServletContext().getContextPath();

        File dir = new File(audioRealPath);
        Collection<File> audioFiles = FileUtils.listFiles(dir, new SuffixFileFilter(".wav"), TrueFileFilter.INSTANCE );
        for (File anyFile: audioFiles) {
            WavItem item = new WavItem();
            String itemRelativePath = anyFile.getPath().substring(contextRealPath.length());
            String presentationName = anyFile.getPath().substring(contextRealPath.length() + "audio".length() );
            item.setUrl( contextPath + "/" + itemRelativePath );
            item.setFilename(presentationName);
            items.add(item);
        }
        return items;
    }

    public static void deleteWav(String projectName, String wavname, WorkspaceStorage storage) throws WavItemDoesNotExist {
        String filepath = getProjectWavsPath(projectName, storage) + File.separator + wavname;
        File wavfile = new File(filepath);
        if ( wavfile.delete() ) {
            if(logger.isDebugEnabled()) {
                logger.info( "Deleted " + wavname + " from " + projectName + " app" );
            }
        }
        else {
            //logger.warn( "Cannot delete " + wavname + " from " + projectName + " app" );
            throw new WavItemDoesNotExist("Wav file does not exist - " + filepath );
        }

    }

    public static void storeRapp(Rapp rapp, String projectName, WorkspaceStorage storage) throws StorageException {
        storage.storeEntity(rapp, rapp.getClass(), "rapp", projectName + "/ras");
    }

    public static Rapp loadRapp(String projectName, WorkspaceStorage storage) throws StorageException {
        return storage.loadEntity("rapp", projectName+"/ras", Rapp.class);
    }

    public static void backupProjectState(String projectName, WorkspaceStorage storage) throws StorageException {
        File sourceStateFile = new File(storage.rootPath + File.separator + projectName + File.separator + "state");
        File backupStateFile = new File(storage.rootPath + File.separator + projectName + File.separator + "state" + ".old");

        try {
            FileUtils.copyFile(sourceStateFile, backupStateFile);
        } catch (IOException e) {
            throw new StorageException("Error creating state file backup: " + backupStateFile);
        }
    }

    public static void updateProjectState(String projectName, String newState, WorkspaceStorage storage) throws StorageException {
        FileOutputStream stateFile_os;
        try {
            stateFile_os = new FileOutputStream(storage.rootPath + File.separator + projectName + File.separator + "state");
            IOUtils.write(newState, stateFile_os, Charset.forName("UTF-8"));
            stateFile_os.close();
        } catch (FileNotFoundException e) {
            throw new StorageException("Error updating state file for project '" + projectName + "'", e);
        } catch (IOException e) {
            throw new StorageException("Error updating state file for project '" + projectName + "'", e);
        }
    }

    public static String loadStep(String projectName, String nodeName, String stepName, WorkspaceStorage storage) throws StorageException {
        //return storageBase.loadProjectFile(projectName, "data", nodeName + "." + stepName);
        return storage.loadEntityString(nodeName + "." + stepName, projectName+"/data");
    }
}



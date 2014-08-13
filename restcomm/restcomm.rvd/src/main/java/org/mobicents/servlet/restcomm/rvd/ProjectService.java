package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.ProjectValidator;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.ValidationResult;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationException;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationFrameworkException;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.storage.FsStorageBase;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.utils.Unzipper;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProjectService {

    static final Logger logger = Logger.getLogger(ProjectService.class.getName());

    private ServletContext servletContext; // TODO we have to find way other that directly through constructor parameter.

    ProjectStorage projectStorage;
    RvdConfiguration settings;
    RvdContext rvdContext;

    public ProjectService(RvdContext rvdContext) {
        this.rvdContext = rvdContext;
        this.servletContext = rvdContext.getServletContext();
        this.projectStorage = rvdContext.getProjectStorage();
        this.settings = rvdContext.getSettings();
    }


    /**
     * Builds the startUrl for an application based on the application name and the incoming httpRequest. It is depending on the
     * initial REST request that called this function. Usually this httpRequest comes either from user's browser when he runs
     * Admin-UI or RVD. However, this url will be used in Restcomm too. Make sure that Restcomm can access the generated the
     * same way client's browser does.
     *
     * @param projectName
     * @param httpRequest
     * @return An absolute url pointing to the starting URL of the application
     * @throws UnsupportedEncodingException
     * @throws URISyntaxException
     */
    public static String getStartUrlForProject(String projectName, HttpServletRequest httpRequest) throws URISyntaxException {
        URI startURI = new URI(httpRequest.getScheme(), null, httpRequest.getServerName(),
                (httpRequest.getServerPort() == 80 ? -1 : httpRequest.getServerPort()), httpRequest.getContextPath()
                        + httpRequest.getServletPath() + "/apps/" + projectName + "/controller", null, null);

        //logger.info("startURI.getPath(): " + startURI.getPath());
        //logger.info("startURI.getRawPath(): " + startURI.getRawPath());
        //logger.info("startURI.toASCIIString(): " + startURI.toASCIIString());
        return startURI.getRawPath();  //toASCIIString();
    }

    /**
     * Populates an application list with startup urls for each application
     *
     * @param items
     * @param httpRequest
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    public static void fillStartUrlsForProjects(List<ProjectItem> items, HttpServletRequest httpRequest)
            throws URISyntaxException {
        for (ProjectItem item : items) {
            item.setStartUrl(getStartUrlForProject(item.getName(), httpRequest));
        }
    }

    /**
     * Populates a list of ProjectItems each representing a project. The project kind property defaults to
     * 'voice' if it does not exist.
     * @throws StorageException
     */
    public List<ProjectItem> getAvailableProjects() throws StorageException {

        List<ProjectItem> items = new ArrayList<ProjectItem>();
        for (String entry : projectStorage.listProjectNames() ) {

            String kind = "voice";
            try {
                StateHeader header = projectStorage.loadStateHeader(entry);
                kind = header.getProjectKind();
            } catch ( BadProjectHeader e ) {
                // for old projects
                JsonParser parser = new JsonParser();
                JsonObject root_element = parser.parse(projectStorage.loadProjectState(entry)).getAsJsonObject();
                JsonElement projectKind_element = root_element.get("projectKind");
                if ( projectKind_element != null ) {
                    kind = projectKind_element.getAsString();
                }
            }

            ProjectItem item = new ProjectItem();
            item.setName(entry);
            item.setKind(kind);
            items.add(item);
        }
        return items;
    }

    /**
     * Returns the projects owned by ownerFilter (in addition to those that belong to none and are freely accessible). If ownerFilter is null only
     * freely accessible projectds are returned.
     * @param ownerFilter
     * @throws StorageException
     */
    public List<ProjectItem> getAvailableProjectsByOwner(String ownerFilter) throws StorageException {

        List<ProjectItem> items = new ArrayList<ProjectItem>();
        for (String entry : projectStorage.listProjectNames() ) {

            String kind = "voice";
            String owner = null;
            try {
                StateHeader header = projectStorage.loadStateHeader(entry);
                kind = header.getProjectKind();
                owner = header.getOwner();
            } catch ( BadProjectHeader e ) {
                // for old projects
                JsonParser parser = new JsonParser();
                JsonObject root_element = parser.parse(projectStorage.loadProjectState(entry)).getAsJsonObject();
                JsonElement projectKind_element = root_element.get("projectKind");
                if ( projectKind_element != null ) {
                    kind = projectKind_element.getAsString();
                }
            }

            if ( ownerFilter != null ) {
                if ( owner == null || owner.equals(ownerFilter) ) {
                    ProjectItem item = new ProjectItem();
                    item.setName(entry);
                    item.setKind(kind);
                    items.add(item);
                }
            } else {
                ProjectItem item = new ProjectItem();
                item.setName(entry);
                item.setKind(kind);
                items.add(item);
            }
        }
        return items;
    }

    public String openProject(String projectName) throws ProjectDoesNotExist, StorageException, IncompatibleProjectVersion {
        if ( !projectExists(projectName) )
            throw new ProjectDoesNotExist();

        try {
            StateHeader header = projectStorage.loadStateHeader(projectName);
            if ( ! header.getVersion().equals(RvdConfiguration.getRvdProjectVersion()) )
                throw new IncompatibleProjectVersion("Error loading project '" + projectName + "'. Project version: " + header.getVersion() + " - RVD project version: " + RvdConfiguration.getRvdProjectVersion() );
        } catch ( BadProjectHeader e ) {
            throw new IncompatibleProjectVersion("Bad or missing project header for project '" + projectName + "'");
        }

        return projectStorage.loadProjectState(projectName);
    }

    public boolean projectExists(String projectName) throws BadWorkspaceDirectoryStructure {
        return projectStorage.projectExists(projectName);
    }

    public ProjectState createProject(String projectName, String kind, String owner) throws StorageException, InvalidServiceParameters {
        if ( !"voice".equals(kind) && !"ussd".equals(kind) && !"sms".equals(kind) )
            throw new InvalidServiceParameters("Invalid project kind specified - '" + kind + "'");

        ProjectState state = null;
        if ( "voice".equals(kind) )
            state = ProjectState.createEmptyVoice(owner);
        else
        if ( "ussd".equals(kind) )
            state = ProjectState.createEmptyUssd(owner);
        else
        if ( "sms".equals(kind) )
            state = ProjectState.createEmptySms(owner);

        projectStorage.createProjectSlot(projectName);
        projectStorage.storeProject(projectName, state, true);
        return state;
    }

    public void updateProject(HttpServletRequest request, String projectName, ProjectState existingProject) throws IOException, StorageException, ValidationFrameworkException, ValidationException, IncompatibleProjectVersion {
        String stateData = IOUtils.toString(request.getInputStream());
        try {
            // first validate
            ProjectValidator validator = new ProjectValidator();
            ValidationResult result = validator.validate(stateData);
            if (!result.isSuccess())
                throw new ValidationException(result);
            // then save
            ProjectState state = rvdContext.getMarshaler().toModel(stateData, ProjectState.class);
            // preserve project owner
            state.getHeader().setOwner(existingProject.getHeader().getOwner());
            projectStorage.storeProject(projectName, state, false);
        } catch (ProcessingException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        }

    }

    public void renameProject(String projectName, String newProjectName) throws ProjectDoesNotExist, StorageException {
        if (  ! projectStorage.projectExists(projectName) ) {
            throw new ProjectDoesNotExist();
        } else if ( projectStorage.projectExists(newProjectName) ) {
            throw new ProjectDirectoryAlreadyExists();
        }
        projectStorage.renameProject(projectName, newProjectName);
    }

    public void deleteProject(String projectName) throws ProjectDoesNotExist, StorageException {
        if (! projectStorage.projectExists(projectName))
            throw new ProjectDoesNotExist();
        projectStorage.deleteProject(projectName);
    }

    public InputStream archiveProject(String projectName) throws StorageException {
        return projectStorage.archiveProject(projectName);
    }

    public String importProjectFromArchive(InputStream archiveStream, String archiveFilename) throws StorageException {
        File archiveFile = new File(archiveFilename);
        String projectName = FilenameUtils.getBaseName(archiveFile.getName());

        // First unzip to temp dir
        File tempProjectDir;
        try {
            tempProjectDir = RvdUtils.createTempDir();
        } catch (RvdException e) {
            throw new StorageException("Error importing project from archive. Cannot create temp directory for project: " + projectName, e );
        }
        Unzipper unzipper = new Unzipper(tempProjectDir);
        unzipper.unzip(archiveStream);

        // Then try to load in case we got garbage
        FsStorageBase storageBase = new FsStorageBase(tempProjectDir.getParent(), projectStorage.getMarshaler());
        ProjectState state = storageBase.loadModelFromFile(tempProjectDir.getPath() + File.separator + "state", ProjectState.class);

        // TODO Make these an atomic action!
        projectName = projectStorage.getAvailableProjectName(projectName);
        projectStorage.createProjectSlot(projectName);

        projectStorage.importProjectFromDirectory(tempProjectDir, projectName, true);

        return projectName;
    }

    public void addWavToProject(String projectName, String wavName, InputStream wavStream) throws StorageException {
        projectStorage.storeWav(projectName, wavName, wavStream);
    }

    public List<WavItem> getWavs(String appName) throws StorageException {
        return projectStorage.listWavs(appName);
    }

    public void removeWavFromProject(String projectName, String wavName) throws WavItemDoesNotExist {
        projectStorage.deleteWav(projectName, wavName);
    }




    /**
     * Loads the project specified into an rvd project object
     * @param projectName
     * @return
     * @throws RvdException
     */
    public RvdProject load(String projectName) throws RvdException {
        String projectJson = projectStorage.loadProjectState(projectName);
        RvdProject project = RvdProject.fromJson(projectName, projectJson);
        return project;
    }

}

package org.mobicents.servlet.restcomm.rvd.ras;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappBinaryInfo;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappInfo;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.ras.exceptions.RasException;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.utils.Unzipper;
import org.mobicents.servlet.restcomm.rvd.utils.Zipper;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.RvdValidationException;

import com.google.gson.Gson;

/**
 * Functionality for importing and setting up an app from the app store
 * @author "Tsakiridis Orestis"
 *
 */
public class RasService {

    static final Logger logger = Logger.getLogger(RasService.class.getName());

    ProjectStorage storage;

    public RasService(ProjectStorage storage) {
        this.storage = storage;
    }

    /*
    public void saveRappConfig(RappConfig rappConfig, String projectName) throws RvdException {
        // validate here...
        storage.storeRappConfig(rappConfig, projectName);
    }
    */

    /**
     * Builds a RappConfig object out of its JSON representation
     *
     */
    public RappConfig toModel(Class<RappConfig> clazz, String data) {
        Gson gson = new Gson();
        RappConfig rappConfig = gson.fromJson(data, RappConfig.class);
        return rappConfig;
    }


    public InputStream createZipPackage(RvdProject project) throws RvdException {

        logger.debug("Creating zip package for project " + project.getName());
        String projectName = project.getName();

        // extract info and config parts of a Rapp
        Gson gson = new Gson();
        Rapp rapp = storage.loadRapp(projectName);
        String configData = gson.toJson(rapp.getConfig());
        String infoData = gson.toJson(rapp.getInfo());

        // create the zip bundle
        try {
            File tempFile = File.createTempFile("rapp",".tmp");

            Zipper zipper = new Zipper(tempFile);
            try {
                zipper.addDirectory("/app/");
                zipper.addFileContent("/app/info", infoData );
                zipper.addFileContent("/app/config", configData );
                zipper.addDirectory("/app/rvd/");
                zipper.addFileContent("/app/rvd/state", storage.loadProjectState(projectName));

                if ( project.supportsWavs() ) {
                    zipper.addDirectory("/app/rvd/wavs/");
                    for ( WavItem wavItem : storage.listWavs(projectName) ) {
                        InputStream wavStream = storage.getWav(projectName, wavItem.getFilename());
                        try {
                            zipper.addFile("app/rvd/wavs/" + wavItem.getFilename(), wavStream );
                        } finally {
                            wavStream.close();
                        }
                    }
                }


            } finally {
                zipper.finish();
            }

            storage.storeRappBinary(projectName, tempFile);
            // TODO - if FsProjectStorage  is not used, the temporaty file should still be removed (in this case it is not moved) !!!

            logger.debug("Zip package created for project " + projectName);

            return storage.getRappBinary(projectName);
        } catch (IOException e) {
            throw new PackagingException("Error creating temporaty zip file ", e);
        }

    }


    /**
     * Unzips the package stream in a temporary directory and creates an app out of it
     * @param packageZipStream
     * @return The name (some sort of identifier) of the new project created
     * @throws RvdException
     */
    public String importAppToWorkspace( InputStream packageZipStream ) throws RvdException {
        File tempDir = RvdUtils.createTempDir();
        logger.debug("Unzipping ras package to temporary directory " + tempDir.getPath());
        Unzipper unzipper = new Unzipper(tempDir);
        unzipper.unzip(packageZipStream);

        //String infoPath = tempDir.getPath() + "/app/" + "info";
        RappInfo info = storage.loadModelFromFile( tempDir.getPath() + "/app/" + "info", RappInfo.class );
        RappConfig config = storage.loadModelFromFile( tempDir.getPath() + "/app/" + "config", RappConfig.class );


        // create a project with the application name specified in the package. This should be a default. The user should be able to override it
        String newProjectName = storage.getAvailableProjectName(info.getName());
        storage.createProjectSlot(newProjectName);

        // add project state
        storage.storeProjectState(newProjectName, new File(tempDir.getPath() + "/app/rvd/state" ));

        // and wav files one-by-one (if any)
        File wavDir = new File(tempDir.getPath() + "/app/rvd/wavs");
        if ( wavDir.exists() ) {
            File[] wavFiles = wavDir.listFiles();
            for ( File wavFile : wavFiles ) {
                storage.storeWav(newProjectName, wavFile.getName(), wavFile);
            }
        }

        // Store rapp for later usage
        Rapp rapp = new Rapp(info, config);
        storage.storeRapp(rapp, newProjectName);

        // now remove temporary directory
        try {
            FileUtils.deleteDirectory(tempDir);
        } catch (IOException e) {
            logger.warn(new RasException("Error removing temporary directory after importing project '" + newProjectName + "'"));
        }

        return newProjectName;
    }

    public void saveApp(Rapp rapp, String projectName) throws RvdValidationException, StorageException {
        ValidationReport report = rapp.validate();
        if ( ! report.isOk() )
            throw new RvdValidationException("Cannot validate rapp", report);

        storage.storeRapp(rapp, projectName);
        //storage.storeRappInfo(rapp.getInfo(), projectName);
        //storage.storeRappConfig(rapp.getConfig(), projectName);
    }

    public Rapp getApp(String projectName) throws StorageException {
        return storage.loadRapp(projectName);
    }

    public RappConfig getRappConfig(String projectName) throws StorageException {
        Rapp rapp = storage.loadRapp(projectName);
        return rapp.getConfig();
    }

    public RappBinaryInfo getBinaryInfo(String projectName) {
        RappBinaryInfo binaryInfo = new RappBinaryInfo();
        binaryInfo.setExists( storage.binaryAvailable(projectName) );

        return binaryInfo;
    }

}

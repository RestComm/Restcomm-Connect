package org.mobicents.servlet.restcomm.rvd.ras;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.utils.Unzipper;
import org.mobicents.servlet.restcomm.rvd.utils.Zipper;

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
    
    public void saveRappConfig(ServletInputStream rappConfig, String projectName) throws RvdException {
        String data;
        try {
            data = IOUtils.toString(rappConfig);
        } catch (IOException e) {
            throw new RvdException("Error getting app configuration from the request", e);
        }
        logger.debug("RappConfig json: " + data);
        storage.storeRappConfig(data, projectName);

    }
    
    public void saveRappConfig(String rappConfig, String projectName) throws StorageException {
        storage.storeRappConfig(rappConfig, projectName);
    }

    /**
     * Builds a RappConfig object out of its JSON representation
     *
     */
    public RappConfig toModel(Class<RappConfig> clazz, String data) {
        Gson gson = new Gson();
        RappConfig rappConfig = gson.fromJson(data, RappConfig.class);
        return rappConfig;
    }


    public RappConfig getRappConfig(String projectName) throws StorageException {
        String data = storage.loadRappConfig(projectName);
        return toModel(RappConfig.class, data);
    }


    public InputStream createZipPackage(RvdProject project) throws RvdException {

        logger.debug("Creating zip package for project " + project.getName());
        String projectName = project.getName();

        try {
            File tempFile = File.createTempFile("rapp",".tmp");

            Zipper zipper = new Zipper(tempFile);
            try {
                zipper.addDirectory("/app/");
                zipper.addFileContent("/app/config", storage.loadRappConfig(projectName) );
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

            storage.storeAppPackage(projectName, tempFile);
            // TODO - if FsProjectStorage  is not used, the temporaty file should still be removed (in this case it is not moved) !!!

            logger.debug("Zip package created for project " + projectName);

            return storage.getAppPackage(projectName);
        } catch (IOException e) {
            throw new PackagingException("Error creating temporaty zip file ", e);
        }

    }    


    /**
     * Unzips the package stream in a temporary directory and creates an app out of it
     * @param packageZipStream
     * @throws RvdException
     */
    public void importAppToWorkspace( InputStream packageZipStream ) throws RvdException {
        File tempDir = RvdUtils.createTempDir();
        logger.debug("Unzipping ras package to temporary directory " + tempDir.getPath());
        Unzipper unzipper = new Unzipper(tempDir);
        unzipper.unzip(packageZipStream);
        
        
    }

}

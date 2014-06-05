package org.mobicents.servlet.restcomm.rvd.packaging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.utils.Zipper;

import com.google.gson.Gson;

public class PackagingService {
    
    static final Logger logger = Logger.getLogger(PackagingService.class.getName());
    
    private ProjectStorage projectStorage;
    
    public PackagingService(ProjectStorage projectStorage) {
        this.projectStorage = projectStorage;
    }
    
    public void saveRappConfig(ServletInputStream rappConfig, String projectName) throws RvdException {
        String data;
        try {
            data = IOUtils.toString(rappConfig);
        } catch (IOException e) {
            throw new RvdException("Error getting app configuration from the request", e);
        }
        logger.debug("RappConfig json: " + data);
        projectStorage.storeRappConfig(data, projectName);

    }
    public void saveRappConfig(String rappConfig, String projectName) throws StorageException {
        projectStorage.storeRappConfig(rappConfig, projectName);
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
        String data = projectStorage.loadRappConfig(projectName);
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
                zipper.addFileContent("/app/config", projectStorage.loadRappConfig(projectName) );
                zipper.addDirectory("/app/rvd/");
                zipper.addFileContent("/app/rvd/state", projectStorage.loadProjectState(projectName));

                if ( project.supportsWavs() ) {
                    zipper.addDirectory("/app/rvd/wavs/");
                    for ( WavItem wavItem : projectStorage.listWavs(projectName) ) {
                        InputStream wavStream = projectStorage.getWav(projectName, wavItem.getFilename());
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

            projectStorage.storeAppPackage(projectName, tempFile);
            // TODO - if FsProjectStorage  is not used, the temporaty file should still be removed (in this case it is not moved) !!!

            logger.debug("Zip package created for project " + projectName);

            return projectStorage.getAppPackage(projectName);
        } catch (IOException e) {
            throw new PackagingException("Error creating temporaty zip file ", e);
        }

    }
}

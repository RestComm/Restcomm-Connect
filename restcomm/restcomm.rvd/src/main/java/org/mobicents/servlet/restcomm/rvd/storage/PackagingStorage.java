package org.mobicents.servlet.restcomm.rvd.storage;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.AppPackageDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class PackagingStorage {
    private ProjectStorage storage;

    public PackagingStorage( ProjectStorage projectStorage) {
        this.storage = projectStorage;
    }

    public Rapp loadRapp(String projectName) throws StorageException {
        return storage.loadModelFromProjectFile(projectName, RvdSettings.PACKAGING_DIRECTORY_NAME, "rapp" , Rapp.class);
    }

    public void storeRapp(Rapp rapp, String projectName) throws StorageException {
        storage.storeFileToProject(rapp, rapp.getClass(), projectName, RvdSettings.PACKAGING_DIRECTORY_NAME, "rapp");
    }

    public void storeRappBinary(File sourceFile, String projectName ) throws RvdException {
        storage.storeProjectBinaryFile(sourceFile, projectName, RvdSettings.PACKAGING_DIRECTORY_NAME, "app.zip");
    }

    public InputStream getRappBinary(String projectName) throws RvdException {
        try {
            return storage.getProjectBinaryFile(projectName, RvdSettings.PACKAGING_DIRECTORY_NAME, "app.zip");
        } catch (FileNotFoundException e) {
            throw new AppPackageDoesNotExist("Binary package does not exist for project " + projectName);
        }
    }

    public boolean hasPackaging(String projectName) throws ProjectDoesNotExist {
        return storage.projectPathExists(projectName, RvdSettings.PACKAGING_DIRECTORY_NAME);
    }

    public boolean binaryAvailable(String projectName) {
        return storage.projectFileExists(projectName, RvdSettings.PACKAGING_DIRECTORY_NAME, "app.zip");
    }

}

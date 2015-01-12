package org.mobicents.servlet.restcomm.rvd.storage;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.packaging.AppPackageDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.model.packaging.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class FsPackagingStorage implements PackagingStorage {
    private FsStorageBase storageBase;

    public FsPackagingStorage( FsStorageBase storageBase) {
        this.storageBase = storageBase;
    }

    public Rapp loadRapp(String projectName) throws StorageException {
        return storageBase.loadModelFromProjectFile(projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME, "rapp" , Rapp.class);
    }

    public void storeRapp(Rapp rapp, String projectName) throws StorageException {
        storageBase.storeFileToProject(rapp, rapp.getClass(), projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME, "rapp");
    }

    public void storeRappBinary(File sourceFile, String projectName ) throws RvdException {
        storageBase.storeProjectBinaryFile(sourceFile, projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME, "app.zip");
    }

    public InputStream getRappBinary(String projectName) throws RvdException {
        try {
            return storageBase.getProjectBinaryFile(projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME, "app.zip");
        } catch (FileNotFoundException e) {
            throw new AppPackageDoesNotExist("Binary package does not exist for project " + projectName);
        }
    }

    public boolean hasPackaging(String projectName) throws ProjectDoesNotExist {
        return storageBase.projectPathExists(projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME);
    }

    public boolean binaryAvailable(String projectName) {
        return storageBase.projectFileExists(projectName, RvdConfiguration.PACKAGING_DIRECTORY_NAME, "app.zip");
    }

}

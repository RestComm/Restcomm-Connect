package org.mobicents.servlet.restcomm.rvd.storage;

import java.io.File;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingStorage {
    Rapp loadRapp(String projectName) throws StorageException;
    void storeRapp(Rapp rapp, String projectName) throws StorageException;
    void storeRappBinary(File sourceFile, String projectName ) throws RvdException;
    InputStream getRappBinary(String projectName) throws RvdException;
    boolean hasPackaging(String projectName) throws ProjectDoesNotExist;
    boolean binaryAvailable(String projectName);
}

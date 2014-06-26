package org.mobicents.servlet.restcomm.rvd.storage;

import java.io.File;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingStorage {
    public Rapp loadRapp(String projectName) throws StorageException;
    public void storeRapp(Rapp rapp, String projectName) throws StorageException;
    public void storeRappBinary(File sourceFile, String projectName ) throws RvdException;
    public InputStream getRappBinary(String projectName) throws RvdException;
    public boolean hasPackaging(String projectName) throws ProjectDoesNotExist;
    public boolean binaryAvailable(String projectName);
}

package org.mobicents.servlet.restcomm.rvd.packaging;


import java.io.File;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingStorage {
    void storeRappConfig(String data, String projectName) throws StorageException;
    String loadRappConfig(String projectName) throws StorageException;
    boolean hasRappConfig(String projectName) throws ProjectDoesNotExist;
    void storeAppPackage(String projectName, File packageFile) throws RvdException;
    InputStream getAppPackage(String projectName) throws RvdException;
}

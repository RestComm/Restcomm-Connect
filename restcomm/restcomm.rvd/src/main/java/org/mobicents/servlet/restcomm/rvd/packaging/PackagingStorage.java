package org.mobicents.servlet.restcomm.rvd.packaging;


import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingStorage {
    void storeRappConfig(String data, String projectName) throws StorageException;
    String loadRappConfig(String projectName) throws StorageException;
    boolean hasRappConfig(String projectName);
}

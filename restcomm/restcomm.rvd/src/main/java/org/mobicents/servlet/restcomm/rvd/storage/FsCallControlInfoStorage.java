package org.mobicents.servlet.restcomm.rvd.storage;

import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class FsCallControlInfoStorage {
    private FsStorageBase storageBase;

    public FsCallControlInfoStorage(FsStorageBase storageBase) {
        super();
        this.storageBase = storageBase;
    }

    public CallControlInfo loadInfo(String projectName) throws StorageException {
        return storageBase.loadModelFromProjectFile(projectName, "", "cc", CallControlInfo.class);
    }



}

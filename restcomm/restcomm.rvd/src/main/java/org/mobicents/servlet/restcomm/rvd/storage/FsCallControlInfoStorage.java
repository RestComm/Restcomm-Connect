package org.mobicents.servlet.restcomm.rvd.storage;

import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class FsCallControlInfoStorage {

    public static CallControlInfo loadInfo(String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        return workspaceStorage.loadEntity("cc", projectName, CallControlInfo.class);
        //return storageBase.loadModelFromProjectFile(projectName, "", "cc", CallControlInfo.class);
    }

    public static void storeInfo(CallControlInfo info, String projectName, WorkspaceStorage workspaceStorage) throws StorageException {
        workspaceStorage.storeEntity(info, CallControlInfo.class, "cc", projectName);
    }

}

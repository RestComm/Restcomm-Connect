package org.mobicents.servlet.restcomm.rvd.storage;

import java.util.List;

import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

/**
 * Basic project management. Create, remove, list, rename project etc.
 * @author "Tsakiridis Orestis"
 *
 */
public interface ProjectManagementStorage {
    void createProjectSlot(String projectName) throws StorageException;
    void renameProject(String projectName, String newProjectName) throws StorageException;
    void deleteProject(String projectName) throws StorageException;
    boolean projectExists(String projectName);
    List<String> listProjectNames() throws StorageException;    
}

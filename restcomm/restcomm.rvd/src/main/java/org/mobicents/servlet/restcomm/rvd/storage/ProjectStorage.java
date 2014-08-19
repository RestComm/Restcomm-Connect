package org.mobicents.servlet.restcomm.rvd.storage;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.Node;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;


public interface ProjectStorage {
    // Basic project management
    void createProjectSlot(String projectName) throws StorageException;
    void renameProject(String projectName, String newProjectName) throws StorageException;
    void deleteProject(String projectName) throws StorageException;
    boolean projectExists(String projectName);
    List<String> listProjectNames() throws StorageException;

    // Bootstrap parameters
    //void storeBootstrapInfo(String bootstrapInfo, String projectName) throws StorageException;
    //boolean hasBootstrapInfo(String projectName);
    //JsonElement loadBootstrapInfo(String projectName) throws StorageException;

    // Rapp
    void storeRapp(Rapp rapp, String projectName) throws StorageException;
    Rapp loadRapp(String projectName) throws StorageException;
    //List<RappItem> listRapps(List<String> projectNames) throws StorageException;

    // Higher level
    String getAvailableProjectName(String projectName) throws StorageException;
    ProjectOptions loadProjectOptions(String projectName) throws StorageException;
    void storeProjectOptions(String projectName, ProjectOptions projectOptions) throws StorageException;
    String loadProjectState(String projectName) throws StorageException;
    StateHeader loadStateHeader(String projectName) throws StorageException;
    void storeNodeStep(String projectName, String nodeName, String stepName, String content) throws StorageException;
    void updateProjectState(String projectName, String newState) throws StorageException;
    void storeWav(String projectName, String wavname, InputStream wavStream) throws StorageException;
    void storeWav(String projectName, String wavname, File sourceWavFile) throws StorageException;
    List<WavItem> listWavs(String projectName) throws StorageException;
    InputStream getWav(String projectName, String filename) throws StorageException;
    void deleteWav(String projectName, String wavname) throws WavItemDoesNotExist;
    String loadStep(String projectName, String nodeName, String stepName) throws StorageException;
    void storeNodeStepnames(String projectName, Node node) throws StorageException;
    List<String> loadNodeStepnames(String projectName, String nodeName) throws StorageException;
    void backupProjectState(String projectName) throws StorageException;
    void storeProjectState(String projectName, File sourceStateFile) throws StorageException;
    InputStream archiveProject(String projectName) throws StorageException;
    void importProjectFromDirectory(File sourceProjectDirectory, String projectName, boolean overwrite) throws StorageException;

    ProjectState loadProject(String name) throws StorageException;
    void storeProject(String name, ProjectState projectState, boolean firstTime) throws StorageException;

    ModelMarshaler getMarshaler();

}

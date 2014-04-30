package org.mobicents.servlet.restcomm.rvd.storage;

import java.io.InputStream;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.model.client.Node;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;


public interface ProjectStorage {
    String loadProjectOptions(String projectName) throws StorageException;
    void storeProjectOptions(String projectName, String projectOptions) throws StorageException;
    void clearBuiltProject(String projectName) throws StorageException;
    String loadProjectState(String projectName) throws StorageException;
    //String loadNodeStepnames(String projectName, String nodeName) throws StorageException;
    StateHeader loadStateHeader(String projectName) throws StorageException;
   // void storeNodeStepnames(String projectName, String nodeName, String stepNames) throws StorageException;
    void storeNodeStep(String projectName, String nodeName, String stepName, String content) throws StorageException;
    boolean projectExists(String projectName);
    List<String> listProjectNames() throws StorageException;
    void cloneProject(String name, String clonedName) throws StorageException;
    void updateProjectState(String projectName, String newState) throws StorageException;
    void renameProject(String projectName, String newProjectName) throws StorageException;
    void deleteProject(String projectName) throws StorageException;
    void storeWav(String projectName, String wavname, InputStream wavStream) throws StorageException;
    List<WavItem> listWavs(String projectName) throws StorageException;
    void deleteWav(String projectName, String wavname) throws WavItemDoesNotExist;
    String loadStep(String projectName, String nodeName, String stepName) throws StorageException;
    void storeNodeStepnames(String projectName, Node node) throws StorageException;
    List<String> loadNodeStepnames(String projectName, String nodeName) throws StorageException;
    void backupProjectState(String projectName) throws StorageException;
    void cloneProtoProject(String kind, String clonedName) throws StorageException;
}

package org.mobicents.servlet.restcomm.rvd.storage;

import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class RasStorage {
    private ProjectStorage storage;

    public RasStorage( ProjectStorage projectStorage) {
        this.storage = projectStorage;
    }

    public void storeBootstrapInfo(String bootstrapInfo, String projectName) throws StorageException {
        storage.storeProjectFile(bootstrapInfo, projectName, "ras", "bootstrap");
    }

    public boolean hasBootstrapInfo(String projectName) {
        return storage.projectFileExists(projectName, "ras", "bootstrap");
    }

    public JsonElement loadBootstrapInfo(String projectName) throws StorageException {
        String data = storage.loadProjectFile(projectName, "ras", "bootstrap");
        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(data);
        return rootElement;
    }

    public void storeRapp(Rapp rapp, String projectName) throws StorageException {
        storage.storeFileToProject(rapp, rapp.getClass(), projectName, "ras", "rapp");
    }

    public Rapp loadRapp(String projectName) throws StorageException {
        return storage.loadModelFromProjectFile(projectName, "ras", "rapp", Rapp.class);
    }

    //public List<RappInfo> listRapps() {

    //}
}

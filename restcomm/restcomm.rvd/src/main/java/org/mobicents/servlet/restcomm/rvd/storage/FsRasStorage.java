package org.mobicents.servlet.restcomm.rvd.storage;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappInfo;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class FsRasStorage implements RasStorage {
    private FsStorageBase storage;

    public FsRasStorage( FsStorageBase storageBase) {
        this.storage = storageBase;
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

    /**
     * Is this projoct a ras application. Checks for the existence "ras" directory
     * @param projectName
     * @return
     */
   /* public boolean isRasApp(String projectName) {
        if ( storage.getPro)
    }
    */

    public List<RappInfo> listRapps() throws StorageException {
        List<String> projectNames = storage.listProjectNames();
        List<RappInfo> rapps = new ArrayList<RappInfo>();
        for (String projectName : projectNames) {
            Rapp rapp = storage.loadModelFromProjectFile(projectName, "ras", "rapp", Rapp.class);
            rapps.add(rapp.getInfo());
        }
        return rapps;
    }
}

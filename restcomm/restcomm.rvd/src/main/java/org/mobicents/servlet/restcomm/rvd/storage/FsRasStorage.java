package org.mobicents.servlet.restcomm.rvd.storage;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.ras.RappItem;
import org.mobicents.servlet.restcomm.rvd.ras.RappItem.RappStatus;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class FsRasStorage implements RasStorage {
    private FsStorageBase storageBase;

    public FsRasStorage( FsStorageBase storageBase) {
        this.storageBase = storageBase;
    }

    public void storeBootstrapInfo(String bootstrapInfo, String projectName) throws StorageException {
        storageBase.storeProjectFile(bootstrapInfo, projectName, "ras", "bootstrap");
    }

    public boolean hasBootstrapInfo(String projectName) {
        return storageBase.projectFileExists(projectName, "ras", "bootstrap");
    }

    public JsonElement loadBootstrapInfo(String projectName) throws StorageException {
        String data = storageBase.loadProjectFile(projectName, "ras", "bootstrap");
        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(data);
        return rootElement;
    }

    public void storeRapp(Rapp rapp, String projectName) throws StorageException {
        storageBase.storeFileToProject(rapp, rapp.getClass(), projectName, "ras", "rapp");
    }

    public Rapp loadRapp(String projectName) throws StorageException {
        return storageBase.loadModelFromProjectFile(projectName, "ras", "rapp", Rapp.class);
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

    /**
     * Creates a list of rapp info objects out of a set of projects
     * @param projectNames
     * @return
     * @throws StorageException
     */
    public List<RappItem> listRapps(List<String> projectNames) throws StorageException {
        //List<String> projectNames = storageBase.listProjectNames();
        List<RappItem> rapps = new ArrayList<RappItem>();
        for (String projectName : projectNames) {
            if ( storageBase.projectFileExists(projectName, "ras", "rapp") ) {
                RappItem item = new RappItem();
                item.setProjectName(projectName);

                // load info from rapp file
                Rapp rapp = storageBase.loadModelFromProjectFile(projectName, "ras", "rapp", Rapp.class);
                item.setRappInfo(rapp.getInfo());

                // app status
                boolean installedStatus = true;
                boolean configuredStatus = hasBootstrapInfo(projectName);
                boolean activeStatus = installedStatus && configuredStatus;
                RappStatus[] statuses = new RappStatus[3];
                statuses[0] = RappStatus.Installed; // always set
                statuses[1] = configuredStatus ? RappStatus.Configured : RappStatus.Unconfigured;
                statuses[2] = activeStatus ? RappStatus.Active : RappStatus.Inactive;
                item.setStatus(statuses);

                rapps.add(item);
            }
        }
        return rapps;
    }
}

package org.mobicents.servlet.restcomm.rvd.storage;

import java.util.List;

import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.ras.RappItem;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.JsonElement;

public interface RasStorage {
    void storeBootstrapInfo(String bootstrapInfo, String projectName) throws StorageException;
    boolean hasBootstrapInfo(String projectName);
    JsonElement loadBootstrapInfo(String projectName) throws StorageException;
    void storeRapp(Rapp rapp, String projectName) throws StorageException;
    Rapp loadRapp(String projectName) throws StorageException;
    List<RappItem> listRapps(List<String> projectNames) throws StorageException;
}





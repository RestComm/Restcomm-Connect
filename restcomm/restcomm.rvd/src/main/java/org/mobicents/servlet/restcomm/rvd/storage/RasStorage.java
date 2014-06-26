package org.mobicents.servlet.restcomm.rvd.storage;

import java.util.List;

import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappInfo;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.JsonElement;

public interface RasStorage {
    public void storeBootstrapInfo(String bootstrapInfo, String projectName) throws StorageException;
    public boolean hasBootstrapInfo(String projectName);
    public JsonElement loadBootstrapInfo(String projectName) throws StorageException;
    public void storeRapp(Rapp rapp, String projectName) throws StorageException;
    public Rapp loadRapp(String projectName) throws StorageException;
    public List<RappInfo> listRapps() throws StorageException;
}





package org.mobicents.servlet.restcomm.rvd.packaging;

import javax.servlet.ServletInputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingService {
    void saveRappConfig(ServletInputStream data, String projectName) throws RvdException;
    void saveRappConfig(String rappConfig, String projectName) throws StorageException;
    RappConfig getRappConfig(String projectName) throws StorageException;
    RappConfig toModel(Class<RappConfig> clazz, String data);
}

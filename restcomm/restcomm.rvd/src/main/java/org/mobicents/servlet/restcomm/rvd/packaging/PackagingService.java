package org.mobicents.servlet.restcomm.rvd.packaging;

import javax.servlet.ServletInputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;

public interface PackagingService {
    void saveRappConfig(ServletInputStream data, String projectName) throws RvdException;
    void saveRappConfig(String rappConfig, String projectName);
    RappConfig toModel(Class<RappConfig> clazz, String data);
}

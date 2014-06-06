package org.mobicents.servlet.restcomm.rvd.packaging;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;

public class PackagingService {

    static final Logger logger = Logger.getLogger(PackagingService.class.getName());

    private ProjectStorage projectStorage;

    public PackagingService(ProjectStorage projectStorage) {
        this.projectStorage = projectStorage;
    }

}

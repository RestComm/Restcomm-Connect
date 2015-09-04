package org.mobicents.servlet.restcomm.rvd.configuration;

import javax.servlet.ServletContext;

public class RvdConfigurator {

    String rvdContextRootPath; // sth like .../standalone/deployments/restcomm-rvd.war

    RvdConfigurator(ServletContext servletContext) {
        rvdContextRootPath = servletContext.getRealPath("");
    }

    public String getContextRootPath() {
        return rvdContextRootPath;
    }

}

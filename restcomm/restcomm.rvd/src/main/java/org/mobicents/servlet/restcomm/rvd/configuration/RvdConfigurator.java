package org.mobicents.servlet.restcomm.rvd.configuration;

import javax.servlet.ServletContext;

public class RvdConfigurator {

    private static String RESTCOMM_CONTEXT_NAME = "restcomm.war";

    String rvdContextRootPath; // sth like .../standalone/deployments/restcomm-rvd.war

    RvdConfigurator(ServletContext servletContext) {
        rvdContextRootPath = servletContext.getRealPath("");
    }

    public String getContextRootPath() {
        return rvdContextRootPath;
    }

    public String getRestcommContextRootPath() {
        return rvdContextRootPath + "/../" + RESTCOMM_CONTEXT_NAME ;
    }


}

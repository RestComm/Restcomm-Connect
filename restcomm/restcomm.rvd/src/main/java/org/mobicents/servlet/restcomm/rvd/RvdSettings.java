package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

public class RvdSettings {
    private static final String workspaceDirectoryName = "workspace";
    private static final String protoProjectName = "_proto";
    private static final String wavsDirectoryName = "wavs";
    private static final String RVD_PROJECT_VERSION = "1.0"; // version for rvd project syntax
    public static final String STICKY_PREFIX = "sticky_"; // a  prefix for rvd sticky variable names

    private Map<String,String> options;
    private String externalServiceBase; // use this when relative urls (starting with /) are specified in ExternalService steps

    public RvdSettings(ServletContext servletContext) {
        options = new HashMap<String,String>();
        options.put("workspaceBasePath", servletContext.getRealPath(File.separator) + workspaceDirectoryName );
        options.put("protoProjectName", protoProjectName );
        options.put("wavsDirectoryName", wavsDirectoryName );

    }

    public static String getRvdProjectVersion() {
        return RVD_PROJECT_VERSION;
    }

    public String getOption(String optionName) {
        return options.get(optionName);
    }

    public String getExternalServiceBase() {
        return externalServiceBase;
    }

    public void setExternalServiceBase(String externalServiceBase) {
        this.externalServiceBase = externalServiceBase;
    }

}

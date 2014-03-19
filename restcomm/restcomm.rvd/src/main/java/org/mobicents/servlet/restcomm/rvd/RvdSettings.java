package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

public class RvdSettings {
    private static final String workspaceDirectoryName = "workspace";
    private static final String protoProjectName = "_proto";
    private static final String wavsDirectoryName = "wavs";

    private Map<String,String> options;
    private String externalServiceBase; // use this when relative urls (starting with /) are specified in ExternalService steps

    public RvdSettings(ServletContext servletContext) {
        options = new HashMap<String,String>();
        options.put("workspaceBasePath", servletContext.getRealPath(File.separator) + workspaceDirectoryName );
        options.put("protoProjectName", protoProjectName );
        options.put("wavsDirectoryName", wavsDirectoryName );

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

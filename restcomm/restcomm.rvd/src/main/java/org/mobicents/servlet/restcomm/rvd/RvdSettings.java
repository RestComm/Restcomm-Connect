package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.model.RvdConfig;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

public class RvdSettings {
    static final Logger logger = Logger.getLogger(RvdSettings.class.getName());

    private static final String workspaceDirectoryName = "workspace";
    private static final String protoProjectName = "_proto";
    private static final String wavsDirectoryName = "wavs";
    private static final String RVD_PROJECT_VERSION = "1.0"; // version for rvd project syntax
    public static final String STICKY_PREFIX = "sticky_"; // a  prefix for rvd sticky variable names
    public static final String CORE_VARIABLE_PREFIX = "core_"; // a prefix for rvd variables that come from Restcomm parameters

    private Map<String,String> options;
    private String externalServiceBase; // use this when relative urls (starting with /) are specified in ExternalService steps

    public RvdSettings(ServletContext servletContext) {
        String workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName;

        // Try loading from configuration from XML file
        try {
            InputStream is = servletContext.getResourceAsStream("/WEB-INF/rvd.xml");
            XStream xstream = new XStream();
            xstream.alias("rvd", RvdConfig.class);
            RvdConfig rvdConfig = (RvdConfig) xstream.fromXML( is );
            if (rvdConfig.getWorkspaceLocation() != null  &&  !"".equals(rvdConfig.getWorkspaceLocation()) )
                workspaceBasePath = rvdConfig.getWorkspaceLocation();
        } catch (StreamException e) {
            logger.warn("RVD configuration file not found - WEB-INF/rvd.xml");
        }

        logger.info("Using workspace at " + workspaceBasePath);

        options = new HashMap<String,String>();
        options.put("workspaceBasePath", workspaceBasePath );
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

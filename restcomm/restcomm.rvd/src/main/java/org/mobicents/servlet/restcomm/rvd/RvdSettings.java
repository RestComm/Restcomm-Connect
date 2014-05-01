package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.InputStream;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.model.RvdConfig;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

public class RvdSettings {
    static final Logger logger = Logger.getLogger(RvdSettings.class.getName());
    private ServletContext servletContext;

    private static final String WORKSPACE_DIRECTORY_NAME = "workspace";
    public static final String PROTO_DIRECTORY_PREFIX = "_proto";

    public static final String WAVS_DIRECTORY_NAME = "wavs";
    private static final String RVD_PROJECT_VERSION = "1.0"; // version for rvd project syntax
    public static final String STICKY_PREFIX = "sticky_"; // a  prefix for rvd sticky variable names
    public static final String CORE_VARIABLE_PREFIX = "core_"; // a prefix for rvd variables that come from Restcomm parameters

    private String workspaceBasePath;
    private String externalServiceBase; // use this when relative urls (starting with /) are specified in ExternalService steps

    public RvdSettings(ServletContext servletContext) {
        this.servletContext = servletContext;
        String workspaceBasePath = servletContext.getRealPath(File.separator) + WORKSPACE_DIRECTORY_NAME;

        // Try loading from configuration from XML file
        try {
            InputStream is = servletContext.getResourceAsStream("/WEB-INF/rvd.xml");
            XStream xstream = new XStream();
            xstream.alias("rvd", RvdConfig.class);
            RvdConfig rvdConfig = (RvdConfig) xstream.fromXML( is );
            if (rvdConfig.getWorkspaceLocation() != null  &&  !"".equals(rvdConfig.getWorkspaceLocation()) ) {
                if ( rvdConfig.getWorkspaceLocation().startsWith("/") )
                    workspaceBasePath = rvdConfig.getWorkspaceLocation(); // this is an absolute path
                else
                    workspaceBasePath = servletContext.getRealPath(File.separator) + rvdConfig.getWorkspaceLocation(); // this is a relative path hooked under RVD context
            }
        } catch (StreamException e) {
            logger.warn("RVD configuration file not found - WEB-INF/rvd.xml");
        }

        logger.info("Using workspace at " + workspaceBasePath);

        this.workspaceBasePath = workspaceBasePath;
    }

    public String getWorkspaceBasePath() {
        return this.workspaceBasePath;
    }

    // for RVD 7.1.5 and later
    public String getPrototypeProjectsPath() {
        String workspaceBasePath = servletContext.getRealPath(File.separator) + "protoProjects";
        return workspaceBasePath;
    }

    public static String getRvdProjectVersion() {
        return RVD_PROJECT_VERSION;
    }

    public String getExternalServiceBase() {
        return externalServiceBase;
    }

    public void setExternalServiceBase(String externalServiceBase) {
        this.externalServiceBase = externalServiceBase;
    }
}

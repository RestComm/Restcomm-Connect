package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.model.RvdConfig;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

public class RvdConfiguration {
    static final Logger logger = Logger.getLogger(RvdConfiguration.class.getName());
    private static RvdConfiguration instance = null;

    private static final String WORKSPACE_DIRECTORY_NAME = "workspace";
    public static final String PROTO_DIRECTORY_PREFIX = "_proto";
    public static final String REST_SERVICES_PATH = "services"; // the "services" from the /restcomm-rvd/services/apps/... path

    public static final String WAVS_DIRECTORY_NAME = "wavs";
    private static final String RVD_PROJECT_VERSION = "1.3"; // version for rvd project syntax
    private static final String PACKAGING_VERSION = "1.0";
    private static final String RAS_APPLICATION_VERSION = "2"; // version of the RAS application specification
    public static final String STICKY_PREFIX = "sticky_"; // a  prefix for rvd sticky variable names
    public static final String MODULE_PREFIX = "module_"; // a  prefix for rvd module-scoped variable names
    public static final String CORE_VARIABLE_PREFIX = "core_"; // a prefix for rvd variables that come from Restcomm parameters
    public static final String PACKAGING_DIRECTORY_NAME = "packaging";
    public static final String TICKET_COOKIE_NAME = "rvdticket"; // the name of the cookie that is used to store ticket ids for authentication
    private static Set<String> restcommParameterNames  = new HashSet<String>(Arrays.asList(new String[] {"CallSid","AccountSid","From","To","Body","CallStatus","ApiVersion","Direction","CallerName"})); // the names of the parameters supplied by restcomm request when starting an application
    public static final String PROJECT_LOG_FILENAME = "projectLog";
    public static final String DEFAULT_APPSTORE_DOMAIN = "apps.restcomm.com";
    public static final HashSet<String> builtinRestcommParameters = new HashSet<String>(Arrays.asList(new String[] {"CallSid","AccountSid","From","To","Body","CallStatus","ApiVersion","Direction","CallerName"}));
    public static final String RESTCOMM_HEADER_PREFIX = "SipHeader_"; // the prefix added to HTTP headers from Restcomm
    public static final String RESTCOMM_HEADER_PREFIX_DIAL = "DialSipHeader_"; // another prefix

    private String workspaceBasePath;
    private String prototypeProjectsPath;
    private String externalServiceBase; // use this when relative urls (starting with /) are specified in ExternalService steps
    private RvdConfig rvdConfig;  // the configuration settings from rvd.xml
    //private String effectiveRestcommIp; // the IP address to access .wavs and other resources from the internet. It takes into account rvd.xml and servletContext


    public static RvdConfiguration getInstance(ServletContext servletContext) {
        if ( instance == null ) {
            instance = new RvdConfiguration(servletContext);
        }
        return instance;
    }

    private RvdConfiguration(ServletContext servletContext) {
        this.prototypeProjectsPath = servletContext.getRealPath(File.separator) + "protoProjects";
        String workspaceBasePath = servletContext.getRealPath(File.separator) + WORKSPACE_DIRECTORY_NAME;

        // Try loading from configuration from XML file
        try {
            InputStream is = servletContext.getResourceAsStream("/WEB-INF/rvd.xml");
            XStream xstream = new XStream();
            xstream.alias("rvd", RvdConfig.class);
            rvdConfig = (RvdConfig) xstream.fromXML( is );
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

    public String getProjectBasePath(String projectName) {
        return this.workspaceBasePath + File.separator + projectName;
    }

    // for RVD 7.1.5 and later
    public String getPrototypeProjectsPath() {
        return prototypeProjectsPath;
    }

    public static String getRvdProjectVersion() {
        return RVD_PROJECT_VERSION;
    }

    public static String getPackagingVersion() {
        return PACKAGING_VERSION;
    }

    public static String getRasApplicationVersion() {
        return RAS_APPLICATION_VERSION;
    }

    public String getExternalServiceBase() {
        return externalServiceBase;
    }

    public void setExternalServiceBase(String externalServiceBase) {
        this.externalServiceBase = externalServiceBase;
    }

    public String getEffectiveRestcommIp(HttpServletRequest request) {
        String ipFromXml = rvdConfig.getRestcommPublicIp();

        String ip = request.getLocalAddr(); // use request ip as default
        if ( ipFromXml != null  &&  !"".equals(ipFromXml) ) {
            ip = ipFromXml;
        }
        return ip;
    }

    // Always returns the destination port in the request. When the configuration/settings scheme clears
    // out a proper implementation should be done.
    // TODO
    public String getEffectiveRestcommPort(HttpServletRequest request) {
        int port = request.getLocalPort();
        return "" + port;
    }

    /**
     * Returns the IP rvd listens to for internal use. This address can be used by restcomm to access applications.
     * A request object is required to get it. Even when this request comes from a browser, this function should report
     * the correct IP.
     * @param request
     * @return
     */
    public String getRvdInternalIp(HttpServletRequest request) {
        return request.getLocalAddr();
    }

    public int getRvdInternalPort(HttpServletRequest request) {
        return request.getLocalPort();
    }

    public static Set<String> getRestcommParameterNames() {
        return restcommParameterNames;
    }



}

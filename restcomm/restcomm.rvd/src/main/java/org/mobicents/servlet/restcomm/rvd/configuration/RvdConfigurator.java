package org.mobicents.servlet.restcomm.rvd.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;

public class RvdConfigurator {
    static final Logger logger = Logger.getLogger(RvdConfigurator.class.getName());

    private static String RESTCOMM_CONTEXT_NAME = "restcomm.war";
    private static final String RVD_ADAPTER_FILEPATH = "/WEB-INF/conf/rvd/keycloak.json";

    String rvdContextRootPath; // sth like .../standalone/deployments/restcomm-rvd.war

    // keycloak deployment handling
    protected KeycloakDeployment cache;
    private KeycloakDeployment emptyDeployment = null; // new KeycloakDeployment(); // return this in case there is no adapter file available
    long lastModified = 0; // any valid modification time will be greater than this

    RvdConfigurator(ServletContext servletContext) {
        rvdContextRootPath = servletContext.getRealPath("");
        cache = emptyDeployment;
        checkDeployment();
        if (getDeployment() == null) {
            logger.warn("No identity configuration found. Make sure Restcomm instance is properly registered to an authorization server before using RVD.");
        }
    }

    public String getContextRootPath() {
        return rvdContextRootPath;
    }

    public String getRestcommContextRootPath() {
        return rvdContextRootPath + "/../" + RESTCOMM_CONTEXT_NAME ;
    }

    public KeycloakDeployment getDeployment() {
        // TODO monitor adapter file and re-build deployment if required.
        return cache;
    }

    /**
     * Checks whether the deployment configuration has changed and updates it if required.
     */
    public void checkDeployment() {
        String configFilepath = getRestcommContextRootPath() + RVD_ADAPTER_FILEPATH;
        File file = new File(configFilepath);
        if ( file.exists() ) {
            long thisLastModified = file.lastModified();
            if ( thisLastModified > lastModified ) {
                cache = createFreshKeycloakDeployment(configFilepath);
                lastModified = thisLastModified;
            }
        } else {
            cache = emptyDeployment;
        }
    }

    private KeycloakDeployment createFreshKeycloakDeployment(String configFilepath) {
        InputStream is;
        try {
            logger.info("Detected change in identity configuration. A new keycloak deployment will be created.");
            is = new FileInputStream(configFilepath);
        } catch (FileNotFoundException e) {
            logger.error("Could not open keycloak adapter config file: '" + configFilepath + "'. Identity/security features will be disabled" );
            return emptyDeployment;
        }
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        return deployment;
    }

}

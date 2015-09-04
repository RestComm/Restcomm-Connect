package org.mobicents.servlet.restcomm.rvd.keycloak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.keycloak.adapters.HttpFacade.Request;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.mobicents.servlet.restcomm.rvd.configuration.RvdConfigurator;
import org.mobicents.servlet.restcomm.rvd.configuration.RvdConfiguratorBuilder;


public class RvdKeycloakConfigResolver implements KeycloakConfigResolver {

    static final Logger logger = Logger.getLogger(RvdKeycloakConfigResolver.class.getName());

    private KeycloakDeployment cache;
    private KeycloakDeployment emptyDeployment = new KeycloakDeployment(); // return this in case there is no adapter file available
    long lastModified = 0; // any valid modification time will be greater than this

    @Override
    public KeycloakDeployment resolve(Request request) {
        RvdConfigurator configurator = RvdConfiguratorBuilder.get();

        String configFilepath = configurator.getContextRootPath() + "/WEB-INF/keycloak.json";
        File file = new File(configFilepath);
        if ( file.exists() ) {
            long thisLastModified = file.lastModified();
            if ( thisLastModified > lastModified || cache == null ) {
                cache = createFreshKeycloakDeployment(configFilepath);
                lastModified = thisLastModified;
            }
            return cache;
        } else
            return emptyDeployment;
    }

    private KeycloakDeployment createFreshKeycloakDeployment(String configFilepath) {
        InputStream is;
        try {
            is = new FileInputStream(configFilepath);
        } catch (FileNotFoundException e) {
            logger.error("Could not open keycloak adapter config file: '" + configFilepath + "'. Identity/security features will be disabled" );
            return emptyDeployment;
        }
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        return deployment;
    }
}

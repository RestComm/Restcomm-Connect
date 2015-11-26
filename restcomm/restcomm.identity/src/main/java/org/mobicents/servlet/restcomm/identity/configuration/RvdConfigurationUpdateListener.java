package org.mobicents.servlet.restcomm.identity.configuration;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakConfigurationBuilder;

public class RvdConfigurationUpdateListener implements ConfigurationUpdateListener<MutableIdentityConfigurationSet> {

    // Relative path of the rvd keycloak adapter configuration parent directory based on Restcomm context root path
    private static final String RVD_ADAPTER_PATH = "/WEB-INF/conf/rvd";

    protected Logger logger = Logger.getLogger(RvdConfigurationUpdateListener.class);

    private String absoluteRvdAdapterPath;
    private IdentityConfigurationSet identityConfig;

    public RvdConfigurationUpdateListener(ServletContext servletContext, IdentityConfigurationSet identityConfig) {
        absoluteRvdAdapterPath = servletContext.getRealPath(RVD_ADAPTER_PATH);
        this.identityConfig = identityConfig;
    }

    /**
     * Updates RVD adapter configuration file, restcomm.war/WEB-INF/conf/keycloak.json
     * with options built out of current restcomm configuration.
     */
    @Override
    public void configurationUpdated(MutableIdentityConfigurationSet identityInstanceConfig) {
        // Create the rvd configuration directory if it does not exist
        File adapterParentDir = new File(absoluteRvdAdapterPath);
        if ( ! adapterParentDir.exists() ) {
            try {
                FileUtils.forceMkdir(adapterParentDir);
            } catch (IOException e1) {
                logger.error("Could not create keycloak RVD adapter directory: '" + adapterParentDir + "'. RVD won't be properly secured.", e1);
            }
        }

        // remove adapter file if still in 'init' mode
        String adapterConfigFilePath = absoluteRvdAdapterPath + "/keycloak.json";
        if ("init".equals(identityInstanceConfig.getMode())) {
            FileUtils.deleteQuietly(new File(adapterConfigFilePath)); // remove RVD keycloak adapter to have a consistent setup
            return;
        }

        // create adapter file - keycloak.json
        KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(identityConfig.getRealm(),identityConfig.getRealmkey(),identityConfig.getAuthServerUrl(),identityInstanceConfig.getInstanceId(),identityInstanceConfig.getRestcommClientSecret());
        AdapterConfig adapterConfig = confBuilder.getRestcommRvdConfig();
        if (adapterConfig == null) {
            logger.error("Restcomm not registered to an authorization server. There is probably a configuration error. RVD keycloak adapter configuration won't be updated.");
            return;
        }
        IdentityUtils.writeAdapterConfigToFile(adapterConfig, adapterConfigFilePath);
        logger.info("Updated RVD keycloak adapter configuration file: '" + adapterConfigFilePath + "'");
    }

}

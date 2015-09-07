package org.mobicents.servlet.restcomm.identity.configuration;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator.IdentityNotSet;

public class RvdConfigurationUpdateListener implements ConfigurationUpdateListener<IdentityConfigurator> {

    // Relative path of the rvd keycloak adapter configuration parent directory based on Restcomm context root path
    private static final String RVD_ADAPTER_PATH = "/WEB-INF/conf/rvd";

    protected Logger logger = Logger.getLogger(RvdConfigurationUpdateListener.class);

    private String absoluteRvdAdapterPath;

    public RvdConfigurationUpdateListener(ServletContext servletContext) {
        absoluteRvdAdapterPath = servletContext.getRealPath(RVD_ADAPTER_PATH);
    }

    /**
     * Updates RVD adapter configuration file, restcomm.war/WEB-INF/conf/keycloak.json
     * with options built out of current restcomm configuration.
     */
    @Override
    public void configurationUpdated(IdentityConfigurator configurator) {
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
        if (configurator.getMode() == IdentityMode.init) {
            FileUtils.deleteQuietly(new File(adapterConfigFilePath)); // remove RVD keycloak adapter to have a consistent setup
            return;
        }

        // create adapter file - keycloak.json
        try {
            AdapterConfig adapterConfig = configurator.getRestcommRvdConfig();
            configurator.writeAdapterConfigToFile(adapterConfig, adapterConfigFilePath);
            logger.info("Updated RVD keycloak adapter configuration file: '" + adapterConfigFilePath + "'");
        } catch (IdentityNotSet e) {
            logger.error("Restcomm not registered to an authorization server. There is probably a configuration error. RVD keycloak adapter configuration won't be updated.",e);
        }
    }

}

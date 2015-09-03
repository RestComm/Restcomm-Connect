package org.mobicents.servlet.restcomm.identity.configuration;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator.IdentityNotSet;

public class RvdConfigurationUpdateListener implements ConfigurationUpdateListener<IdentityConfigurator> {

    protected Logger logger = Logger.getLogger(RvdConfigurationUpdateListener.class);

    String rvdContextRealPath; // the root directory where RVD lives

    public RvdConfigurationUpdateListener(ServletContext servletContext) {
        rvdContextRealPath = servletContext.getRealPath("../restcomm-rvd.war");
    }

    /**
     * Updates RVD adapter configuration file, restcomm-rvd.war/WEB-INF/keycloak.json
     * with with options built out of current restcomm configuration.
     */
    @Override
    public void configurationUpdated(IdentityConfigurator configurator) {
        // update rvd keycloak.json file

        String adapterConfigFilePath = rvdContextRealPath + "/WEB-INF/keycloak.json";

        if (configurator.getMode() == IdentityMode.init) {
            FileUtils.deleteQuietly(new File(adapterConfigFilePath)); // remove RVD keycloak adapter to have a consistent setup
            return;
        }

        try {
            AdapterConfig adapterConfig = configurator.getRestcommRvdConfig();
            configurator.writeAdapterConfigToFile(adapterConfig, adapterConfigFilePath);
            logger.info("Updated RVD keycloak adapter configuration.");
        } catch (IdentityNotSet e) {
            logger.error("Restcomm not registered to an authorization server. There is probably a configuration error. RVD keycloak adapter configuration won't be updated.",e);
        }


    }

}

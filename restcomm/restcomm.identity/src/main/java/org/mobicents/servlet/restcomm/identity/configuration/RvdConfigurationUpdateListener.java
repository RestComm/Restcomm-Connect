package org.mobicents.servlet.restcomm.identity.configuration;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;

public class RvdConfigurationUpdateListener implements ConfigurationUpdateListener<IdentityConfigurator> {

    protected Logger logger = Logger.getLogger(RvdConfigurationUpdateListener.class);

    @Override
    public void configurationUpdated(IdentityConfigurator configurator) {
        // TODO Auto-generated method stub
        // update rvd keycloak.json file
        logger.info("NOTIFY RVD for configuration update here!");
    }

}

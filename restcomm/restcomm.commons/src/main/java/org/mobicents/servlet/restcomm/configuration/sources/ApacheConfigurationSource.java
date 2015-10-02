package org.mobicents.servlet.restcomm.configuration.sources;

import org.apache.commons.configuration.Configuration;

public class ApacheConfigurationSource implements ConfigurationSource {

    private final Configuration apacheConfiguration;

    public ApacheConfigurationSource(Configuration apacheConfiguration) {
        super();
        this.apacheConfiguration = apacheConfiguration;
    }

    @Override
    public String getProperty(String key) {
        return apacheConfiguration.getString(key);
    }

}

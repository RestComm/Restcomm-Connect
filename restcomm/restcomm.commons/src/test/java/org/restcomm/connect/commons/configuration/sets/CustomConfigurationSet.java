package org.restcomm.connect.commons.configuration.sets;

import org.restcomm.connect.commons.configuration.sets.impl.ConfigurationSet;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

public class CustomConfigurationSet extends ConfigurationSet {

    public static final String PROPERTY1_KEY = "property1";
    private String property1;

    public CustomConfigurationSet(ConfigurationSource source) {
        super(source);
        property1 = source.getProperty(PROPERTY1_KEY);
    }

    public String getProperty1() {
        return property1;
    }

}

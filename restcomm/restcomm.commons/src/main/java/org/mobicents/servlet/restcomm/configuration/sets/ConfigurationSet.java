package org.mobicents.servlet.restcomm.configuration.sets;

import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class ConfigurationSet {
    private final ConfigurationSource source;

    protected ConfigurationSet(ConfigurationSource source) {
        super();
        this.source = source;
    }

    public ConfigurationSource getSource() {
        return source;
    }
}

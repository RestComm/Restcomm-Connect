package org.mobicents.servlet.restcomm.configuration;

import org.mobicents.servlet.restcomm.configuration.sets.ConfigurationSet;

public interface ConfigurationUpdateListener<T extends ConfigurationSet> {
    void configurationUpdated(T configurationSet );
}

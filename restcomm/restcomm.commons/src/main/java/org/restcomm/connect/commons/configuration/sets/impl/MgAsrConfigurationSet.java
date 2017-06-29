package org.restcomm.connect.commons.configuration.sets.impl;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

import java.util.Collections;
import java.util.List;

/**
 * Created by gdubina on 26.06.17.
 */
public class MgAsrConfigurationSet extends ConfigurationSet {

    private final List<String> drivers;

    private final String defaultDriver;

    public MgAsrConfigurationSet(ConfigurationSource source, Configuration config) {
        super(source);
        this.drivers = Collections.unmodifiableList(config.getList("runtime-settings.mg-asr-drivers.driver"));
        this.defaultDriver = config.getString("runtime-settings.mg-asr-drivers[@default]");
    }

    public List<String> getDrivers() {
        return this.drivers;
    }

    public String getDefaultDriver() {
        return defaultDriver;
    }
}

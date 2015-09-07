package org.mobicents.servlet.restcomm.configuration;

public interface Configurator {
    void load();
    void save();
    void registerUpdateListener(ConfigurationUpdateListener listener);
}

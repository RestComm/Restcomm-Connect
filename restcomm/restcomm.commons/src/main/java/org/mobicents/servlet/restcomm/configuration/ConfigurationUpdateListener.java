package org.mobicents.servlet.restcomm.configuration;

public interface ConfigurationUpdateListener<T extends Configurator> {
    void configurationUpdated(T configurator );
}

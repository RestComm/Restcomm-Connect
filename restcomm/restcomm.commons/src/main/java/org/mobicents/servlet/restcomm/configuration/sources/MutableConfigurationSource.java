package org.mobicents.servlet.restcomm.configuration.sources;

public interface MutableConfigurationSource extends ConfigurationSource {
    void setProperty(String key, String value);
}

package org.mobicents.servlet.restcomm.configuration.sets;

import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

import java.util.List;

/**
 * Created by nando on 12/10/15.
 */
public interface ConfigurationSet {
    ConfigurationSource getSource();
    void registerUpdateListener(ConfigurationUpdateListener listener);
    List<ConfigurationUpdateListener> getUpdateListeners();
}

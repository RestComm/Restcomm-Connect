package org.mobicents.servlet.restcomm.identity.migration;

import org.mobicents.servlet.restcomm.configuration.sources.MutableConfigurationSource;

/**
 * Created by otsakir on 12/11/15.
 */
public class MockMutableConfigurationSource implements MutableConfigurationSource {
    @Override
    public void setProperty(String key, String value) {

    }

    @Override
    public String getProperty(String key) {
        return null;
    }
}

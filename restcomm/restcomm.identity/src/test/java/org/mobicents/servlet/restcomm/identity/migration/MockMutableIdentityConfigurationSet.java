package org.mobicents.servlet.restcomm.identity.migration;

import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;
import org.mobicents.servlet.restcomm.configuration.sources.MutableConfigurationSource;

import java.util.List;

/**
 * Created by otsakir on 12/11/15.
 */
public class MockMutableIdentityConfigurationSet implements MutableIdentityConfigurationSet {

    String mode;
    String restcommClientSecret;
    String instanceId;
    Boolean autoImportUsers;
    MutableConfigurationSource source = new MockMutableConfigurationSource();


    public MockMutableIdentityConfigurationSet(String mode, String restcommClientSecret, String instanceId, Boolean autoImportUsers) {
        this.mode = mode;
        this.restcommClientSecret = restcommClientSecret;
        this.instanceId = instanceId;
        this.autoImportUsers = autoImportUsers;

    }

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public Boolean getAutoImportUsers() {
        return autoImportUsers;
    }

    @Override
    public ConfigurationSource getSource() {
        return source;
    }

    @Override
    public void registerUpdateListener(ConfigurationUpdateListener listener) {
    }

    @Override
    public List<ConfigurationUpdateListener> getUpdateListeners() {
        return null;
    }
}

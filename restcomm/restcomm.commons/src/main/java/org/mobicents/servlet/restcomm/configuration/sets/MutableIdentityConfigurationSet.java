package org.mobicents.servlet.restcomm.configuration.sets;

/**
 * Created by otsakir on 12/10/15.
 */
public interface MutableIdentityConfigurationSet extends ConfigurationSet {
    String getMode();
    String getRestcommClientSecret();
    String getInstanceId();
    Boolean getAutoImportUsers();

    String MODE_KEY = "identity.mode";
    String RESTCOMM_CLIENT_SECRET_KEY = "identity.restcomm-client-secret";
    String INSTANCE_ID_KEY = "identity.instance-id";

}

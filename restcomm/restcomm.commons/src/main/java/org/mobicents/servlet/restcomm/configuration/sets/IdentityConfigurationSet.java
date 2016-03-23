package org.mobicents.servlet.restcomm.configuration.sets;

/**
 * Created by otsakir on 12/10/15.
 */
public interface IdentityConfigurationSet {

    String getAuthServerUrl();

    String getRealm();

    String getRealmkey();

    String ADMINISTRATOR_ROLE = "Administrator"; // TODO is this really needed
}

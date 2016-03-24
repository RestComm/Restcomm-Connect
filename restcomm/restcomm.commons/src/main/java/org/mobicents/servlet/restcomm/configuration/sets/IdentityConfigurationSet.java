package org.mobicents.servlet.restcomm.configuration.sets;

/**
 * @author Orestis Tsakiridis
 */
public interface IdentityConfigurationSet {

    String getAuthServerUrl();

    String getRealm();

    String getRealmkey();

    String ADMINISTRATOR_ROLE = "Administrator"; // TODO is this really needed
}

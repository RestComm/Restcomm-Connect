package org.mobicents.servlet.restcomm.configuration.sets;

/**
 * @author Orestis Tsakiridis
 */
public interface IdentityConfigurationSet {

    /**
     * Keycloak base url like: http://authserver:8080/auth.
     *
     * It also controls whether Keycloak or Restcomm will be used as an authorization server. Leave it blank to
     * set Restcomm operate on legacy mode.
     *
     * @return the keycloak base url
     */
    String getAuthServerUrl();

    String getRealm();

    String getRealmkey();

    /**
     * Flags whether Keycloak (external) or Restcomm are used for authentication.
     *
     * @return
     */
    boolean externalAuthEnabled();

    String ADMINISTRATOR_ROLE = "Administrator"; // TODO is this really needed
}

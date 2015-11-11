package org.mobicents.servlet.restcomm.identity.keycloak;

import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * Creates keycloak adapter configuration objects for various keycloak clients/applications.
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class KeycloakConfigurationBuilder {

    private String realmName;
    private String realmKey;
    private String authServerUrl;

    public KeycloakConfigurationBuilder(String realmName, String realmKey, String authServerUrl) {
        super();
        this.realmName = realmName;
        this.realmKey = realmKey;
        this.authServerUrl = authServerUrl;
    }

    public AdapterConfig getUnregisteredRestcommConfig() {
        AdapterConfig config = new AdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("all");
        config.setResource("unregistered-restcomm");
        config.setPublicClient(true);

        return config;
    }



}

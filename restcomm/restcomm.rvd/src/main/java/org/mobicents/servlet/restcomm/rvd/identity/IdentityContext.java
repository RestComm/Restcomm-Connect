package org.mobicents.servlet.restcomm.rvd.identity;

import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityContext {
    String authServerUrl;
    String realm;
    String realmPublicKey;

    IdentityProvider identityProvider;

    public IdentityContext(String authServerUrl, String realm, String realmPublicKey, IdentityProvider identityProvider) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.realmPublicKey = realmPublicKey;
        if (identityProvider == null)
            throw new IllegalStateException("identityProvder is null");
        this.identityProvider = identityProvider;
    }

    /**
     * Returns the KeycloakDeployment object for the current request if available on null.
     *
     * @return
     */
    KeycloakDeployment getKeycloak(RequestOrigin origin) {
        return identityProvider.getKeycloak(origin);


    }

}

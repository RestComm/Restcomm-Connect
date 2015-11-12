package org.mobicents.servlet.restcomm.identity.keycloak;

import org.keycloak.adapters.KeycloakDeployment;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;

public class KeycloakContext {

    private KeycloakDeployment deployment;
    private KeycloakDeployment unregisteredDeployment;

    public KeycloakContext(String realmName, String realmKey, String authServerUrl) {
        KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(realmName, realmKey, authServerUrl);
        this.unregisteredDeployment = IdentityUtils.createDeployment(confBuilder.getUnregisteredRestcommConfig());
    }

    public void refresh(IdentityConfigurationSet identityConfig) {
        KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(identityConfig.getRealm(), identityConfig.getRealmKey(), identityConfig.getAuthServerUrl());
        this.unregisteredDeployment = IdentityUtils.createDeployment(confBuilder.getUnregisteredRestcommConfig());
        this.deployment = IdentityUtils.createDeployment(confBuilder.getRestcommConfig(identityConfig.getInstanceId()));
    }


}

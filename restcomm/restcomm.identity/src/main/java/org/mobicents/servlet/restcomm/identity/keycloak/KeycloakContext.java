package org.mobicents.servlet.restcomm.identity.keycloak;

import org.keycloak.adapters.KeycloakDeployment;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityMigrationConfigurationSet;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityResourceNames;

public class KeycloakContext {
    // keycloak specific properties
    String realmName;
    String realmKey;
    String authServerUrl;
    // our custom 'instance' layer properties
    String instanceId;

    private KeycloakDeployment deployment;
    private KeycloakDeployment unregisteredDeployment;

    private KeycloakContext(String realmName, String realmKey, String authServerUrl, String instanceId, String clientSecret) {
        KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(realmName, realmKey, authServerUrl, instanceId, clientSecret);
        this.unregisteredDeployment = IdentityUtils.createDeployment(confBuilder.getUnregisteredRestcommConfig());
        if ( instanceId != null )
            this.deployment = IdentityUtils.createDeployment(confBuilder.getRestcommConfig());
    }

    public String getRealmName() {
        return realmName;
    }

    public String getRealmKey() {
        return realmKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public KeycloakDeployment getUnregisteredDeployment() {
        return unregisteredDeployment;
    }

    // singleton/factory stuff

    private static KeycloakContext instance;

    public static void init(String realmName, String realmKey, String authServerUrl, String instanceId, String clientSecret) {
        KeycloakContext instance = new KeycloakContext(realmKey, realmKey, authServerUrl, instanceId, clientSecret);
        setInstance(instance);
    }

    public static void init(IdentityConfigurationSet config) {
        KeycloakContext instance = new KeycloakContext(config.getRealm(), config.getRealmKey(), config.getAuthServerUrl(), config.getInstanceId(), config.getRestcommClientSecret());
        setInstance(instance);
    }

    public static void init(IdentityMigrationConfigurationSet config) {
        KeycloakContext instance = new KeycloakContext(config.getRealm(), config.getRealmkey(), config.getAuthServerBaseUrl() + "/auth", null, null);
        setInstance(instance);
    }

    private static void setInstance(KeycloakContext inst) {
        KeycloakContext.instance = inst;
    }


    public static KeycloakContext getInstance() {
        return instance;
    }


    public String getClientName(IdentityResourceNames clientType) {
        switch (clientType) {
            case RESTCOMM_REST: return instanceId + "-restcomm-rest";
            case RESTCOMM_UI: return instanceId + "-restcomm-ui";
            case RESTCOMM_RVD_REST: return instanceId + "-restcomm-rvd-rest";
            case RESTCOMM_RVD_UI: return instanceId + "-restcomm-rvd-ui";
        }
        throw new IllegalStateException("Invalid IdentityResourceName found: " + clientType.toString());
    }


}

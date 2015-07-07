package org.mobicents.servlet.restcomm.http.keycloak;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.representations.adapters.config.BaseAdapterConfig;

/**
 * Builds keycloak adapter configuration for various client applications
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class KeycloakConfigurator {

    // Fixed values for known properties that will help testing.They will be overriden in the long run.
    private final String realmKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    private final String realmName = "restcomm";
    private final String authServerUrl = "https://identity.restcomm.com/auth";

    public KeycloakConfigurator() {
        // TODO Auto-generated constructor stub
    }

    public String getRealmKey() {
        return realmKey;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    // Returns the instance prefix in terms of keycloak client/app.
    // If the instance is not registered it returns "".
    public String getInstancePrefix() {
        return "";
    }

    public BaseAdapterConfig getRestcommConfig() throws IOException {
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource("restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", "password");
        config.setCredentials(credentials);

        return config;
    }

    public BaseAdapterConfig getRestcommUIConfig(String instanceId) throws IOException {
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-ui");
        config.setPublicClient(true);

        return config;
    }

    public BaseAdapterConfig getRestcommRvdUIConfig(String instanceId) throws IOException {
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rvd-ui");
        config.setPublicClient(true);

        return config;
    }

}

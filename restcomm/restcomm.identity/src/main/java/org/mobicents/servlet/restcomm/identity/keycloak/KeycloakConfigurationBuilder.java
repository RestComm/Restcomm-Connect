package org.mobicents.servlet.restcomm.identity.keycloak;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;

/**
 * Creates keycloak adapter configuration objects for various keycloak clients/applications.
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class KeycloakConfigurationBuilder {

    private String realmName;
    private String realmKey;
    private String authServerUrl;
    private String instanceId;
    private String clientSecret;

    public KeycloakConfigurationBuilder(String realmName, String realmKey, String authServerUrl, String instanceId,
            String clientSecret) {
        this.realmName = realmName;
        this.realmKey = realmKey;
        this.authServerUrl = authServerUrl;
        this.instanceId = instanceId;
        this.clientSecret = clientSecret;
    }

    public KeycloakConfigurationBuilder(String realmName, String realmKey, String authServerUrl) {
        this(realmName, realmKey, authServerUrl, null, null);
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

 // Returns null if no instanceId is set
    public AdapterConfig getRestcommConfig(String instanceId) {
        if ( StringUtils.isEmpty(instanceId) )
            return null;
        AdapterConfig config = new AdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);
        config.setUseResourceRoleMappings(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", clientSecret);
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommUIConfig() {
        if ( StringUtils.isEmpty(instanceId))
            return null;
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-ui");
        config.setPublicClient(true);
        config.setUseResourceRoleMappings(true);

        return config;
    }

    // Returns null if no instanceId is set
    public AdapterConfig getRestcommRvdConfig() {
        if ( StringUtils.isEmpty(instanceId))
            return null;
        AdapterConfig config = new AdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rvd");
        config.setCors(true);
        config.setUseResourceRoleMappings(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", clientSecret);
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommRvdUIConfig() {
        if ( StringUtils.isEmpty(instanceId))
            return null;
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rvd-ui");
        config.setPublicClient(true);
        config.setUseResourceRoleMappings(true);

        return config;
    }

}

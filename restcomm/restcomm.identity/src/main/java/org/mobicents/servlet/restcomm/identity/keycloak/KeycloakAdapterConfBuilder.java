package org.mobicents.servlet.restcomm.identity.keycloak;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * Creates keycloak adapter configuration objects for various keycloak clients/applications.
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class KeycloakAdapterConfBuilder {

    private String realmName;
    private String realmKey;
    private String authServerUrl;
    private String instanceId;
    private String clientSecret;

    public KeycloakAdapterConfBuilder(String realmName, String realmKey, String authServerUrl, String instanceId,
                                      String clientSecret) {
        this.realmName = realmName;
        this.realmKey = realmKey;
        this.authServerUrl = authServerUrl;
        this.instanceId = instanceId;
        this.clientSecret = clientSecret;
    }

    public KeycloakAdapterConfBuilder(String realmName, String realmKey, String authServerUrl) {
        this(realmName, realmKey, authServerUrl, null, null);
    }

    // Returns null if no instanceId is set
    public AdapterConfig getRestcommConfig() {
        if ( StringUtils.isEmpty(instanceId) )
            return null;
        AdapterConfig config = new AdapterConfig();
        config.setRealm(realmName);
        config.setRealmKey(realmKey);
        config.setAuthServerUrl(authServerUrl);
        config.setSslRequired("none");
        config.setResource(instanceId + "-restcomm");
        config.setEnableBasicAuth(true);
        config.setCors(true);
        config.setUseResourceRoleMappings(true);

        Map<String,Object> credentials = new HashMap<String,Object>();
        credentials.put("secret", clientSecret);
        config.setCredentials(credentials);

        return config;
    }

}

package org.mobicents.servlet.restcomm.keycloak;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;

/**
 * Builds keycloak adapter configuration for various client applications
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class KeycloakConfigurator {

    public static class CloudIdentityNotSet extends Exception {}

    // Fixed values for known properties that will help testing.They will be overriden in the long run.
    private final String realmKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    private final String realmName = "restcomm";
    private final String authServerUrl = "https://identity.restcomm.com/auth";
    private final String instanceId; // instance ID on identity.restcomm.com
    private final String restcommClientSecret;
    private final String contextPath;

    /*public KeycloakConfigurator(String instanceId) {
        this.instanceId = instanceId;
        // TODO Auto-generated constructor stub
    }*/

    public KeycloakConfigurator(Configuration restcommConfiguration, ServletContext context) {
        this.instanceId = restcommConfiguration.getString("runtime-settings.identity.instance-id");
        this.restcommClientSecret = restcommConfiguration.getString("runtime-settings.identity.restcomm-client-secret");
        this.contextPath = context.getRealPath("/");
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

    public String getInstanceId() {
        return instanceId;
    }

    public String getContextPath() {
        return contextPath;
    }

    public boolean isHookedUpToKeycloak() {
        return ! StringUtils.isEmpty(instanceId);
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommConfig() throws IOException, CloudIdentityNotSet {
        if ( StringUtils.isEmpty(instanceId))
            throw new CloudIdentityNotSet();
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", restcommClientSecret);
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommUIConfig() throws IOException, CloudIdentityNotSet {
        if ( StringUtils.isEmpty(instanceId))
            throw new CloudIdentityNotSet();
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-ui");
        config.setPublicClient(true);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommRvdUIConfig() throws IOException, CloudIdentityNotSet {
        if ( StringUtils.isEmpty(instanceId))
            throw new CloudIdentityNotSet();
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(instanceId + "-restcomm-rvd-ui");
        config.setPublicClient(true);

        return config;
    }

    public void persistRestcommConfig() throws IOException, CloudIdentityNotSet {
        String path = contextPath + "/WEB-INF/keycloak.json";

        if ( ! isHookedUpToKeycloak() ) {
            //FileUtils.deleteQuietly(new File(path));
            // do nothing. Leave old keycloak.json be if already there.
            return;
        }

        BaseAdapterConfig config = getRestcommConfig();
        FileOutputStream out = new FileOutputStream(path);
        JsonSerialization.writeValueToStream(out, config);

        return;
    }

    // Update keycloak adapter configuration for all applications
    public void persistAdaptersConfig() throws IOException {
        try {
            persistRestcommConfig();
            // TODO - other application here
        } catch (CloudIdentityNotSet e) {
            // do nothing. Configuration option is missing, no harm done.
        }
    }

}

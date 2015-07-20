package org.mobicents.servlet.restcomm.identity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;

/**
 * Builds keycloak adapter configuration for various client applications
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class KeycloakConfigurator {
    protected Logger logger = Logger.getLogger(KeycloakConfigurator.class);

    public static class CloudIdentityNotSet extends Exception {}

    private static KeycloakConfigurator singleInstance;

    public enum IdentityMode {
        init,
        cloud,
        standalone
    }

    // Fixed values for known properties that will help testing.They will be overriden in the long run.
    private final String realmKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    private final String realmName = "restcomm";
    private final String authServerUrl = "https://identity.restcomm.com/auth";
    private final String cloudInstanceId; // instance ID on identity.restcomm.com
    private final String restcommClientSecret;
    private final String contextPath;
    private final String identityModeInConfig;
    private final IdentityMode identityMode;

    public static KeycloakConfigurator create(Configuration restcommConfiguration, ServletContext context) {
        // TODO - throw an exception if the instance has already been created??
        if (singleInstance != null) {
            throw new IllegalStateException("Singleton KeycloakConfigurator instance has already been created.");
        }

        singleInstance = new KeycloakConfigurator(restcommConfiguration, context);
        return singleInstance;
    }

    public static KeycloakConfigurator getInstance() {
        if ( singleInstance == null )
            throw new IllegalStateException("KeycloakConfigurator singleton has not been created yet. Make sure restcomm bootstrapper has run.");

        return singleInstance;
    }

    private KeycloakConfigurator(Configuration restcommConfiguration, ServletContext context) {
        this.identityModeInConfig = restcommConfiguration.getString("runtime-settings.identity.mode");
        this.cloudInstanceId = restcommConfiguration.getString("runtime-settings.identity.instance-id");
        this.restcommClientSecret = restcommConfiguration.getString("runtime-settings.identity.restcomm-client-secret");
        this.contextPath = context.getRealPath("/");
        this.identityMode = determineMode();
        logger.info("Restcomm now operating in '" + identityMode + "' identity mode. Instance id: " + getCloudInstanceId());
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

    public String getCloudInstanceId() {
        return cloudInstanceId;
    }

    public String getContextPath() {
        return contextPath;
    }

    /**
     * Implements logic for determining identity mode based on config.
     * By default 'init' is used. If mode is missing from conf and instance-id is defined return 'cloud'. Return 'standalone' only if explicitly set to that value.
     * @return
     */
    private IdentityMode determineMode() {
        IdentityMode mode;
        // fallback to 'init' mode if crap on nothing is entered
        try {
            mode = IdentityMode.valueOf(identityModeInConfig);
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.warn("Invalid or missing identity mode configuration in restcomm.xml");
            mode = IdentityMode.init;
        }
        // if instance-id is defined assume we are in the cloud
        if ( getCloudInstanceId() != null && mode != IdentityMode.standalone )
            mode = IdentityMode.cloud;

        return mode;
    }

    /**
     * @return The effective identity mode. See {@link #determineMode()} to get an idea how this is found.
     */
    public IdentityMode getMode() {
        return identityMode;
    }

    public boolean isHookedUpToKeycloak() {
        return ! StringUtils.isEmpty(cloudInstanceId);
    }

    // Returns null if no instanceId is set
    public AdapterConfig getRestcommConfig() throws CloudIdentityNotSet {
        if ( StringUtils.isEmpty(cloudInstanceId))
            throw new CloudIdentityNotSet();
        AdapterConfig config = new AdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(cloudInstanceId + "-restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", restcommClientSecret);
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommUIConfig() throws CloudIdentityNotSet {
        if ( StringUtils.isEmpty(cloudInstanceId))
            throw new CloudIdentityNotSet();
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(cloudInstanceId + "-restcomm-ui");
        config.setPublicClient(true);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommRvdUIConfig() throws CloudIdentityNotSet {
        if ( StringUtils.isEmpty(cloudInstanceId))
            throw new CloudIdentityNotSet();
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(cloudInstanceId + "-restcomm-rvd-ui");
        config.setPublicClient(true);

        return config;
    }

    // not used
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

    // not used - Update keycloak adapter configuration for all applications
    public void persistAdaptersConfig() throws IOException {
        try {
            persistRestcommConfig();
            // TODO - other application here
        } catch (CloudIdentityNotSet e) {
            // do nothing. Configuration option is missing, no harm done.
        }
    }

}

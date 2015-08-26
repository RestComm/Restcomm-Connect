package org.mobicents.servlet.restcomm.identity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
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
public class IdentityConfigurator {
    protected Logger logger = Logger.getLogger(IdentityConfigurator.class);

    public static class CloudIdentityNotSet extends Exception {}

    private static IdentityConfigurator singleInstance;

    public enum IdentityMode {
        init,
        cloud,
        standalone
    }

    // Fixed values for known properties that will help testing.They will be overriden in the long run.
    private final String realmKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    private final String realmName = "restcomm";
    //private String authServerUrl = "https://identity.restcomm.com/auth";
    private String authServerUrlBase = "https://identity.restcomm.com";
    private String cloudInstanceId; // instance ID on identity.restcomm.com
    private String restcommClientSecret;
    private final String contextPath;
    private final String identityModeInConfig;
    private IdentityMode identityMode;
    private static final String IDENTITY_PROXY_CONTEXT_NAME = "restcomm-identity";

    public static IdentityConfigurator create(Configuration restcommConfiguration, ServletContext context) {
        // TODO - throw an exception if the instance has already been created??
        if (singleInstance != null) {
            throw new IllegalStateException("Singleton KeycloakConfigurator instance has already been created.");
        }

        singleInstance = new IdentityConfigurator(restcommConfiguration, context);
        return singleInstance;
    }

    public static IdentityConfigurator getInstance() {
        if ( singleInstance == null )
            throw new IllegalStateException("KeycloakConfigurator singleton has not been created yet. Make sure restcomm bootstrapper has run.");

        return singleInstance;
    }

    private IdentityConfigurator(Configuration restcommConfiguration, ServletContext context) {
        this.identityModeInConfig = restcommConfiguration.getString("runtime-settings.identity.mode");
        this.cloudInstanceId = restcommConfiguration.getString("runtime-settings.identity.instance-id");
        this.restcommClientSecret = restcommConfiguration.getString("runtime-settings.identity.restcomm-client-secret");
        this.authServerUrlBase = restcommConfiguration.getString("runtime-settings.identity.auth-server-url-base");
        this.contextPath = context.getRealPath("/");
        this.identityMode = determineMode();
        logger.info("Restcomm is now operating in '" + identityMode + "' identity mode" + (StringUtils.isEmpty(this.authServerUrlBase) ? "" : " using authorization server " + this.authServerUrlBase )  +". Instance id: " + getCloudInstanceId());
    }

    public String getRealmKey() {
        return realmKey;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getAuthServerUrl() {
        return authServerUrlBase + "/auth";
    }

    public String getIdentityProxyUrl() {
        return authServerUrlBase + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }

    public static String getIdentityProxyUrl(String authServerUrlBase ) {
        return authServerUrlBase + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }


    public void setAuthServerUrlBase(String authServerUrlBase) {
        this.authServerUrlBase = authServerUrlBase;
    }

    public String getCloudInstanceId() {
        return cloudInstanceId;
    }

    public void setCloudInstanceId(String identityInstanceId) {
        this.cloudInstanceId = identityInstanceId;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setRestcommClientSecret(String restcommClientSecret) {
        this.restcommClientSecret = restcommClientSecret;
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

    public void setMode(IdentityMode mode) {
        this.identityMode = mode;
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
        config.setUseResourceRoleMappings(true);

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
        config.setUseResourceRoleMappings(true);

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

    // update central configuration file restcomm.xml with current identity options.
    public void updateRestcommXml(XMLConfiguration restcommConfiguration) {
        //logger.info("restcomm.xml has been updated with the following identity settings: mode = " + getMode().toString() + ", instance-id = " + cloudInstanceId + ", restcomm-client-secret = " + restcommClientSecret + ", auth-server-url-base = " + authServerUrlBase );
        restcommConfiguration.setProperty("runtime-settings.identity.mode", getMode().toString() );
        restcommConfiguration.setProperty("runtime-settings.identity.instance-id", cloudInstanceId);
        restcommConfiguration.setProperty("runtime-settings.identity.restcomm-client-secret", restcommClientSecret );
        restcommConfiguration.setProperty("runtime-settings.identity.auth-server-url-base", authServerUrlBase );
        //try {
            logger.warn("restcomm.xml has not been updated. Here are the respective identity settings: mode = " + getMode().toString() + ", instance-id = " + cloudInstanceId + ", restcomm-client-secret = " + restcommClientSecret + ", auth-server-url-base = " + authServerUrlBase );
            //restcommConfiguration.save();
        //} catch (ConfigurationException e) {
        //    logger.warn("restcomm.xml couldn't be updated on the fly. You will need to stop, update manually and start your server. Here are the details: mode = " + getMode().toString() + ", instance-id = " + cloudInstanceId + ", restcomm-client-secret = " + restcommClientSecret + ", auth-server-url-base = " + authServerUrlBase );
        //}
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

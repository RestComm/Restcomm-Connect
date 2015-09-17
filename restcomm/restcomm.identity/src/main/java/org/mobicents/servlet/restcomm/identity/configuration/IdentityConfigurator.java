package org.mobicents.servlet.restcomm.identity.configuration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.mobicents.servlet.restcomm.configuration.ConfiguratorBase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IdentityConfigurator extends ConfiguratorBase implements IdentityConfigurationSet  {
    protected Logger logger = Logger.getLogger(IdentityConfigurator.class);

    protected static final IdentityMode DEFAULT_IDENTITY_MODE = IdentityMode.init;
    protected static final String DEFAULT_REALM_NAME = "restcomm";
    protected static final String DEFAULT_AUTH_SERVER_URL_BASE = "https://identity.restcomm.com";
    protected static final String DEFAULT_REALM_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";
    protected static final String IDENTITY_PROXY_CONTEXT_NAME = "restcomm-identity";
    private static final Boolean DEFAULT_AUTO_IMPORT_USERS = true; // move this option to the configuration file at some point

    protected IdentityMode identityMode;
    protected String identityInstanceId; // instance ID on authorization server
    protected String restcommClientSecret;
    protected String authServerUrlBase;
    protected String realmPublicKey;
    protected String realmName;
    protected KeycloakDeployment deployment;

    protected static IdentityConfigurator singleInstance;

    IdentityConfigurationSource configurationSource;

    public static IdentityConfigurator create(IdentityConfigurationSource source, ServletContext servletContext) {
        // TODO - throw an exception if the instance has already been created??
        if (singleInstance != null) {
            throw new IllegalStateException("Singleton KeycloakConfigurator instance has already been created.");
        }

        singleInstance = new IdentityConfigurator(source, servletContext);
        return singleInstance;
    }

    public static IdentityConfigurator getInstance() {
        if ( singleInstance == null )
            throw new IllegalStateException("KeycloakConfigurator singleton has not been created yet. Make sure restcomm bootstrapper has run.");

        return singleInstance;
    }

    private IdentityConfigurator(IdentityConfigurationSource source, ServletContext servletContext) {
        this.configurationSource = source;
        this.realmName = DEFAULT_REALM_NAME;
        this.realmPublicKey = DEFAULT_REALM_PUBLIC_KEY;
        registerUpdateListener(new RvdConfigurationUpdateListener(servletContext)); // RVD needs to know when identity configuratio is updated
        load();
    }

    public IdentityConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    // Loads configuration settings from the configuration source and initializes configurator
    @Override
    public void load() {
        this.identityMode = loadMode();
        this.authServerUrlBase = loadAuthServerUrlBase();
        if (StringUtils.isEmpty(this.authServerUrlBase)) {
            logger.warn("Missing authorization server configuration. Please set 'identity.auth-server-url-base' configuration setting.");
        }
        if (this.identityMode != IdentityMode.init) {
            // looks like we need to load other configuration options too
            this.identityInstanceId = loadInstanceId();
            this.restcommClientSecret = loadRestcommClientSecret();
            try {
                this.deployment = KeycloakDeploymentBuilder.build(this.getRestcommConfig());
            } catch (IdentityNotSet e) {
                logger.error("Instance id is missing from configuration. Restcomm won't be properly initialized. Please set 'identity.instance-id' configuration setting.", e );
            }
        }
        // notify all who are interected when there is new identity-specific configuration
        notifyUpdateListeners();

        if ( this.identityMode == IdentityMode.init) {
            logger.info("Restcomm is now operating in 'init' mode. Only restration capabilities will be available");
        } else
            logger.info("Restcomm is now operating in '" + identityMode + "' identity mode" + (StringUtils.isEmpty(this.authServerUrlBase) ? "" : " using authorization server " + this.authServerUrlBase )  +". Instance id: " + identityInstanceId);
    }

    @Override
    public void save() {
        configurationSource.saveMode(identityMode.toString());
        configurationSource.saveInstanceId(identityInstanceId);
        configurationSource.saveRestcommClientSecret(restcommClientSecret);
        configurationSource.saveAuthServerUrlBase(authServerUrlBase);
        logger.debug("Persisted identity specific configuration to storage");
        // notify people
        notifyUpdateListeners();
    }

    private IdentityMode loadMode() {
        String mode = configurationSource.loadMode();
        IdentityMode identityMode;
        if (StringUtils.isEmpty(mode))
            identityMode = DEFAULT_IDENTITY_MODE;
        else
            identityMode = IdentityMode.valueOf(mode); // TODO in case of conversion error throw a InvalidIdentityConfiguration exception
        return identityMode;
    }

    private String loadInstanceId() {
        return configurationSource.loadInstanceId();
    }

    private String loadRestcommClientSecret() {
        return configurationSource.loadRestcommClientSecret();
    }

    private String loadAuthServerUrlBase() {
        return configurationSource.loadAuthServerUrlBase();
    }

    public IdentityMode getMode() {
        return identityMode;
    }

    public String getRealmPublicKey() {
        return realmPublicKey;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getAuthServerUrl() {
        return authServerUrlBase + "/auth";
    }

    public String getAuthServerUrlBase() {
        return authServerUrlBase;
    }

    public String getIdentityProxyUrl() {
        return authServerUrlBase + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }

    public String getIdentityProxyUrl(String authServerUrlBase ) {
        return authServerUrlBase + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }

    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    @Override
    public String getRealmKey() {
        return realmPublicKey;
    }

    @Override
    public String getIdentityInstanceId() {
        return identityInstanceId;
    }

    @Override
    public Boolean getAutoImportUsers() {
        return DEFAULT_AUTO_IMPORT_USERS;
    }

    @Override
    public String getClientName(IdentityResourceNames clientType) {
        switch (clientType) {
            case RESTCOMM_REST: return identityInstanceId + "-restcomm-rest";
            case RESTCOMM_UI: return identityInstanceId + "-restcomm-ui";
            case RESTCOMM_RVD_REST: return identityInstanceId + "-restcomm-rvd-rest";
            case RESTCOMM_RVD_UI: return identityInstanceId + "-restcomm-rvd-ui";
        }
        throw new IllegalStateException("Invalid IdentityResourceName found: " + clientType.toString());
    }

    @Override
    public void setAuthServerUrlBase(String urlBase) {
        this.authServerUrlBase = urlBase;
    }

    @Override
    public void setMode(IdentityMode mode) {
        this.identityMode = mode;
    }

    @Override
    public void setRestcommClientSecret(String secret) {
        this.restcommClientSecret = secret;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.identityInstanceId = instanceId;
    }

    // Returns null if no instanceId is set
    public AdapterConfig getRestcommConfig() throws IdentityNotSet {
        if ( StringUtils.isEmpty(getIdentityInstanceId()))
            throw new IdentityNotSet("Error while building keycloak adapter configuration for restcomm-rest client");
        AdapterConfig config = new AdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(getIdentityInstanceId() + "-restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);
        config.setUseResourceRoleMappings(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", getRestcommClientSecret());
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommUIConfig() throws IdentityNotSet {
        if ( StringUtils.isEmpty(getIdentityInstanceId()))
            throw new IdentityNotSet("Error while building keycloak adapter configuration for restcomm-ui client");
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(getIdentityInstanceId() + "-restcomm-ui");
        config.setPublicClient(true);
        config.setUseResourceRoleMappings(true);

        return config;
    }

    // Returns null if no instanceId is set
    public AdapterConfig getRestcommRvdConfig() throws IdentityNotSet {
        if ( StringUtils.isEmpty(getIdentityInstanceId()))
            throw new IdentityNotSet("Error while building keycloak adapter configuration for restcomm-rvd client");
        AdapterConfig config = new AdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(getIdentityInstanceId() + "-restcomm-rvd");
        config.setCors(true);
        config.setUseResourceRoleMappings(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", getRestcommClientSecret());
        config.setCredentials(credentials);

        return config;
    }

    // Returns null if no instanceId is set
    public BaseAdapterConfig getRestcommRvdUIConfig() throws IdentityNotSet {
        if ( StringUtils.isEmpty(getIdentityInstanceId()))
            throw new IdentityNotSet("Error while building keycloak adapter configuration for restcomm-rvd-ui client");
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm(getRealmName());
        config.setRealmKey(getRealmKey());
        config.setAuthServerUrl(getAuthServerUrl());
        config.setSslRequired("all");
        config.setResource(getIdentityInstanceId() + "-restcomm-rvd-ui");
        config.setPublicClient(true);
        config.setUseResourceRoleMappings(true);

        return config;
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public void writeAdapterConfigToFile(AdapterConfig adapterConfig, String filepath) {
        try {
            AdapterConfigEntity entity = new AdapterConfigEntity();

            entity.setRealm(adapterConfig.getRealm());
            entity.setRealmPublicKey(adapterConfig.getRealmKey());
            entity.setResource(adapterConfig.getResource());
            entity.setAuthServerUrl(adapterConfig.getAuthServerUrl());
            entity.setBearerOnly(adapterConfig.isBearerOnly());
            entity.setPublicClient(adapterConfig.isPublicClient());
            entity.setSslRequired(adapterConfig.getSslRequired());
            entity.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(filepath);
            gson.toJson(entity, writer);
            writer.close();

        } catch (IOException e) {
            logger.error("Error saving keycloak adapter configuration for '" + adapterConfig.getResource() + "' to '" + filepath + "'" );
        }
    }


    // Identity-specific exceptions

    /**
     * Thrown when an instance is not bound to an authorization server but it should be.
     *
     * @author "Tsakiridis Orestis"
     */
    public static class IdentityNotSet extends Exception {
        private static final long serialVersionUID = 239483444282400569L;
        public IdentityNotSet() {
            super();
            // TODO Auto-generated constructor stub
        }
        public IdentityNotSet(String message) {
            super(message);
            // TODO Auto-generated constructor stub
        }
    }

}

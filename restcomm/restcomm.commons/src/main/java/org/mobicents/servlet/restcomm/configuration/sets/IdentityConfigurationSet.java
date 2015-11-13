package org.mobicents.servlet.restcomm.configuration.sets;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class IdentityConfigurationSet extends ConfigurationSet {

    public static final String AUTH_SERVER_BASE_URL_KEY = "identity.auth-server-base-url";
    public static final String MODE_KEY = "identity.mode";
    public static final String RESTCOMM_CLIENT_SECRET_KEY = "identity.restcomm-client-secret";
    public static final String INSTANCE_ID_KEY = "identity.instance-id";
    public static final String REALM_KEY = "identity.realm";
    public static final String REALM_KEY_KEY = "identity.realmKey";

    private final String authServerBaseUrl;
    private final String mode;
    private final String restcommClientSecret;
    private final String instanceId;
    private final String realm;
    private final String realmKey;

    private static final String MODE_DEFAULT = "init";
    private static final String REALM_DEFAULT = "restcomm";
    // TODO - don't forget to use generate a new public key for identity.restcomm.com and put it here
    private static final String REALM_KEY_DEFAULT = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv";

    // other static stuff to keep them all in a single place
    public static final String IDENTITY_PROXY_CLIENT_NAME = "restcomm-identity-rest";
    public static final String IDENTITY_PROXY_CONTEXT_NAME = "restcomm-identity";

    public IdentityConfigurationSet(ConfigurationSource source) {
        super(source);
        // authServerBaseUrl option
        this.authServerBaseUrl = source.getProperty(AUTH_SERVER_BASE_URL_KEY);
        // mode option
        String mode = source.getProperty(MODE_KEY);
        if (StringUtils.isEmpty(mode))
            this.mode = MODE_DEFAULT;
        else
        if (validateMode(mode))
            this.mode = mode;
        else
            throw new RuntimeException("Error initializing '" + MODE_KEY + "' configuration setting. Invalid value: " + mode);
        // restcommClientSecret option
        this.restcommClientSecret = source.getProperty(RESTCOMM_CLIENT_SECRET_KEY);
        // instanceId option
        this.instanceId = source.getProperty(INSTANCE_ID_KEY);
        // realm option
        String realm = source.getProperty(REALM_KEY);
        if (StringUtils.isEmpty(realm))
            this.realm = REALM_DEFAULT;
        else
            this.realm = realm;
        // realm key option
        String realmKey = source.getProperty(REALM_KEY_KEY);
        if (StringUtils.isEmpty(realmKey))
            this.realmKey = REALM_KEY_DEFAULT;
        else
            this.realmKey = realmKey;
    }

    private boolean validateMode(String mode) {
        if (!StringUtils.isEmpty(mode)) {
            if (mode.equals("init") || mode.equals("cloud") || mode.equals("standalone"))
                return true;
        }
        return false;
    }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public String getMode() {
        return mode;
    }

    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getRealm() {
        return realm;
    }

    public String getRealmKey() {
        return realmKey;
    }

    // secondary getters built on top of configuration defined above

    public String getAuthServerUrl() {
        return getAuthServerBaseUrl() + "/auth";
    }

    // static getters

    public static String getAuthServerUrl(String authServerBaseUrl) {
        return authServerBaseUrl + "/auth";
    }

    public static String getIdentityProxyUrl(String authServerBaseUrl) {
        return authServerBaseUrl + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }

}

package org.mobicents.servlet.restcomm.configuration.sets;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class IdentityConfigurationSet extends ConfigurationSet {
    private static Logger logger = Logger.getLogger(IdentityConfigurationSet.class);

    // identity connectivity keys
    public static final String HEADLESS_KEY = "identity.headless";
    public static final String AUTH_SERVER_BASE_URL_KEY = "identity.auth-server-base-url";
    public static final String REALM_KEY_KEY = "identity.realm-public-key";
    // migration/registration-specific keys
    public static final String USERNAME_KEY = "identity.migration.username";
    public static final String PASSWORD_KEY = "identity.migration.password";
    public static final String INVITE_EXISTING_USERS_KEY = "identity.migration.invite-existing-users";
    public static final String ADMIN_ACCOUNT_SID_KEY = "identity.migration.admin-account-sid";
    public static final String REDIRECT_URIS_KEY = "identity.migration.redirect-uris";
    public static final String METHOD_KEY = "identity.migration.method";

    // identity connectivity variables
    private final Boolean headless;
    private final String authServerBaseUrl;
    private final String realm;
    private final String realmkey;
    // migration/registration specific variables
    private final String username;
    private final String password;
    private final Boolean inviteExistingUsers;
    private final String adminAccountSid;
    private final String[] redirectUris;
    private MigrationMethod method;

    // default values
    public static final Boolean HEADLESS_DEFAULT = false;
    public static final String AUTH_SERVER_BASE_URL_DEFAULT = "https://identity.restcomm.com";
    public static final Boolean INVITE_EXISTING_USERS_DEFAULT = false;
    public static final String REALM_DEFAULT = "restcomm";
    // TODO replace this default value (or remove it??)
    public static final String REALM_KEY_DEFAULT = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv";
    public static final MigrationMethod METHOD_DEFAULT = MigrationMethod.ui;

    // other static stuff to keep them all in a single place
    public static final String IDENTITY_PROXY_CLIENT_NAME = "restcomm-identity-rest";
    public static final String IDENTITY_PROXY_CONTEXT_NAME = "restcomm-identity";
    public static final String ADMINISTRATOR_ROLE = "Administrator";

    public IdentityConfigurationSet(ConfigurationSource source) {
        super(source,null);
        // headless option
        String headlessString = source.getProperty(HEADLESS_KEY);
        if (StringUtils.isEmpty(headlessString))
            this.headless = HEADLESS_DEFAULT;
        else
        try {
            this.headless = Boolean.parseBoolean(headlessString);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + HEADLESS_KEY + "' configuration option",e);
        }
        if (this.headless) {
            // disable all other identity options
            this.authServerBaseUrl = null;
            this.realm = null;
            this.realmkey = null;
            this.username = null;
            this.password = null;
            this.inviteExistingUsers = null;
            this.adminAccountSid = null;
            this.redirectUris = null;

            return;
        }
        // authServerBaseUrl option
        String authServerBaseUrl = source.getProperty(AUTH_SERVER_BASE_URL_KEY);
        if (StringUtils.isEmpty(authServerBaseUrl))
            authServerBaseUrl = AUTH_SERVER_BASE_URL_DEFAULT;
        this.authServerBaseUrl = authServerBaseUrl;
        // username option
        this.username = source.getProperty(USERNAME_KEY);
        // password option
        this.password = source.getProperty(PASSWORD_KEY);
        // inviteExistingUsersRaw option
        try {
            String inviteExistingUsersRaw = source.getProperty(INVITE_EXISTING_USERS_KEY);
            if (StringUtils.isEmpty(inviteExistingUsersRaw))
                this.inviteExistingUsers = INVITE_EXISTING_USERS_DEFAULT;
            else {
                this.inviteExistingUsers = Boolean.parseBoolean(inviteExistingUsersRaw);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + INVITE_EXISTING_USERS_KEY + "' configuration setting", e);
        }
        // adminAccountSid option
        this.adminAccountSid = source.getProperty(ADMIN_ACCOUNT_SID_KEY);
        // redirect uris option
        String redirectUrisRaw = source.getProperty(REDIRECT_URIS_KEY);
        if (!StringUtils.isEmpty(redirectUrisRaw))
            this.redirectUris = redirectUrisRaw.split(",");
        else
            this.redirectUris = null;
        // realm option. Not loaded from source yet. Just a placeholder for the defaults.
        this.realm = REALM_DEFAULT;
        // realmKey option
        String realmkey = source.getProperty(REALM_KEY_KEY);
        if(StringUtils.isEmpty(realmkey))
            this.realmkey = REALM_KEY_DEFAULT;
        else {
            this.realmkey = realmkey;
        }

        // method option
        try {
            this.method = MigrationMethod.valueOf(source.getProperty(METHOD_KEY));
        } catch (IllegalArgumentException | NullPointerException e){
            logger.warn("Error parsing '" + METHOD_KEY + "' property in restcomm.xml. Using default: '" + METHOD_DEFAULT.toString() + "'" , e);
            this.method = METHOD_DEFAULT;
        }

        this.reloaded();
    }

    public Boolean getHeadless() { return headless; }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Boolean getInviteExistingUsers() {
        return inviteExistingUsers;
    }

    public String getAdminAccountSid() {
        return adminAccountSid;
    }

    public String[] getRedirectUris() {
        return redirectUris;
    }

    public String getRealm() {
        return realm;
    }

    public String getRealmkey() {
        return realmkey;
    }

    public MigrationMethod getMethod() {
        return method;
    }

    public String getAuthServerUrl() {
        return authServerBaseUrl + "/auth";
    }

    public static String getAuthServerUrl(String authServerBaseUrl) {
        return authServerBaseUrl + "/auth";
    }

    public static String getIdentityProxyUrl(String authServerBaseUrl) {
        return authServerBaseUrl + "/" + IDENTITY_PROXY_CONTEXT_NAME;
    }

    public enum MigrationMethod {
        startup,
        ui
    }

}

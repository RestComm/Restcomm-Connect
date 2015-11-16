package org.mobicents.servlet.restcomm.configuration.sets;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class IdentityMigrationConfigurationSet extends ConfigurationSet {
    // keys to retrieve options from source
    public static final String AUTH_SERVER_BASE_URL_KEY = "identity.migration.auth-server-url-base";
    public static final String USERNAME_KEY = "identity.migration.username";
    public static final String PASSWORD_KEY = "identity.migration.password";
    public static final String INVITE_EXISTING_USERS_KEY = "identity.migration.invite-existing-users";
    public static final String ADMIN_ACCOUNT_SID_KEY = "identity.migration.admin-account-sid";
    public static final String REDIRECT_URIS_KEY = "identity.migration.redirect-uris";

    private final String authServerBaseUrl;
    private final String username;
    private final String password;
    private final Boolean inviteExistingUsers;
    private final String adminAccountSid;
    private final String[] redirectUris;
    private final String realm;
    private final String realmkey;

    // default values
    public static final String AUTH_SERVER_BASE_URL_DEFAULT = "https://identity.restcomm.com";
    public static final Boolean INVITE_EXISTING_USERS_DEFAULT = false;
    public static final String REALM_DEFAULT = "restcomm";
    public static final String REALM_KEY_DEFAULT = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv";

    public IdentityMigrationConfigurationSet(ConfigurationSource source) {
        super(source,null);
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
        this.realmkey = REALM_KEY_DEFAULT;

        this.reloaded();
    }

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

}

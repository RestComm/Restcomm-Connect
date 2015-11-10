package org.mobicents.servlet.restcomm.configuration.sets;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class IdentityMigrationConfigurationSet extends ConfigurationSet {
    // keys to retrieve options from source
    public static final String AUTH_SERVER_BASE_URL_KEY = "identity.migration.auth-server-url-base";
    public static final String USERNAME_KEY = "identity.migration.username";
    public static final String PASSWORD_KEY = "identity.migration.password";
    public static final String INVITE_EXISTING_USERS_KEY = "identity.migration.invite-existing-users";

    private final String authServerBaseUrl;
    private final String username;
    private final String password;
    private final Boolean inviteExistingUsers;

    // default values
    public static final String AUTH_SERVER_BASE_URL_DEFAULT = "https://identity.restcomm.com";
    public static final Boolean INVITE_EXISTING_USERS_DEFAULT = false;

    public IdentityMigrationConfigurationSet(ConfigurationSource source) {
        super(source);
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

}

package org.mobicents.servlet.restcomm.configuration.sets;

/**
 * Created by otsakir on 12/10/15.
 */
public interface IdentityConfigurationSet {

    Boolean getHeadless();

    String getAuthServerBaseUrl();

    String getUsername();

    String getPassword();

    Boolean getInviteExistingUsers();

    String getAdminAccountSid();

    String[] getRedirectUris();

    String getRealm();

    String getRealmkey();

    MigrationMethod getMethod();

    String getAuthServerUrl();

    enum MigrationMethod {
        startup,
        ui
    }

    String ADMINISTRATOR_ROLE = "Administrator";
}

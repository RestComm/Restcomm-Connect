/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.identity.migration;

import java.util.List;
import java.util.UUID;

import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sources.MutableConfigurationSource;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.identity.IdentityMode;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.identity.keycloak.IdentityContext;

import javax.servlet.ServletContext;

/**
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class IdentityMigrationTool {
    private static Logger logger = Logger.getLogger(IdentityMigrationTool.class);

    private AccountsDao accountsDao;
    private RestcommIdentityApi identityApi;
    private boolean inviteExistingUsers;
    private String adminAccountSid;
    private MutableIdentityConfigurationSet mutableIdentityConfiguration; // needed to persist new configuration after migration
    // the following are updated throughout the migration
    private String instanceId;
    private String clientSecret;
    private String[] redirectUris;

    /*
        Wrapper function for identity-oriented bootstraper logic
     */
    public static void onBootstrap(RestcommConfiguration config, AccountsDao accountsDao, ServletContext servletContext) {
        IdentityConfigurationSet identityConfig = config.getIdentity();
        if ( ! identityConfig.getHeadless() ) {
            MutableIdentityConfigurationSet mutableIdentityConfig = config.getMutableIdentity();
            if (identityConfig.getMethod().equals(IdentityConfigurationSet.MigrationMethod.startup) && !"cloud".equals(mutableIdentityConfig.getMode())) {
                RestcommIdentityApi api = new RestcommIdentityApi(identityConfig.getAuthServerBaseUrl(), identityConfig.getUsername(), identityConfig.getPassword(), identityConfig.getRealm(), null);
                IdentityMigrationTool migrationTool = new IdentityMigrationTool(accountsDao, api, identityConfig.getInviteExistingUsers(), identityConfig.getAdminAccountSid(), mutableIdentityConfig, identityConfig.getRedirectUris());
                migrationTool.migrate();
                config.reloadMutableIdentity();
                // Reset identity context after migration
                IdentityContext oldContext = (IdentityContext) servletContext.getAttribute(IdentityContext.class.getName());
                IdentityContext identityContext = new IdentityContext(config.getIdentity(),config.getMutableIdentity(), oldContext.getRestcommRoles());
                servletContext.setAttribute(IdentityContext.class.getName(), identityContext);
            }
        }
    }

    public IdentityMigrationTool(AccountsDao dao, RestcommIdentityApi identityApi, boolean inviteExisting, String adminAccountSid, MutableIdentityConfigurationSet mutableIdentityConfig, String[] redirectUris) {
        super();
        this.accountsDao = dao;
        this.identityApi = identityApi;
        this.inviteExistingUsers = inviteExisting;
        this.adminAccountSid = adminAccountSid;
        this.mutableIdentityConfiguration = mutableIdentityConfig;
        this.redirectUris = redirectUris;
    }

    public IdentityMigrationTool(AccountsDao accountsDao, RestcommIdentityApi identityApi, boolean inviteExistingUsers) {
        this(accountsDao, identityApi, inviteExistingUsers, null, null, null);
    }

    public void migrate() {
        report("---------- MIGRATION START ----------");
        report("Using auth server at " + identityApi.getAuthServerBaseUrl());
        if ( registerInstance(redirectUris) == false ) {
            report("Migration FAILED!!!");
            return;
        }
        migrateUsers();
        linkAdministratorAccount();
        updateConfiguration();
        report("---------- MIGRATION END ----------");
        logger.info("Using auth server at: " + identityApi.getAuthServerBaseUrl() + ". Instance ID: " + instanceId);
    }

    /**
     * Registers an instance to an authorization server. The keycloak Clients are created.
     * @param redirectUris
     * @return true if successfull, false otherwise
     */
    boolean registerInstance(String[] redirectUris) {
        report("--- Registration phase ---");
        try {
            String clientSecret = generateRandomInstanceSecret();
            this.instanceId = identityApi.createInstance(redirectUris, clientSecret).instanceId;
            this.clientSecret = clientSecret;
        } catch (RestcommIdentityApiException e) {
            report(e.getMessage());
            return false;
        }
        identityApi.bindInstance(this.instanceId);
        report("Registered instance '" + this.instanceId + "' successfully");
        return true;
    }

    void migrateUsers() {
        report("--- User migration phase ---");
        List<Account> accounts = accountsDao.getAccounts(); // retrieve all available accounts
        for (Account account: accounts) {
            migrateAccount(account);
        }
    }

    boolean linkAdministratorAccount() {
        report("--- Processing administrator account ---");
        if (adminAccountSid == null)
            adminAccountSid = guessAccountSid();

        if (adminAccountSid == null) {
            //report("Error. No administrator account found to be linked to '" + username +"'. Please set 'identity.migration.admin-account-sid' in restcomm.xml.");
            return false;
        }

        Account adminAccount = accountsDao.getAccount(adminAccountSid);
        if (adminAccount == null) {
            String message = "FAILED to link migration user '" + identityApi.getUsername() + "' to local administration account. No account with sid '" + adminAccountSid + "' found.";
            report(message);
            logger.error(message);
            return false;
        }
        adminAccount = adminAccount.setEmailAddress(identityApi.getUsername());
        accountsDao.updateAccount(adminAccount);
        report("User '" + identityApi.getUsername() + "' was granted administrator access to instance '" + identityApi.getBoundInstanceId() + "'");

        return true;
    }

    String guessAccountSid() {
        report("WARNING: Using a hardcoded admin account sid!");
        return "ACae6e420f425248d6a26948c17a9e2acf";
        //throw new UnsupportedOperationException();
    }

    void updateConfiguration() {
        report("--- Updating muttable configuration ---");
        if ( mutableIdentityConfiguration == null )
            throw new UnsupportedOperationException();
        MutableConfigurationSource source = (MutableConfigurationSource) mutableIdentityConfiguration.getSource();
        //source.setProperty(mutableIdentityConfiguration.AUTH_SERVER_BASE_URL_KEY, identityApi.getAuthServerBaseUrl());
        source.setProperty(mutableIdentityConfiguration.MODE_KEY, IdentityMode.cloud.toString());
        source.setProperty(mutableIdentityConfiguration.INSTANCE_ID_KEY, this.instanceId);
        source.setProperty(mutableIdentityConfiguration.RESTCOMM_CLIENT_SECRET_KEY, this.clientSecret);
        report("Configuration updated.");
    }

    boolean migrateAccount(Account account) {
        if (StringUtils.isEmpty(account.getEmailAddress()))
            report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + ") FAILED: account has no email address and won't be migrated.");
        else {
            String password = account.getAuthToken(); // use auth_token as password for the user. Other options?
            UserEntity user = new UserEntity(account.getEmailAddress(), account.getEmailAddress(), account.getFriendlyName(), null, password );
            Outcome outcome = identityApi.createUser(user);
            if ( outcome == Outcome.CONFLICT ) {
                // the user is already there. Check the policy and act accordingly
                if (this.inviteExistingUsers) {
                    if ( ! identityApi.inviteUser(account.getEmailAddress()) )
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") FAILED: invitation to existing user failed.");
                    else {
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") OK (invited)");
                        return true;
                    }
                } else
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") FAILED: user already exists and policy does not allow invitations");
            } else
            if (outcome == Outcome.OK) {
                if (!identityApi.inviteUser(account.getEmailAddress()))
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") failed: new user was created but invitation failed" );
                else {
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") OK (created)");
                    return true;
                }
            } else
                report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") FAILED: could not create user");
        }
        return false;
    }

    private void report(String message) {
        logger.info(message);
    }

    private String generateRandomInstanceSecret() {
        return UUID.randomUUID().toString();
    }

}

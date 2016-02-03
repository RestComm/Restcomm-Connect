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

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityMigrationException;
import org.mobicents.servlet.restcomm.identity.keycloak.IdentityContext;
import org.mobicents.servlet.restcomm.util.UriUtils;


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


    public static void performMigration(MigrationContext migrationContext) throws IdentityMigrationException {
        RestcommIdentityApi api = new RestcommIdentityApi(migrationContext.getIdentityConfig().getAuthServerBaseUrl(), migrationContext.getIdentityConfig().getUsername(), migrationContext.getIdentityConfig().getPassword(), migrationContext.getIdentityConfig().getRealm(), null);
        // if no RedirectUris have been defined in configuration, use the values form the container connectors
        String [] uris = IdentityMigrationTool.determineRedirectUris(migrationContext.getIdentityConfig());
        if (uris == null)
            throw new IdentityMigrationException("Could not determine redirect uris");
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(migrationContext.getAccountsDao(), api, migrationContext.getIdentityConfig().getInviteExistingUsers(), migrationContext.getIdentityConfig().getAdminAccountSid(), migrationContext.getMutableIdentityConfig(), uris);
        migrationTool.migrate();
        migrationContext.getRestcommConfiguration().reloadMutableIdentity();
        // Reset identity context after migration
        IdentityContext oldContext = (IdentityContext) migrationContext.getServletContext().getAttribute(IdentityContext.class.getName());
        IdentityContext identityContext = new IdentityContext(migrationContext.getRestcommConfiguration().getIdentity(),migrationContext.getRestcommConfiguration().getMutableIdentity(), oldContext.getRestcommRoles());
        migrationContext.getServletContext().setAttribute(IdentityContext.class.getName(), identityContext);
    }

    /**
     * Determines whether migration will be done or not. Relies on several things as headless flag, startup|ui migration method, whether
     * migration has already been done (identity mode) and whether restcomm is on bootstrap mode or not.
     *
     * @param migrationContext
     * @return
     */
    public static boolean shouldMigrate(MigrationContext migrationContext) {
        if (migrationContext.isBootstrapping()) {
            if ( ! migrationContext.getIdentityConfig().getHeadless() ) {
                if ( migrationContext.getIdentityConfig().getMethod().equals(IdentityConfigurationSet.MigrationMethod.startup) && !"cloud".equals(migrationContext.getMutableIdentityConfig().getMode())) {
                    return true;
                }
            }
        } else {
            if ( ! migrationContext.getIdentityConfig().getHeadless() ) {
                if ( migrationContext.getIdentityConfig().getMethod().equals(IdentityConfigurationSet.MigrationMethod.ui) && !"cloud".equals(migrationContext.getMutableIdentityConfig().getMode())) {
                    return true;
                }
            }
        }
        return false;
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
            UserEntity user = new UserEntity(account.getEmailAddress(), account.getFriendlyName(), null, password );
            Outcome outcome = identityApi.createUser(user);
            if ( outcome == Outcome.CONFLICT ) {
                // the user is already there. Check the policy and act accordingly
                if (this.inviteExistingUsers) {
                    if ( ! (identityApi.inviteUser(account.getEmailAddress()) == Outcome.OK))
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") FAILED: invitation to existing user failed.");
                    else {
                        report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") OK (invited)");
                        return true;
                    }
                } else
                    report("Migrating account " + account.getSid().toString() + " (" + account.getFriendlyName() + " - " + account.getEmailAddress() + ") FAILED: user already exists and policy does not allow invitations");
            } else
            if (outcome == Outcome.OK) {
                if (! (identityApi.inviteUser(account.getEmailAddress()) == Outcome.OK))
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
        //System.out.println( message );
        logger.info(message);
    }

    private String generateRandomInstanceSecret() {
        return UUID.randomUUID().toString();
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Tries to determine a valid set of redirect uris using the container and configuration.
     *
     * @param identityConfig
     * @return null if redirect uri was found
     */
    private static String[] determineRedirectUris(IdentityConfigurationSet identityConfig) {
        String [] uris = identityConfig.getRedirectUris();
        if (uris == null) {
            URI uri;
            try {
                uri = UriUtils.resolve(new URI("/"));
            } catch (Exception e) {
                logger.error("Could not determine identity RedirectUris.", e);
                return null;
            }
            String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : (":" + uri.getPort()));
            uris = new String[] {base};
        }
        return uris;
    }
}

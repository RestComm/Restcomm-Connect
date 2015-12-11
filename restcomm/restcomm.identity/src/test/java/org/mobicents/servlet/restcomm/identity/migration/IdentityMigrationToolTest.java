package org.mobicents.servlet.restcomm.identity.migration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;

import static org.junit.Assert.*;

public class IdentityMigrationToolTest {

    private static String authServerBaseUrl = "http://192.168.1.40:8080";
    private static String username = "administrator@company.com";
    private static String password = "RestComm";
    private static String realm = "restcomm";

    static RestcommIdentityApi api;

    public IdentityMigrationToolTest() {
        // TODO Auto-generated constructor stub
    }

    @BeforeClass
    public static void setup() throws RestcommIdentityApiException {
        // create api
        api = new RestcommIdentityApi(authServerBaseUrl, username, password, realm, null);
        String instanceId = api.createInstance(new String[] {"http://localhost"}, "my-secret").instanceId;
        api.bindInstance(instanceId);

    }

    @Test
    public void migrateSingleAccount() {
        // test exception if the email is missing
        MockAccountsDao dao = new MockAccountsDao();
        Account account = dao.buildTestAccount(null,  "account1@company.com", "account1", "auth_token1", null);
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false);

        assertTrue(migrationTool.migrateAccount(account));
        assertNotNull( api.retrieveTokenString("account1@company.com", "auth_token1") );

        api.dropUser("account1@company.com");
    }

    @Test
    public void createWithoutEmailFails() {
        // test exception if the email is missing
        MockAccountsDao dao = new MockAccountsDao();
        Account accountWithoutEmail = dao.buildTestAccount(null,  null, "Missing Email Account", null, null);
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false);

        assertFalse(migrationTool.migrateAccount(accountWithoutEmail));
    }

    /**
     * Test 'inviteExistingUsers' flag
     */
    @Test
    public void testMigrationByIviteExistingUserPolicy() {
        MockAccountsDao dao = new MockAccountsDao();
        // create migration tool with 'inviteExisting' == false
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false);
        // create user
        UserEntity userEntity = new UserEntity("existing@company.com","existing@company.com","existing user",null,"password");
        api.createUser(userEntity);
        // try to migrate over him
        Account existingAccount = dao.buildTestAccount(null,  "existing@company.com", "existing user", "password", null);
        assertFalse("Existing user shouldn't be migrated as the 'inviteExistingUsers' policy is false.", migrationTool.migrateAccount(existingAccount));
        // create migration tool with 'inviteExisting' == true
        migrationTool = new IdentityMigrationTool(dao, api, true);
        assertTrue("Existing user shouldn't be migrated as the 'inviteExistingUsers' policy is false.", migrationTool.migrateAccount(existingAccount));
        // remove user
        api.dropUser("existing@company.com");
    }

    @Test
    public void existingAdminAccountLinkingWorks() {
        MockAccountsDao dao = new MockAccountsDao();
        Sid sid = Sid.generate(Sid.Type.ACCOUNT);
        // create an admin account with no email to test linking
        dao.addAccount(dao.buildTestAccount(sid, null, "account1", "auth_token1", null));
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false, sid.toString(), null, null );

        migrationTool.linkAdministratorAccount();
        Account updatedAccount = dao.getAccount(sid.toString());
        assertEquals("Admin account linking failed. Email address property not properly updated.", this.username, updatedAccount.getEmailAddress());
    }

    @Test
    public void missingAdminAccountLinkingFails() {
        MockAccountsDao dao = new MockAccountsDao();
        Sid sid = Sid.generate(Sid.Type.ACCOUNT);
        // create an admin account with no email to test linking
        dao.addAccount(dao.buildTestAccount(sid, null, "account1", "auth_token1", null));
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false, "missing-SID", null, null );

        assertFalse("Admin account linking should have failed since the account does not exist.", migrationTool.linkAdministratorAccount());
        //Account updatedAccount = dao.getAccount(sid.toString());
        //assertEquals("Admin account linking failed. Email address property not properly updated.", this.username, updatedAccount.getEmailAddress());

    }

    @Test
    public void testMigrate() {
        MockAccountsDao dao = new MockAccountsDao();
        Sid sid = Sid.generate(Sid.Type.ACCOUNT);
        dao.addAccount(dao.buildTestAccount(sid, null, "account1", "auth_token1", null));
        MutableIdentityConfigurationSet mutableIdentityConfig = new MockMutableIdentityConfigurationSet("init", null, null, true);
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(dao, api, false, sid.toString(), mutableIdentityConfig, new String [] {"http://localhost:8080"} );
        migrationTool.migrate();

        api.dropInstance(mutableIdentityConfig.getInstanceId());
        for (Account account : dao.getAccounts()) {
            if ( ! sid.toString().equals(account.getSid().toString()) ) {
                api.dropUser(account.getEmailAddress());
            }
        }
    }

    @AfterClass
    public static void shutdown() {
        assertEquals("Error removing instance " + api.getBoundInstanceId(), Outcome.OK, api.dropInstance(api.getBoundInstanceId()));
    }

}

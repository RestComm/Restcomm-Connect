package org.mobicents.servlet.restcomm.identity.migration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;

import static org.junit.Assert.*;

public class IdentityMigrationToolTest {

    private static String authServerBaseUrl = "http://192.168.1.3:8080"; //"https://identity.restcomm.com";
    private static String username = "otsakir@gmail.com"; //"test_user";
    private static String password = "password";
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

    @AfterClass
    public static void shutdown() {
        assertEquals("Error removing instance " + api.getBoundInstanceId(), Outcome.OK, api.dropInstance(api.getBoundInstanceId()));
    }

}

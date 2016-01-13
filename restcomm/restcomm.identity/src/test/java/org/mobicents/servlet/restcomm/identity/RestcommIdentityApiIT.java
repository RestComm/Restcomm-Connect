package org.mobicents.servlet.restcomm.identity;

import static org.junit.Assert.*;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;

import java.io.IOException;
import java.io.InputStream;

public class RestcommIdentityApiIT {

    static String authServerBaseUrl = IdentityTestTool.AUTH_SERVER_BASE_URL;
    static String username = "administrator@company.com";
    static String password = "RestComm";
    static String realm = "restcomm";

    static RestcommIdentityApi api;
    static String instanceId;

    private static IdentityTestTool tool;

    public RestcommIdentityApiIT() {}

    @BeforeClass
    public static void createInstance() throws RestcommIdentityApiException, IOException {
        tool = new IdentityTestTool();
        tool.importRealm("simple-identity-instance-realm.json");
        api = new RestcommIdentityApi(authServerBaseUrl, username, password, realm, null);
        instanceId = api.createInstance(new String[] {"http://localhost"}, "my-secret").instanceId;
        Assert.assertNotNull("Error creating identity instance", instanceId);
        api.bindInstance(instanceId);
        Assert.assertEquals(instanceId, api.getBoundInstanceId());
    }

    @Test void retrieveToken() {
        RestcommIdentityApi api = new RestcommIdentityApi(authServerBaseUrl, username, password, realm, null);
        Assert.assertNotNull("Error retrieving token for user '" + username +"'", api.getTokenString());
    }

    /*
    @Test
    public void userIsCreated() {
        UserEntity user = new UserEntity("user1@gmail.com", "user1@gmail.com","first1", "last1", "pass1" );
        Assert.assertEquals(api.createUser(user));
    }
*/
    @Test
    public void testCreateAndInviteUser() {
        UserEntity user = new UserEntity("test_invited_user",null, "Test Invited User", null, "invited_password");
        assertEquals("Error creating user", Outcome.OK, api.createUser(user));
        assertTrue("Error inviting user",api.inviteUser("test_invited_user"));
        assertEquals("Error removing user",Outcome.OK, api.dropUser("test_invited_user"));
    }

    @AfterClass
    public static void removeInstance() throws RestcommIdentityApiException {
        tool.dropRealm(realm);
    }


}

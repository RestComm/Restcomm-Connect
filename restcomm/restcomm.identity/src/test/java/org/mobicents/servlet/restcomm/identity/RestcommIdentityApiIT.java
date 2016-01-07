package org.mobicents.servlet.restcomm.identity;

import static org.junit.Assert.*;

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
    static String username = "administrator@company.com";  //"test_user";
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
        //api = new RestcommIdentityApi("http://192.168.1.40:8080/auth", "administrator@company.com", "RestComm", realm, null);
       instanceId = api.createInstance(new String[] {"http://localhost"}, "my-secret").instanceId;
       api.bindInstance(instanceId);
    }

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
        /*if (instanceId != null) {
            Outcome outcome = api.dropInstance(instanceId);
            assertEquals("Error removing instance", Outcome.OK, outcome);
        }*/
    }


}

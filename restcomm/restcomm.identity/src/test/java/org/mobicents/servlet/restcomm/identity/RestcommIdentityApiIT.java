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

    private IdentityTestTool tool;

    public RestcommIdentityApiIT() {
        tool = new IdentityTestTool();
    }

    @Test
    public void fooTest() throws IOException {
        //InputStream inputStream = getClass().getResourceAsStream("default-realm.json");
        //String data = IOUtils.toString(inputStream,"UTF-8");
        //System.out.println("data: " + data);
        tool.importRealm("default-realm.json");
        tool.dropRealm("restcomm-test");
    }

    /*
    @BeforeClass
    public static void createInstance() throws RestcommIdentityApiException {
        api = new RestcommIdentityApi(authServerBaseUrl, username, password, realm, null);
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
        if (instanceId != null) {
            Outcome outcome = api.dropInstance(instanceId);
            assertEquals("Error removing instance", Outcome.OK, outcome);
        }
    }
    */


}

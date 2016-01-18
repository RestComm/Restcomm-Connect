/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.identity;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.UserEntity;

import java.io.IOException;

/**
 * @author Orestis Tsakiridis
 */
public class RestcommIdentityApiTest {

    static String authServerBaseUrl = IdentityTestTool.AUTH_SERVER_BASE_URL;
    static String username = "administrator@company.com";
    static String password = "RestComm";
    static String realm = "restcomm";

    static RestcommIdentityApi api;
    static String instanceId;

    private static IdentityTestTool tool;

    public RestcommIdentityApiTest() {}

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

    @Test
    public void retrieveToken() {
        RestcommIdentityApi api = new RestcommIdentityApi(authServerBaseUrl, username, password, realm, null);
        Assert.assertNotNull("Error retrieving token for user '" + username +"'", api.getTokenString());
    }

    @Test
    public void userManagementTasksWork() {
        UserEntity user = new UserEntity("user1@company.com","first1", "last1", "pass1" );
        // test user creation
        Assert.assertEquals("User creation failed", Outcome.OK, api.createUser(user));
        // also check keycloak directly
        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/auth/admin/realms/" + realm + "/users");
        String response = webResource.queryParam("username","user1@company.com").header("Authorization", "Bearer " + tool.getToken()).get(String.class); // use 'master' realm admin account for this request
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        Assert.assertEquals("Exactly one (1) user expected",1,jsonResponse.size());
        JsonObject jsonUser = jsonResponse.get(0).getAsJsonObject();
        String userId = jsonUser.get("id").getAsString(); // will use it in a while
        Assert.assertEquals("Username stored wrong", "user1@company.com",jsonUser.get("username").getAsString());
        Assert.assertEquals("User firstname stored wrong", "first1",jsonUser.get("firstName").getAsString());
        Assert.assertEquals("User lastname stored wrong","last1",jsonUser.get("lastName").getAsString());
        // also check that the user can authenticate (simply creating the api2 object also retrieves a token

        // test user invitation
        RestcommIdentityApi api2 = new RestcommIdentityApi(authServerBaseUrl, "user1@company.com", "pass1", realm, api.getBoundInstanceId());
        Assert.assertEquals("User invitation to identity instance failed", Outcome.OK, api.inviteUser("user1@company.com") );
        // now we need to check the exact roles for the user
        webResource = jerseyClient.resource(authServerBaseUrl + "/auth/admin/realms/" + realm + "/users/" + userId + "/role-mappings" );
        response = webResource.header("Authorization", "Bearer " + tool.getToken()).get(String.class);
        JsonElement mappingsElement = parser.parse(response);
        Assert.assertTrue("Developer role was not assigned", tool.roleMappingsContainRole(mappingsElement,"Developer",api.getBoundInstanceId(),"client"));

        //test user removal
        api.dropUser("user1@company.com");
        webResource = jerseyClient.resource(authServerBaseUrl + "/auth/admin/realms/" + realm + "/users");
        response = webResource.queryParam("username","user1@company.com").header("Authorization", "Bearer " + tool.getToken()).get(String.class);
        Assert.assertEquals("User was not removed", "[]",response);
    }


    @AfterClass
    public static void removeInstance() throws RestcommIdentityApiException {
        api.dropInstance(api.getBoundInstanceId());
        tool.dropRealm(realm);
    }
}

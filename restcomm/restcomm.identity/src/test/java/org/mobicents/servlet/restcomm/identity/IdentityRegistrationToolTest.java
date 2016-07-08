/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import com.google.gson.JsonObject;
import junit.framework.Assert;
import org.junit.*;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
//import org.mobicents.servlet.restcomm.identity.entities.ClientEntity;
import org.mobicents.servlet.restcomm.identity.entities.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityClientRegistrationError;

import java.io.IOException;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class IdentityRegistrationToolTest {
    IdentityRegistrationTool tool;
    // these depend on your keycloak installation
    static String KEYCLOAK_URL = "https://identity.restcomm.com:8443/auth";
    static String ADMIN_USERNAME = "admin";
    static String ADMIN_PASSWORD = "43stc0mm";
    // this does not depend on it
    static String testRealm = "restcomm-test";

    static IdentityTestingTool testingTool;
    static String iat;

    @BeforeClass
    public static void beforeClass() throws IOException {
        testingTool = new IdentityTestingTool(KEYCLOAK_URL, ADMIN_USERNAME, ADMIN_PASSWORD);
        testingTool.importRealm("restcomm-test-realm.json");
        String powerfulToken = testingTool.getTokenWithClientGrant("service-client","client-secret",testRealm);
        iat = testingTool.getIAT(powerfulToken,0,1000,testRealm);
    }

    @AfterClass
    public static void afterClass() {
        testingTool.dropRealm(testRealm);
    }

    @Test
    public void identityRegistrationSucceeds() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool(KEYCLOAK_URL,testRealm);
        IdentityInstance ii = tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080","client-secret1");
        String token = testingTool.getTokenWithDirectAccessGrant("administrator@company.com", "RestComm", "service-client", "client-secret", "restcomm-test");
        // check *-restcomm-ui Client
        JsonObject restcommClient = testingTool.getClient(token, ii.getName() + "-restcomm", testRealm);
        Assert.assertEquals("http://restcomm-server:8080", restcommClient.get("baseUrl").getAsString());
        Assert.assertEquals("http://restcomm-server:8080/*", restcommClient.get("redirectUris").getAsString());
        Assert.assertEquals("http://restcomm-server:8080", restcommClient.get("webOrigins").getAsString());
        // check trailing '/' character is removed from root url

    }

    @Test
    public void trailingSlashIsRemoved() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool(KEYCLOAK_URL,testRealm);
        IdentityInstance ii = tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080/","client-secret1");
        String token = testingTool.getTokenWithDirectAccessGrant("administrator@company.com", "RestComm", "service-client", "client-secret", "restcomm-test");
        // check *-restcomm-ui Client. No need to test the rest since trimming is done on a higher level
        JsonObject restcommUI = testingTool.getClient(token, ii.getName() + "-restcomm", testRealm);
        Assert.assertEquals("http://restcomm-server:8080", restcommUI.get("baseUrl").getAsString());
        Assert.assertEquals("http://restcomm-server:8080/*", restcommUI.get("redirectUris").getAsString());
        Assert.assertEquals("http://restcomm-server:8080", restcommUI.get("webOrigins").getAsString());
    }

    @Test(expected=AuthServerAuthorizationError.class)
    public void badIATShouldFail() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool(KEYCLOAK_URL,testRealm);
        tool.registerInstanceWithIAT("bad_IAT", "http://restcomm-server:8080","client-secret1");
    }

    @Test(expected=IdentityClientRegistrationError.class)
    public void badKeycloakAddressShouldFail() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool("badurl",testRealm);
        tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080","client-secret1");
    }

    @Test
    public void registeredClientCanBeUpdated() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        // first create a new identity instance
        // TODO, do that by importing a small realm instead of creating the instance each time
        tool = new IdentityRegistrationTool(KEYCLOAK_URL,testRealm);
        IdentityInstance ii = tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080","client-secret1");
        String oldRAT = ii.getRestcommRAT();
        KeycloakClient client = new KeycloakClient();
        client.setBearerOnly(true);
        KeycloakClient updatedClient = tool.updateRegisteredClientWithRAT(client,ii, IdentityRegistrationTool.RESTCOMM_CLIENT_SUFFIX);
        Assert.assertTrue("Updated keycloak Client property was not really updated: BearerOnly", updatedClient.getBearerOnly().booleanValue());
        Assert.assertFalse("Registration access token has not changed but should have.", oldRAT.equals(updatedClient.getRegistrationAccessToken()));
        Assert.assertEquals("Updated client RAT was not properly updated inside Identity Instance", ii.getRestcommRAT(), updatedClient.getRegistrationAccessToken());
        // make sure that the new RAT is still valid for another round of updates
        KeycloakClient client2 = new KeycloakClient();
        client2.setBaseUrl("/");
        KeycloakClient updatedClient2 = tool.updateRegisteredClientWithRAT(client2, ii, IdentityRegistrationTool.RESTCOMM_CLIENT_SUFFIX);
        Assert.assertEquals("/", updatedClient2.getBaseUrl());

    }

//    @Test
//    public void testClientCreationAndRemoval() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
//        // create client
//        ClientEntity clientEntity = tool.registerClient("TEST-restcomm-rest", iat, "http://192.168.1.39:8080","topsecret",null, null );
//        Assert.assertNotNull(clientEntity);
//        // drop client
//        Assert.assertNotNull(clientEntity.getRegistrationAccessToken());
//        tool.unregisterClient("TEST-restcomm-rest",clientEntity.getRegistrationAccessToken());
//    }
//
//    @Test
//    public void testIdentityInstanceCreationAndRemoval() throws AuthServerAuthorizationError {
//        IdentityInstance identityInstance = tool.registerInstanceWithIAT(iat, null, "topsecret");
//        Assert.assertNotNull(identityInstance);
//        tool.unregisterInstanceWithRAT(identityInstance);
//    }
}

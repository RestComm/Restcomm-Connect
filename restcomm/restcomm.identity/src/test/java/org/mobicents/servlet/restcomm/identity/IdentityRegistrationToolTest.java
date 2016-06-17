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
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityClientRegistrationError;

import java.io.IOException;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class IdentityRegistrationToolTest {
    IdentityRegistrationTool tool;

    //static String IAT = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiIwOWRjYTNiMi04ZGZjLTRlM2EtYTIwNi02NDg5N2QyNTAyMWUiLCJleHAiOjAsIm5iZiI6MCwiaWF0IjoxNDU4NjUzNTkzLCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODAvYXV0aC9yZWFsbXMvcmVzdGNvbW0tdGVzdCIsImF1ZCI6Imh0dHA6Ly8xMjcuMC4wLjE6ODA4MC9hdXRoL3JlYWxtcy9yZXN0Y29tbS10ZXN0IiwidHlwIjoiSW5pdGlhbEFjY2Vzc1Rva2VuIn0.VehnxN_OC-JiFgUQEghIyoFwaWvP1EOVzof6sCTR52baCtsuCjS6omO_FVs3f9vzclX85Go7vf19F1mBBAXyHy3-fzJgJZ6wdk5AAAWbnmt0TkNM60irXa3sKvqMZnDae0L9dPRNoYEhe6_s6AUVZ25basb7uxLSDntNC4a-v0xI1gug_m-TKsjj967eIqJyUHknCbjKTjBewyFyCnSbWkQzCTojmCQ4QiYHGoLYpWYxpUlLI-iv-eOnlwQwcbDA9AbEf7vrk3FaykhnVisP-FmMhYUUYn11XUqkbIkFosE3mZKDMlvPW-j7r50bR4bIzVhXubw1IQBqoqB9AG0Dnw";
    static String keycloakUrl = "http://127.0.0.1:8081/auth";
    static String testRealm = "restcomm-test";

    static IdentityTestingTool testingTool;
    static String iat;

    @BeforeClass
    public static void beforeClass() throws IOException {
        testingTool = new IdentityTestingTool(keycloakUrl, "admin", "admin");
        testingTool.importRealm("restcomm-test-realm.json");
        String powerfulToken = testingTool.getTokenWithClientGrant("service-client","client-secret",testRealm);
        iat = testingTool.getIAT(powerfulToken,10,1000,testRealm);
    }

    @AfterClass
    public static void afterClass() {
        testingTool.dropRealm(testRealm);
    }

    @Test
    public void identityRegistrationSucceeds() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool(keycloakUrl,testRealm);
        IdentityInstance ii = tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080","client-secret1");
        String token = testingTool.getTokenWithDirectAccessGrant("administrator@company.com", "RestComm", "service-client", "client-secret", "restcomm-test");
        JsonObject restcommUI = testingTool.getClient(token, ii.getName() + "-restcomm-ui", testRealm);
        Assert.assertEquals("http://restcomm-server:8080", restcommUI.get("baseUrl").getAsString());
        Assert.assertEquals("http://restcomm-server:8080/*", restcommUI.get("redirectUris").getAsString());
        Assert.assertEquals("http://restcomm-server:8080", restcommUI.get("webOrigins").getAsString());
    }

    @Test(expected=AuthServerAuthorizationError.class)
    public void badIATShouldFail() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool(keycloakUrl,testRealm);
        tool.registerInstanceWithIAT("bad_IAT", "http://restcomm-server:8080","client-secret1");
    }

    @Test(expected=IdentityClientRegistrationError.class)
    public void badKeycloakAddressShouldFail() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        tool = new IdentityRegistrationTool("badurl",testRealm);
        tool.registerInstanceWithIAT(iat, "http://restcomm-server:8080","client-secret1");
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

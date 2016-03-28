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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.identity.entities.ClientEntity;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityClientRegistrationError;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityRegistrationToolTest {
    IdentityRegistrationTool tool;
    String iat;

    static String IAT = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiIwOWRjYTNiMi04ZGZjLTRlM2EtYTIwNi02NDg5N2QyNTAyMWUiLCJleHAiOjAsIm5iZiI6MCwiaWF0IjoxNDU4NjUzNTkzLCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODAvYXV0aC9yZWFsbXMvcmVzdGNvbW0tdGVzdCIsImF1ZCI6Imh0dHA6Ly8xMjcuMC4wLjE6ODA4MC9hdXRoL3JlYWxtcy9yZXN0Y29tbS10ZXN0IiwidHlwIjoiSW5pdGlhbEFjY2Vzc1Rva2VuIn0.VehnxN_OC-JiFgUQEghIyoFwaWvP1EOVzof6sCTR52baCtsuCjS6omO_FVs3f9vzclX85Go7vf19F1mBBAXyHy3-fzJgJZ6wdk5AAAWbnmt0TkNM60irXa3sKvqMZnDae0L9dPRNoYEhe6_s6AUVZ25basb7uxLSDntNC4a-v0xI1gug_m-TKsjj967eIqJyUHknCbjKTjBewyFyCnSbWkQzCTojmCQ4QiYHGoLYpWYxpUlLI-iv-eOnlwQwcbDA9AbEf7vrk3FaykhnVisP-FmMhYUUYn11XUqkbIkFosE3mZKDMlvPW-j7r50bR4bIzVhXubw1IQBqoqB9AG0Dnw";
    static String keycloakUrl = "http://127.0.0.1:8080/auth";
    static String realm = "restcomm-test";

    @Before
    public void init() {
        tool = new IdentityRegistrationTool(keycloakUrl,realm);
        iat = IAT;
    }

    @Test
    public void testClientCreationAndRemoval() throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        // create client
        ClientEntity clientEntity = tool.registerClient("TEST-restcomm-rest", iat, new String[] {"http://192.168.1.39:8080"},"topsecret",null, null );
        Assert.assertNotNull(clientEntity);
        // drop client
        Assert.assertNotNull(clientEntity.getRegistrationAccessToken());
        tool.unregisterClient("TEST-restcomm-rest",clientEntity.getRegistrationAccessToken());
    }

    @Test
    public void testIdentityInstanceCreationAndRemoval() throws AuthServerAuthorizationError {
        IdentityInstance identityInstance = tool.registerInstanceWithIAT(iat, null, "topsecret");
        Assert.assertNotNull(identityInstance);
        tool.unregisterInstanceWithRAT(identityInstance);
    }
}

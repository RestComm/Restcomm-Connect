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
import org.mobicents.servlet.restcomm.identity.exceptions.InitialAccessTokenExpired;

import java.util.UUID;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityRegistrationToolTest {
    IdentityRegistrationTool tool;
    String iat;

    static String IAT = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiJkZmJiMjNmZi01ZDk2LTRjYTYtOTYxMC0zYzI1NWNiZDVjODYiLCJleHAiOjAsIm5iZiI6MCwiaWF0IjoxNDU4MzEwOTgxLCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODAvYXV0aC9yZWFsbXMvcmVzdGNvbW0iLCJhdWQiOiJodHRwOi8vMTI3LjAuMC4xOjgwODAvYXV0aC9yZWFsbXMvcmVzdGNvbW0iLCJ0eXAiOiJJbml0aWFsQWNjZXNzVG9rZW4ifQ.bJapQkh0ZPBcm4avgeqz6OpPu1EeDXuobNUxBNlToBqnHO1CWNGZN3_WYbn3SoagH50umvbC8WL9bLpMXPeLPTsk75JbTmZGtjRj3IMb6DgQfRLb2sDoX40RMJfUuDZVHqG6dmKwut8SAMDWCVJnfeTucCYrHbV13Lns36TqOqHHmrJ8z6VWcDd05k2cLY29JbiIz7rUIoWotIwr6rtgYvKbUdc2yzXdziqkJ2WMRDGhr0Nl2tOCtU7Ea51MpskYZmq5q7a9k8okXIfscsxpdV_dfmI4pm8bCctiKG7zrJ4ouw_1o8ma-sCDEw0n2cveOFhY14qgkE60OqaUPek6dQ";

    @Before
    public void init() {
        tool = new IdentityRegistrationTool("http://127.0.0.1:8080/auth");
        iat = IAT;
    }

    @Test
    public void testClientCreationAndRemoval() throws InitialAccessTokenExpired {
        // create client
        ClientEntity clientEntity = tool.registerClient("TEST-restcomm-rest", iat, new String[] {"http://192.168.1.39:8080"},"topsecret",null, null );
        Assert.assertNotNull(clientEntity);
        // drop client
        Assert.assertNotNull(clientEntity.getRegistrationAccessToken());
        tool.unregisterClient("TEST-restcomm-rest",clientEntity.getRegistrationAccessToken());
    }

    @Test
    public void testIdentityInstanceCreationAndRemoval() throws InitialAccessTokenExpired {
        IdentityInstance identityInstance = tool.registerInstanceWithIAT(iat, null, "topsecret");
        Assert.assertNotNull(identityInstance);
        tool.unregisterInstanceWithRAT(identityInstance);
    }
}

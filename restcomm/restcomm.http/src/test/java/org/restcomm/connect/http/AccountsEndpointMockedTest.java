/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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

package org.restcomm.connect.http;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.AuthToken;
import org.restcomm.connect.http.exceptions.NotAuthenticated;

import junit.framework.Assert;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A sample unit test for accounts endpoint. It illustrates the use of the EndpointMockedTest.
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class AccountsEndpointMockedTest extends EndpointMockedTest {
    @Test
    public void endpointInitializedAndBasicAuthorizationWork() throws ConfigurationException {
        init(); // setup default mocking values
        AccountsEndpoint endpoint = new AccountsEndpoint(servletContext,request);
        endpoint.init();
    }

    @Test(expected=NotAuthenticated.class)
    public void requestMissingAuthorizationIsRejected()  {
        init(); // setup default mocking values
        when(request.getHeader("Authorization")).thenReturn(null); // override Authorization header to null
        AccountsEndpoint endpoint = new AccountsEndpoint(servletContext,request);
        endpoint.init();
     }
}
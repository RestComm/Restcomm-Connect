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

package org.mobicents.servlet.restcomm.http;

import junit.framework.Assert;
import org.junit.Test;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;

import javax.ws.rs.core.Response;
import java.net.MalformedURLException;

import static org.mockito.Mockito.*;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class OrgIdentityEndpointMockedTest extends EndpointMockedTest {

    @Test
    public void testOrgIdentityCreationAndConflict() throws MalformedURLException {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        organizations.clear();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        organizations.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Orestis organization","orestis",null,null));

        OrgIdentityEndpoint endpoint = new OrgIdentityEndpoint(servletContext,request);
        endpoint.init();
        // if the organization does not exist should get a 400 back and not store any orgIdentity
        Response response = endpoint.createOrgIdentity("newOrgIdentity", "non-existent-org");
        Assert.assertEquals(400, response.getStatus());
        Assert.assertNull(orgIdentityDao.getOrgIdentityByName("newOrgIdentity"));
        // test OrgIdentity creation with explicit organization namespace use
        response = endpoint.createOrgIdentity("random123", "orestis");
        Assert.assertEquals(200, response.getStatus());
        OrgIdentity created = orgIdentityDao.getOrgIdentityByName("random123");
        Assert.assertNotNull(created);
        Assert.assertEquals("OR11111111111111111111111111111111",created.getOrganizationSid().toString());
        // test update really changed orgIdentity name
        endpoint.updateOrgIdentity(created.getSid().toString(),"random321");
        Assert.assertEquals("random321",orgIdentityDao.getOrgIdentity(created.getSid()).getName());
        // if no organization defined as query param, use domain name to determine it
        response = endpoint.createOrgIdentity("newOrgIdentity", null);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(orgIdentityDao.getOrgIdentityByName("newOrgIdentity"));
        // Try to create again. should get a conflict.
        response = endpoint.createOrgIdentity("newOrgIdentity", null);
        Assert.assertEquals(409, response.getStatus());
    }

    @Test
    public void updateOrgIdentity() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        organizations.clear();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Foo organization","fooorg",null,null));
        orgIdentities.clear();
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://127.0.0.1:8080/"));
        OrgIdentityEndpoint endpoint = new OrgIdentityEndpoint(servletContext,request);
        endpoint.init();
        // check OrgIdentity name is really updated
        Response response = endpoint.updateOrgIdentity("OI00000000000000000000000000000000","updated");
        Assert.assertEquals("updated", orgIdentityDao.getOrgIdentity(new Sid("OI00000000000000000000000000000000")).getName());
        Assert.assertEquals(200, response.getStatus());
        // if OrgIdentity does not exist should get a 404
        response = endpoint.updateOrgIdentity("OI00000000000000000000000000000001","foo");
        Assert.assertEquals(404,response.getStatus());
        //if 'name' parameter is missing should get a 400
        response = endpoint.updateOrgIdentity("OI00000000000000000000000000000001",null);
        Assert.assertEquals(400,response.getStatus());
    }

    @Test
    public void orgIdentityIsRemoved() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Foo organization","fooorg",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        when(request.getRequestURL()).thenReturn(new StringBuffer("http://127.0.0.1:8080/"));
        OrgIdentityEndpoint endpoint = new OrgIdentityEndpoint(servletContext,request);
        endpoint.init();

        endpoint.removeOrgIdentity("OI00000000000000000000000000000000");
        Assert.assertNull(orgIdentityDao.getOrgIdentity(new Sid("OI00000000000000000000000000000000")));
    }

}

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
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDaoMock;
import org.mobicents.servlet.restcomm.dao.mocks.OrganizationsDaoMock;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class SecuredEndpointMockedTest extends EndpointMockedTest {

    List<Organization> orgs;
    OrganizationsDaoMock organizationDao;
    List<OrgIdentity> orgIdentities;
    OrgIdentityDaoMock orgIdentitiesDao;

    public void before() {
        init();
        orgs = new ArrayList<Organization>();
        organizationDao = new OrganizationsDaoMock(orgs);
        daoManager.setOrganizationsDao(organizationDao);
        orgIdentities = new ArrayList<OrgIdentity>();
        orgIdentitiesDao = new OrgIdentityDaoMock(orgIdentities);
        daoManager.setOrgIdentityDao(orgIdentitiesDao);
        // mock IdentityContext
        IdentityContext identityContext = new IdentityContext(conf, restcommConfiguration.getMain(),orgIdentitiesDao);
        when(servletContext.getAttribute(IdentityContext.class.getName())).thenReturn(identityContext);
    }

    @Test
    public void TestOrganizationResolving() {
        setRestcommXmlResourcePath("/restcomm.xml");
        before();

        orgs.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        orgs.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));

        // assert Default organization is returned for localhost and 127.0.0.1 namespaces
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        Assert.assertTrue( "Default organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationDao).getNamespace().equals("default") );
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://127.0.0.1/restcomm"));
        Assert.assertTrue( "Default organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationDao).getNamespace().equals("default") );
        // assert existing organization is resolved
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud.com/restcomm"));
        Assert.assertTrue( "telestax organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationDao).getNamespace().equals("telestax") );
        // assert non-existing organization is not-resolved
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://fake.mycloud.com/restcomm"));
        Assert.assertNull( "null organization should be returned when a namespace has no respective organization", SecuredEndpoint.findCurrentOrganization(request,organizationDao) );
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://crap"));
        Assert.assertNull( "null organization should be returned when a namespace has no respective organization", SecuredEndpoint.findCurrentOrganization(request,organizationDao) );
    }

    @Test
    public void TestOrgIdentityResolving() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        before();
        orgs.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        orgs.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI11111111111111111111111111111111"),new Sid("OR11111111111111111111111111111111"),"telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        // telestax orgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud:8080/restcomm"));
        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertEquals( "OI11111111111111111111111111111111", endpoint.findCurrentOrgIdentity(request,orgIdentitiesDao).getSid().toString() );
        // random orgIdentity is mapped to default organization and should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertEquals( "random123", endpoint.findCurrentOrgIdentity(request,orgIdentitiesDao).getName() );
    }

    @Test
    public void TestOrgIdentityResolvingWithKeycloakDisabled() {
        setRestcommXmlResourcePath("/restcomm.xml"); // no <identity/> configuration in restcomm.xml
        before();
        orgs.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        orgs.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI11111111111111111111111111111111"),new Sid("OR11111111111111111111111111111111"),"telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud:8080/restcomm"));
        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull(endpoint.findCurrentOrgIdentity(request,orgIdentitiesDao));
        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull( endpoint.findCurrentOrgIdentity(request,orgIdentitiesDao) );
        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://crap:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull( endpoint.findCurrentOrgIdentity(request,orgIdentitiesDao) );
    }
}

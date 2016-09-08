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
import org.mobicents.servlet.restcomm.dao.OrgIdentityDaoMock;
import org.mobicents.servlet.restcomm.dao.mocks.OrganizationsDaoMock;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.exceptions.NotAuthenticated;
import org.mobicents.servlet.restcomm.http.exceptions.OrganizationAccessForbidden;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class SecuredEndpointMockedTest extends EndpointMockedTest {

    @Test
    public void TestOrganizationResolving() {
        setRestcommXmlResourcePath("/restcomm.xml");
        init();
        organizations.clear();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        organizations.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));

        // assert Default organization is returned for localhost and 127.0.0.1 namespaces
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        Assert.assertTrue( "Default organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationsDao).getNamespace().equals("default") );
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://127.0.0.1/restcomm"));
        Assert.assertTrue( "Default organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationsDao).getNamespace().equals("default") );
        // assert existing organization is resolved
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud.com/restcomm"));
        Assert.assertTrue( "telestax organization expected", SecuredEndpoint.findCurrentOrganization(request,organizationsDao).getNamespace().equals("telestax") );
        // assert non-existing organization is not-resolved
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://fake.mycloud.com/restcomm"));
        Assert.assertNull( "null organization should be returned when a namespace has no respective organization", SecuredEndpoint.findCurrentOrganization(request,organizationsDao) );
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://crap"));
        Assert.assertNull( "null organization should be returned when a namespace has no respective organization", SecuredEndpoint.findCurrentOrganization(request,organizationsDao) );
    }

    @Test
    public void TestOrgIdentityResolving() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        organizations.clear();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        organizations.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI11111111111111111111111111111111"),new Sid("OR11111111111111111111111111111111"),"telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        // telestax orgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud:8080/restcomm"));
        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertEquals( "OI11111111111111111111111111111111", endpoint.findCurrentOrgIdentity(request,orgIdentityDao).getSid().toString() );
        // random orgIdentity is mapped to default organization and should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertEquals( "random123", endpoint.findCurrentOrgIdentity(request,orgIdentityDao).getName() );
    }

    @Test
    public void TestOrgIdentityResolvingWithKeycloakDisabled() {
        setRestcommXmlResourcePath("/restcomm.xml"); // no <identity/> configuration in restcomm.xml
        init();
        organizations.clear();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        organizations.add(new Organization(new Sid("OR11111111111111111111111111111111"),null,null,"Telestax Organization","telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI11111111111111111111111111111111"),new Sid("OR11111111111111111111111111111111"),"telestax",null,null));
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"random123",null,null));

        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://telestax.mycloud:8080/restcomm"));
        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull(endpoint.findCurrentOrgIdentity(request,orgIdentityDao));
        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull( endpoint.findCurrentOrgIdentity(request,orgIdentityDao) );
        // null OrgIdentity should be returned
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://crap:8080/restcomm"));
        endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        Assert.assertNull( endpoint.findCurrentOrgIdentity(request,orgIdentityDao) );
    }

    @Test(expected = OrganizationAccessForbidden.class)
    public void missingOrganizationAccessRoleShouldFail() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        orgIdentities.clear();
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"telestax",null,null));

        // a token from http://127.0.0.1:8081/ for realm 'restcomm', client 'telestax-restcomm' and role 'telestax-access' issued for user 'administrator@company.com'
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiJkYTZhZWUwYy00YWQwLTRkYzItODYxNC0yNjRlMWI3MTY4MTUiLCJleHAiOjE5MDUzMTg0OTEsIm5iZiI6MCwiaWF0IjoxNDczMzE4NDkxLCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODEvYXV0aC9yZWFsbXMvcmVzdGNvbW0iLCJhdWQiOiJ0ZWxlc3RheC1yZXN0Y29tbSIsInN1YiI6IjU0ZTgyMzY4LTc4MjYtNDYxZi04ZjZkLTJlOTI5Yzc2ODZlYSIsInR5cCI6IkJlYXJlciIsImF6cCI6InRlbGVzdGF4LXJlc3Rjb21tIiwic2Vzc2lvbl9zdGF0ZSI6ImJmMzliNTViLWUzNWItNGI4MC05YzlmLWRjZmMxYjlkMGIzNyIsImNsaWVudF9zZXNzaW9uIjoiNjc0ZDJmYjMtNDA1Mi00MTJjLTljY2ItYWJlZTU1NjNhNDk0IiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly8xMjcuMC4wLjE6ODA4MCJdLCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJBZG1pbmlzdHJhdG9yICIsInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluaXN0cmF0b3JAY29tcGFueS5jb20iLCJnaXZlbl9uYW1lIjoiQWRtaW5pc3RyYXRvciJ9.fBuGqI_8YkQvds5S_U1zv617PiREryyi6rZ9rgeoPWwqrN84BlEcWl20NCKXhF5rzg9B74I8hWfwMbSLRY9gc0HwzUJHO2FQtIEaW0QaX9q74AvNt_Kpg_MxNyomSNBx0hmUSaHirznt1SMz8GIiaLHd81YsciH9zaWfGEc2zXQG-4Yj2MvH-knsed4_HqIcQX1KAn5goTwhzS7KY-oAjbC0rwKVmjNfUKF2QJDE_zj0bjJ5QJxVlEE35qXlyxyexivhERdZ6ZPqu_VGC3fShPggHl-JKVW0hqA-kB_pmdlfdqgdSe00QFL0wnQ05KgUH-ko2zqQe7FJKGvv7mmMGg");

        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        endpoint.checkOrganizationAccess();
    }

    @Test(expected = NotAuthenticated.class)
    public void crappyBearerShouldFail() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml");
        init();
        orgIdentities.clear();
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"telestax",null,null));

        // a token from http://127.0.0.1:8081/ for realm 'restcomm', client 'telestax-restcomm' and role 'telestax-access' issued for user 'administrator@company.com'
        when(request.getHeader("Authorization")).thenReturn("Bearer CRAPPY-eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI0OWNkZjMwMC05NDQzLTRjYzUtYTI3My0zMjBjMzA4YWYxODkiLCJleHAiOjE5MDUzMTYxMzksIm5iZiI6MCwiaWF0IjoxNDczMzE2MTM5LCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODEvYXV0aC9yZWFsbXMvcmVzdGNvbW0iLCJhdWQiOiJ0ZWxlc3RheC1yZXN0Y29tbSIsInN1YiI6IjU0ZTgyMzY4LTc4MjYtNDYxZi04ZjZkLTJlOTI5Yzc2ODZlYSIsInR5cCI6IkJlYXJlciIsImF6cCI6InRlbGVzdGF4LXJlc3Rjb21tIiwic2Vzc2lvbl9zdGF0ZSI6IjA1NjA3OGRiLWY3YzktNDlhMy04MTFlLWEyN2UzODhkMWU4NiIsImNsaWVudF9zZXNzaW9uIjoiMTUyYTQzNzAtMWU2My00ZjdjLTlkNWItZDBhYTcyZDkyNmIyIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly8xMjcuMC4wLjE6ODA4MCJdLCJyZXNvdXJjZV9hY2Nlc3MiOnsidGVsZXN0YXgtcmVzdGNvbW0iOnsicm9sZXMiOlsidGVsZXN0YXgtYWNjZXNzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IkFkbWluaXN0cmF0b3IgIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWRtaW5pc3RyYXRvckBjb21wYW55LmNvbSIsImdpdmVuX25hbWUiOiJBZG1pbmlzdHJhdG9yIn0.Cog5AYRDVZUPkwsa_XTu_Fzwakf0Khq2g9eyOwfhND6hMPb24VTDUKNk6i_hQro5ZneONvuV6FpvGCsCY3uPlp4x_PKUScw5_Gq91Xa8H7Jsi5xElNvtk3pET9ObhCp37qauXCmj9EyGRIKnlL4hotmKfYfwVBNbw3FuhKVO_CYUZnyGUZJeFrbDjulgX_t4M43uWDM0mL0LleVEqeKTXqKzn936np1h16Vzcw48RvE7R4AQzS4RMHsrX6qtE57KCEc7ru2pck3cINcGBcJjv1QgRdU0-VvmlcnED0_uLRLR8KvkJvB-_L7IvRJXj1opbsihhqRgjhi_T32WADqGSA");

        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        endpoint.checkOrganizationAccess();
    }

    @Test
    public void organizationAccessIsDisabledWhenNoKeycloak() {
        setRestcommXmlResourcePath("/restcomm.xml");
        init();

        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));
        // also explicitly call checkOrganizationAccess (instead of implicit call in init()
        endpoint.checkOrganizationAccess(); // no exceptions should be thrown here
    }

    @Test
    public void methodHasKeycloakUserRole() {
        setRestcommXmlResourcePath("/restcomm_keycloak.xml"); // no <identity/> configuration in restcomm.xml
        init();
        orgIdentities.clear();
        orgIdentities.add(new OrgIdentity(new Sid("OI00000000000000000000000000000000"),new Sid("OR00000000000000000000000000000000"),"telestax",null,null));

        // a token from http://127.0.0.1:8081/ for realm 'restcomm', client 'telestax-restcomm' and role 'telestax-access' issued for user 'administrator@company.com'
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI0OWNkZjMwMC05NDQzLTRjYzUtYTI3My0zMjBjMzA4YWYxODkiLCJleHAiOjE5MDUzMTYxMzksIm5iZiI6MCwiaWF0IjoxNDczMzE2MTM5LCJpc3MiOiJodHRwOi8vMTI3LjAuMC4xOjgwODEvYXV0aC9yZWFsbXMvcmVzdGNvbW0iLCJhdWQiOiJ0ZWxlc3RheC1yZXN0Y29tbSIsInN1YiI6IjU0ZTgyMzY4LTc4MjYtNDYxZi04ZjZkLTJlOTI5Yzc2ODZlYSIsInR5cCI6IkJlYXJlciIsImF6cCI6InRlbGVzdGF4LXJlc3Rjb21tIiwic2Vzc2lvbl9zdGF0ZSI6IjA1NjA3OGRiLWY3YzktNDlhMy04MTFlLWEyN2UzODhkMWU4NiIsImNsaWVudF9zZXNzaW9uIjoiMTUyYTQzNzAtMWU2My00ZjdjLTlkNWItZDBhYTcyZDkyNmIyIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly8xMjcuMC4wLjE6ODA4MCJdLCJyZXNvdXJjZV9hY2Nlc3MiOnsidGVsZXN0YXgtcmVzdGNvbW0iOnsicm9sZXMiOlsidGVsZXN0YXgtYWNjZXNzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IkFkbWluaXN0cmF0b3IgIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWRtaW5pc3RyYXRvckBjb21wYW55LmNvbSIsImdpdmVuX25hbWUiOiJBZG1pbmlzdHJhdG9yIn0.Cog5AYRDVZUPkwsa_XTu_Fzwakf0Khq2g9eyOwfhND6hMPb24VTDUKNk6i_hQro5ZneONvuV6FpvGCsCY3uPlp4x_PKUScw5_Gq91Xa8H7Jsi5xElNvtk3pET9ObhCp37qauXCmj9EyGRIKnlL4hotmKfYfwVBNbw3FuhKVO_CYUZnyGUZJeFrbDjulgX_t4M43uWDM0mL0LleVEqeKTXqKzn936np1h16Vzcw48RvE7R4AQzS4RMHsrX6qtE57KCEc7ru2pck3cINcGBcJjv1QgRdU0-VvmlcnED0_uLRLR8KvkJvB-_L7IvRJXj1opbsihhqRgjhi_T32WADqGSA");

        SecuredEndpoint endpoint = new SecuredEndpoint(servletContext,request);
        endpoint.init(conf.subset("runtime-settings"));

        Assert.assertFalse("Role is present while it shouldn't", endpoint.hasKeycloakUserRole("foo-role"));
        String existingRole = "telestax-access";
        Assert.assertTrue("Role is missing: " + existingRole,endpoint.hasKeycloakUserRole(existingRole));
    }

}

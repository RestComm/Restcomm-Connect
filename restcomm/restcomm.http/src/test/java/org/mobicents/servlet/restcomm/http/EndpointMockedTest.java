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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.mocks.AccountsDaoMock;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.mocks.DaoManagerMock;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.dao.mocks.OrgIdentityDaoMock;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.dao.mocks.OrganizationsDaoMock;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityContext;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Base class for unit testing restcomm endpoints by mocking the following dependent components:
 *
 *      - Configuration
 *      - DaoManager and individual daos
 *      - ServletContext
 *      - HttpServletRequest
 *
 *  Dafaults
 *
 *      - an organization named 'default' is in place
 *      - no OrgIdentity is present
 *      - user administrator@company.com/RestComm is in place
 *      - any incoming request targets http://127.0.0.1:8080/
 *      - any incoming request caries administrator@company.com credentials (basic auth)
 *
 * Extend this class and further customize it to test other endpoints. See sample AccountsEndpointMockedTest
 * to get an idea how this works.
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class EndpointMockedTest {

    String restcommXmlResourcePath;
    Configuration conf;
    ServletContext servletContext;
    List<Account> accounts;
    AccountsDao accountsDao;
    List<Organization> organizations;
    OrganizationsDao organizationsDao;
    List<OrgIdentity> orgIdentities;
    OrgIdentityDao orgIdentityDao;
    DaoManagerMock daoManager;
    HttpServletRequest request;
    RestcommConfiguration restcommConfiguration;


    void init() {
        if (restcommXmlResourcePath == null)
            restcommXmlResourcePath = "/restcomm.xml";
        String restcommXmlPath = AccountsEndpointMockedTest.class.getResource(restcommXmlResourcePath).getFile();
        try {
            conf = getConfiguration(restcommXmlPath, "/restcomm", "http://localhost:8080");
        } catch (ConfigurationException e) {
            throw new RuntimeException();
        }
        restcommConfiguration = new RestcommConfiguration(conf);
        // create ServletContext mock
        servletContext = Mockito.mock(ServletContext.class);
        when(servletContext.getAttribute(Configuration.class.getName())).thenReturn(conf);
        // mock accountsDao
        accounts = new ArrayList<Account>();
        accounts.add(new Account(new Sid("AC00000000000000000000000000000000"),null,null,"administrator@company.com","Administrator",null,null,null,"77f8c12cc7b8f8423e5c38b035249166","Administrator",null, true));
        accountsDao = new AccountsDaoMock(accounts);
        // mock OrganizationsDao
        organizations = new ArrayList<Organization>();
        organizations.add(new Organization(new Sid("OR00000000000000000000000000000000"),null,null,"Default organization","default",null,null));
        organizationsDao = new OrganizationsDaoMock(organizations);
        // mock OrgIdentityDao
        orgIdentities = new ArrayList<OrgIdentity>();
        orgIdentityDao = new OrgIdentityDaoMock(orgIdentities);
        // mock DaoManager
        daoManager = new DaoManagerMock();
        daoManager.setAccountsDao(accountsDao);
        daoManager.setOrganizationsDao(organizationsDao);
        daoManager.setOrgIdentityDao(orgIdentityDao);
        when(servletContext.getAttribute(DaoManager.class.getName())).thenReturn(daoManager);
        // mock root Configuration
        when(servletContext.getAttribute(Configuration.class.getName())).thenReturn(conf);
        // mock IdentityContext
        IdentityContext identityContext = new IdentityContext(conf, restcommConfiguration.getMain(),orgIdentityDao);
        when(servletContext.getAttribute(IdentityContext.class.getName())).thenReturn(identityContext);
        // createt request mock
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic YWRtaW5pc3RyYXRvckBjb21wYW55LmNvbTo3N2Y4YzEyY2M3YjhmODQyM2U1YzM4YjAzNTI0OTE2Ng==");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://127.0.0.1:8080/")); // use this as default. Different implementation can override this value
    }

    private Configuration getConfiguration(String path, String homeDirectory, String rootUri) throws ConfigurationException {
        Configuration xml = null;

        XMLConfiguration xmlConfiguration = new XMLConfiguration();
        xmlConfiguration.setDelimiterParsingDisabled(true);
        xmlConfiguration.setAttributeSplittingDisabled(true);
        xmlConfiguration.load(path);
        xml = xmlConfiguration;
        xml.setProperty("runtime-settings.home-directory", homeDirectory);
        xml.setProperty("runtime-settings.root-uri", rootUri);
        return xml;
    }

    public void setRestcommXmlResourcePath(String restcommXmlResourcePath) {
        this.restcommXmlResourcePath = restcommXmlResourcePath;
    }
}

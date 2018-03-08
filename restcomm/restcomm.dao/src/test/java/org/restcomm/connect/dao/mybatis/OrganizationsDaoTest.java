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
package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.OrganizationsDao;
import org.restcomm.connect.dao.entities.Organization;

import junit.framework.Assert;

public class OrganizationsDaoTest extends DaoTest {
    private static MybatisDaoManager manager;

    public OrganizationsDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("organizationsTest");
        String mybatisFilesPath = getClass().getResource("/organizationsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() throws Exception {
        manager.shutdown();
        removeTempDir(sandboxRoot.getAbsolutePath());
    }

    @Test
    public void addOrganizationsTest() throws IllegalArgumentException, URISyntaxException {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        dao.addOrganization(new Organization(sid, "test.restcomm.com", new DateTime(), new DateTime(), Organization.Status.ACTIVE));
        Organization organization = dao.getOrganization(sid);
        Assert.assertNotNull("Organization not found",organization);
        Assert.assertNotNull(organization.getSid());
    }

    @Test
    public void addOrganizationsTestWithDot() throws IllegalArgumentException, URISyntaxException {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        dao.addOrganization(new Organization(sid, "test.restcomm.com.", new DateTime(), new DateTime(), Organization.Status.ACTIVE));
        Organization organization = dao.getOrganization(sid);
        Assert.assertEquals("test.restcomm.com",organization.getDomainName());
    }

    @Test
    public void readOrganization() {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Organization organization = dao.getOrganization(new Sid("ORafbe225ad37541eba518a74248f0ac4d"));
        Assert.assertNotNull("Organization not found",organization);
    }

    @Test
    public void readOrganizationByDomainDomain() {
        OrganizationsDao dao = manager.getOrganizationsDao();
        Organization organization = dao.getOrganizationByDomainName("test2.restcomm.com");
        Assert.assertNotNull("Organization not found",organization);
        organization = dao.getOrganizationByDomainName("test2.restcomm.com.");
        Assert.assertNotNull("Organization not found",organization);
    }

}

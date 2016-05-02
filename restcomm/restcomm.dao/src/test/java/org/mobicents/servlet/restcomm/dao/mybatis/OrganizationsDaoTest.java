/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author guilherme.jansen@telestax.com
 */
public class OrganizationsDaoTest {
    private static MybatisDaoManager manager;

    public OrganizationsDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void createReadUpdateDelete() {
        final Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final Organization.Builder builder = Organization.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Organization Test");
        builder.setNamespace("organization");
        builder.setApiVersion("2012-04-24");
        builder.setUri(url);
        Organization organization = builder.build();
        final OrganizationsDao organizations = manager.getOrganizationsDao();
        // Create a new organization in the data store
        organizations.addOrganization(organization);
        // Read the organization from the data store
        Organization result = organizations.getOrganization(sid);
        // Validate the results
        assertTrue(result.getSid().equals(organization.getSid()));
        assertTrue(result.getFriendlyName().equals(organization.getFriendlyName()));
        assertTrue(result.getNamespace().equals(organization.getNamespace()));
        assertTrue(result.getApiVersion().equals(organization.getApiVersion()));
        assertTrue(result.getUri().equals(organization.getUri()));
        // Update the application
        organization = organization.setFriendlyName("Test Organization");
        organization = organization.setNamespace("test");
        organizations.updateOrganization(organization);
        // Read the updated application from the data store
        result = organizations.getOrganization(sid);
        // Validate the results
        assertTrue(result.getSid().equals(organization.getSid()));
        assertTrue(result.getFriendlyName().equals(organization.getFriendlyName()));
        assertTrue(result.getNamespace().equals(organization.getNamespace()));
        assertTrue(result.getApiVersion().equals(organization.getApiVersion()));
        assertTrue(result.getUri().equals(organization.getUri()));
        // Delete the organization
        organizations.removeOrganization(sid);
        // Validate that the organization was removed
        assertTrue(organizations.getOrganization(sid) == null);
    }
}

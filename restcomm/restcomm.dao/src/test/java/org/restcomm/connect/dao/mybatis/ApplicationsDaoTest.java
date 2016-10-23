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

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ApplicationsDaoTest {
    private static MybatisDaoManager manager;

    public ApplicationsDaoTest() {
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
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        Application.Kind kind = Application.Kind.VOICE;
        final Application.Builder builder = Application.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Application");
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setUri(url);
        builder.setRcmlUrl(url);
        builder.setKind(kind);
        Application application = builder.build();
        final ApplicationsDao applications = manager.getApplicationsDao();
        // Create a new application in the data store.
        applications.addApplication(application);
        // Read the application from the data store.
        Application result = applications.getApplication(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(application.getSid()));
        assertTrue(result.getFriendlyName().equals(application.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(application.getAccountSid()));
        assertTrue(result.getApiVersion().equals(application.getApiVersion()));
        assertFalse(result.hasVoiceCallerIdLookup());
        assertTrue(result.getUri().equals(application.getUri()));
        assertTrue(result.getRcmlUrl().equals(application.getRcmlUrl()));
        assertTrue(result.getKind().toString().equals(application.getKind().toString()));
        // Update the application.
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/world-hello.xml");
        kind = Application.Kind.SMS;
        application = application.setFriendlyName("Application Test");
        application = application.setVoiceCallerIdLookup(true);
        application = application.setRcmlUrl(url);
        application = application.setKind(kind);
        applications.updateApplication(application);
        // Read the updated application from the data store.
        result = applications.getApplication(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(application.getSid()));
        assertTrue(result.getFriendlyName().equals(application.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(application.getAccountSid()));
        assertTrue(result.getApiVersion().equals(application.getApiVersion()));
        assertTrue(result.hasVoiceCallerIdLookup());
        assertTrue(result.getUri().equals(application.getUri()));
        assertTrue(result.getRcmlUrl().equals(application.getRcmlUrl()));
        assertTrue(result.getKind().toString().equals(application.getKind().toString()));
        // Delete the application.
        applications.removeApplication(sid);
        // Validate that the application was removed.
        assertTrue(applications.getApplication(sid) == null);
    }

    @Test
    public void removeByAccountSid() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        Application.Kind kind = Application.Kind.VOICE;
        final Application.Builder builder = Application.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Application");
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setUri(url);
        builder.setRcmlUrl(url);
        builder.setKind(kind);
        Application application = builder.build();
        final ApplicationsDao applications = manager.getApplicationsDao();
        // Create a new application in the data store.
        applications.addApplication(application);
        assertTrue(applications.getApplications(account).size() == 1);
        // Delete the application.
        applications.removeApplications(account);
        assertTrue(applications.getApplications(account).size() == 0);
    }
}

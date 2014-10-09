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
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Sid;

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
        String method = "GET";
        final Application.Builder builder = Application.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Application");
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setStatusCallback(url);
        builder.setStatusCallbackMethod(method);
        builder.setSmsUrl(url);
        builder.setSmsMethod(method);
        builder.setSmsFallbackUrl(url);
        builder.setSmsFallbackMethod(method);
        builder.setSmsStatusCallback(url);
        builder.setUri(url);
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
        assertTrue(result.getVoiceUrl().equals(application.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(application.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(application.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(application.getVoiceFallbackMethod()));
        assertTrue(result.getStatusCallback().equals(application.getStatusCallback()));
        assertTrue(result.getStatusCallbackMethod().equals(application.getStatusCallbackMethod()));
        assertTrue(result.getSmsUrl().equals(application.getSmsUrl()));
        assertTrue(result.getSmsMethod().equals(application.getSmsMethod()));
        assertTrue(result.getSmsFallbackUrl().equals(application.getSmsFallbackUrl()));
        assertTrue(result.getSmsFallbackMethod().equals(application.getSmsFallbackMethod()));
        assertTrue(result.getSmsStatusCallback().equals(application.getSmsStatusCallback()));
        assertTrue(result.getUri().equals(application.getUri()));
        // Update the application.
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/world-hello.xml");
        method = "POST";
        application = application.setFriendlyName("Application Test");
        application = application.setVoiceCallerIdLookup(true);
        application = application.setVoiceUrl(url);
        application = application.setVoiceMethod(method);
        application = application.setVoiceFallbackUrl(url);
        application = application.setVoiceFallbackMethod(method);
        application = application.setStatusCallback(url);
        application = application.setStatusCallbackMethod(method);
        application = application.setSmsUrl(url);
        application = application.setSmsMethod(method);
        application = application.setSmsFallbackUrl(url);
        application = application.setSmsFallbackMethod(method);
        application = application.setSmsStatusCallback(url);
        applications.updateApplication(application);
        // Read the updated application from the data store.
        result = applications.getApplication(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(application.getSid()));
        assertTrue(result.getFriendlyName().equals(application.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(application.getAccountSid()));
        assertTrue(result.getApiVersion().equals(application.getApiVersion()));
        assertTrue(result.hasVoiceCallerIdLookup());
        assertTrue(result.getVoiceUrl().equals(application.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(application.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(application.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(application.getVoiceFallbackMethod()));
        assertTrue(result.getStatusCallback().equals(application.getStatusCallback()));
        assertTrue(result.getStatusCallbackMethod().equals(application.getStatusCallbackMethod()));
        assertTrue(result.getSmsUrl().equals(application.getSmsUrl()));
        assertTrue(result.getSmsMethod().equals(application.getSmsMethod()));
        assertTrue(result.getSmsFallbackUrl().equals(application.getSmsFallbackUrl()));
        assertTrue(result.getSmsFallbackMethod().equals(application.getSmsFallbackMethod()));
        assertTrue(result.getSmsStatusCallback().equals(application.getSmsStatusCallback()));
        assertTrue(result.getUri().equals(application.getUri()));
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
        String method = "GET";
        final Application.Builder builder = Application.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Test Application");
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setStatusCallback(url);
        builder.setStatusCallbackMethod(method);
        builder.setSmsUrl(url);
        builder.setSmsMethod(method);
        builder.setSmsFallbackUrl(url);
        builder.setSmsFallbackMethod(method);
        builder.setSmsStatusCallback(url);
        builder.setUri(url);
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

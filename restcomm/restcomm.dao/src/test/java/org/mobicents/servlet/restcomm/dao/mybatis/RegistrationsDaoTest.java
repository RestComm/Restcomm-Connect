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

import static org.junit.Assert.*;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RegistrationsDaoTest {
    private static MybatisDaoManager manager;

    public RegistrationsDaoTest() {
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
        final Sid sid = Sid.generate(Sid.Type.REGISTRATION);
        final DateTime now = DateTime.now();
        String username = "tom_" + now;
        String displayName = "Tom_" + now;
        Registration registration = new Registration(sid, now, now, now, "sip:tom@company.com", displayName, username,
                "TestUserAgent/1.0", 3600, "sip:tom@company.com");
        final RegistrationsDao registrations = manager.getRegistrationsDao();
        // Create a new registration in the data store.
        assertFalse(registrations.hasRegistration(registration));
        registrations.addRegistration(registration);
        assertTrue(registrations.hasRegistration(registration));
        // Read the registration from the data store.
        Registration result = registrations.getRegistration(username);
        // Validate the results.
        assertTrue(registrations.getRegistrations().size() >= 1);
        assertTrue(result.getSid().equals(registration.getSid()));
        assertTrue(result.getDateCreated().equals(registration.getDateCreated()));
        assertTrue(result.getDateUpdated().equals(registration.getDateUpdated()));
        assertTrue(result.getDateExpires().equals(registration.getDateExpires()));
        assertTrue(result.getAddressOfRecord().equals(registration.getAddressOfRecord()));
        assertTrue(result.getDisplayName().equals(registration.getDisplayName()));
        assertTrue(result.getUserName().equals(registration.getUserName()));
        assertTrue(result.getLocation().equals(registration.getLocation()));
        assertTrue(result.getUserAgent().equals(registration.getUserAgent()));
        assertTrue(result.getTimeToLive() == registration.getTimeToLive());
        // Update the registration.
        registration = registration.setTimeToLive(3600);
        registrations.updateRegistration(registration);
        // Read the updated registration from the data store.
        result = registrations.getRegistration(username);
        // Validate the results.
        assertTrue(result.getSid().equals(registration.getSid()));
        assertTrue(result.getDateCreated().equals(registration.getDateCreated()));
        assertTrue(result.getDateUpdated() != null);
        assertTrue(result.getDateExpires().equals(registration.getDateExpires()));
        assertTrue(result.getAddressOfRecord().equals(registration.getAddressOfRecord()));
        assertTrue(result.getDisplayName().equals(registration.getDisplayName()));
        assertTrue(result.getUserName().equals(registration.getUserName()));
        assertTrue(result.getLocation().equals(registration.getLocation()));
        assertTrue(result.getUserAgent().equals(registration.getUserAgent()));
        assertTrue(result.getTimeToLive() == registration.getTimeToLive());
        // Delete the registration.
        registrations.removeRegistration(registration);
        // Validate that the registration was removed.
        assertTrue(registrations.getRegistrations().isEmpty());
    }

    @Test
    public void checkHasRegistrationWithoutUA() {
        final Sid sid = Sid.generate(Sid.Type.REGISTRATION);
        final DateTime now = DateTime.now();
        String username = "tom_" + now;
        String displayName = "Tom_" + now;
        Registration registration = new Registration(sid, now, now, now, "sip:tom@company.com", displayName, username, null,
                3600, "sip:tom@company.com");
        final RegistrationsDao registrations = manager.getRegistrationsDao();
        // Create a new registration in the data store.
        assertFalse(registrations.hasRegistration(registration));
        registrations.addRegistration(registration);
        assertTrue(registrations.getRegistrations().size() > 0);
        assertNotNull(registrations.getRegistration(username));
        // Expected to fail if UA is null
        assertFalse(registrations.hasRegistration(registration));

    }

    @Test
    public void checkHasRegistrationWithoutDisplayName() {
        final Sid sid = Sid.generate(Sid.Type.REGISTRATION);
        final DateTime now = DateTime.now();
        String username = "tom_" + now;
        String displayName = null;
        Registration registration = new Registration(sid, now, now, now, "sip:tom@company.com", displayName, username,
                "TestUserAgent/1.0", 3600, "sip:tom@company.com");
        final RegistrationsDao registrations = manager.getRegistrationsDao();
        // Create a new registration in the data store.
        assertFalse(registrations.hasRegistration(registration));
        registrations.addRegistration(registration);
        assertTrue(registrations.getRegistrations().size() > 0);
        assertNotNull(registrations.getRegistration(username));
        // Expected to fail if Display Name is null
        assertFalse(registrations.hasRegistration(registration));
    }

}

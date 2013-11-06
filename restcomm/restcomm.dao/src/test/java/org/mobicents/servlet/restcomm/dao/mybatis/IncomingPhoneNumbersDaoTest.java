/*
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

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class IncomingPhoneNumbersDaoTest {
    private static MybatisDaoManager manager;

    public IncomingPhoneNumbersDaoTest() {
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
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        Sid account = Sid.generate(Sid.Type.ACCOUNT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        String method = "GET";
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Incoming Phone Number Test");
        builder.setAccountSid(account);
        builder.setPhoneNumber("+12223334444");
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setStatusCallback(url);
        builder.setStatusCallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setSmsUrl(url);
        builder.setSmsMethod(method);
        builder.setSmsFallbackUrl(url);
        builder.setSmsFallbackMethod(method);
        builder.setSmsApplicationSid(application);
        builder.setUri(url);
        IncomingPhoneNumber number = builder.build();
        final IncomingPhoneNumbersDao numbers = manager.getIncomingPhoneNumbersDao();
        // Create a new incoming phone number in the data store.
        numbers.addIncomingPhoneNumber(number);
        // Read the incoming phone number from the data store.
        IncomingPhoneNumber result = numbers.getIncomingPhoneNumber(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(number.getSid()));
        assertTrue(result.getFriendlyName().equals(number.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(number.getAccountSid()));
        assertTrue(result.getPhoneNumber().equals(number.getPhoneNumber()));
        assertTrue(result.getApiVersion().equals(number.getApiVersion()));
        assertFalse(result.hasVoiceCallerIdLookup());
        assertTrue(result.getVoiceUrl().equals(number.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(number.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(number.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(number.getVoiceFallbackMethod()));
        assertTrue(result.getStatusCallback().equals(number.getStatusCallback()));
        assertTrue(result.getStatusCallbackMethod().equals(number.getStatusCallbackMethod()));
        assertTrue(result.getVoiceApplicationSid().equals(number.getVoiceApplicationSid()));
        assertTrue(result.getSmsUrl().equals(number.getSmsUrl()));
        assertTrue(result.getSmsMethod().equals(number.getSmsMethod()));
        assertTrue(result.getSmsFallbackUrl().equals(number.getSmsFallbackUrl()));
        assertTrue(result.getSmsFallbackMethod().equals(number.getSmsFallbackMethod()));
        assertTrue(result.getSmsApplicationSid().equals(number.getSmsApplicationSid()));
        assertTrue(result.getUri().equals(number.getUri()));
        // Update the incoming phone number.
        application = Sid.generate(Sid.Type.APPLICATION);
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/world-hello.xml");
        method = "POST";
        number = number.setFriendlyName("Test Application");
        number = number.setVoiceCallerIdLookup(true);
        number = number.setVoiceUrl(url);
        number = number.setVoiceMethod(method);
        number = number.setVoiceFallbackUrl(url);
        number = number.setVoiceFallbackMethod(method);
        number = number.setStatusCallback(url);
        number = number.setStatusCallbackMethod(method);
        number = number.setVoiceApplicationSid(application);
        number = number.setSmsUrl(url);
        number = number.setSmsMethod(method);
        number = number.setSmsFallbackUrl(url);
        number = number.setSmsFallbackMethod(method);
        number = number.setSmsApplicationSid(application);
        numbers.updateIncomingPhoneNumber(number);
        // Read the updated application from the data store.
        result = numbers.getIncomingPhoneNumber(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(number.getSid()));
        assertTrue(result.getFriendlyName().equals(number.getFriendlyName()));
        assertTrue(result.getAccountSid().equals(number.getAccountSid()));
        assertTrue(result.getPhoneNumber().equals(number.getPhoneNumber()));
        assertTrue(result.getApiVersion().equals(number.getApiVersion()));
        assertTrue(result.hasVoiceCallerIdLookup());
        assertTrue(result.getVoiceUrl().equals(number.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(number.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(number.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(number.getVoiceFallbackMethod()));
        assertTrue(result.getStatusCallback().equals(number.getStatusCallback()));
        assertTrue(result.getStatusCallbackMethod().equals(number.getStatusCallbackMethod()));
        assertTrue(result.getVoiceApplicationSid().equals(number.getVoiceApplicationSid()));
        assertTrue(result.getSmsUrl().equals(number.getSmsUrl()));
        assertTrue(result.getSmsMethod().equals(number.getSmsMethod()));
        assertTrue(result.getSmsFallbackUrl().equals(number.getSmsFallbackUrl()));
        assertTrue(result.getSmsFallbackMethod().equals(number.getSmsFallbackMethod()));
        assertTrue(result.getSmsApplicationSid().equals(number.getSmsApplicationSid()));
        assertTrue(result.getUri().equals(number.getUri()));
        // Delete the incoming phone number.
        numbers.removeIncomingPhoneNumber(sid);
        // Validate that the incoming phone number was removed.
        assertTrue(numbers.getIncomingPhoneNumber(sid) == null);
    }

    @Test
    public void getByPhoneNumber() {
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        Sid account = Sid.generate(Sid.Type.ACCOUNT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        String method = "GET";
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Incoming Phone Number Test");
        builder.setAccountSid(account);
        builder.setPhoneNumber("+12223334444");
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setStatusCallback(url);
        builder.setStatusCallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setSmsUrl(url);
        builder.setSmsMethod(method);
        builder.setSmsFallbackUrl(url);
        builder.setSmsFallbackMethod(method);
        builder.setSmsApplicationSid(application);
        builder.setUri(url);
        IncomingPhoneNumber number = builder.build();
        final IncomingPhoneNumbersDao numbers = manager.getIncomingPhoneNumbersDao();
        // Create a new incoming phone number in the data store.
        numbers.addIncomingPhoneNumber(number);
        // Read the incoming phone number from the data store.
        IncomingPhoneNumber result = numbers.getIncomingPhoneNumber("+12223334444");
        assert (result != null);
        assertTrue(result.getSid().equals(number.getSid()));
        // Delete the incoming phone number.
        numbers.removeIncomingPhoneNumber(sid);
        // Validate that the incoming phone number was removed.
        assertTrue(numbers.getIncomingPhoneNumber(sid) == null);
    }

    @Test
    public void removeByAccountSid() {
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        Sid account = Sid.generate(Sid.Type.ACCOUNT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        String method = "GET";
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Incoming Phone Number Test");
        builder.setAccountSid(account);
        builder.setPhoneNumber("+12223334444");
        builder.setApiVersion("2012-04-24");
        builder.setHasVoiceCallerIdLookup(false);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setStatusCallback(url);
        builder.setStatusCallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setSmsUrl(url);
        builder.setSmsMethod(method);
        builder.setSmsFallbackUrl(url);
        builder.setSmsFallbackMethod(method);
        builder.setSmsApplicationSid(application);
        builder.setUri(url);
        IncomingPhoneNumber number = builder.build();
        final IncomingPhoneNumbersDao numbers = manager.getIncomingPhoneNumbersDao();
        // Create a new incoming phone number in the data store.
        numbers.addIncomingPhoneNumber(number);
        assertTrue(numbers.getIncomingPhoneNumbers(account).size() == 1);
        // Delete the incoming phone number.
        numbers.removeIncomingPhoneNumbers(account);
        assertTrue(numbers.getIncomingPhoneNumbers(account).size() == 0);
    }
}

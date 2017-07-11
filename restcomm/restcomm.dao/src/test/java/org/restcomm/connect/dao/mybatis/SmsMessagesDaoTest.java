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
import java.math.BigDecimal;
import java.net.URI;
import java.text.ParseException;
import java.util.Currency;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.dao.SmsMessagesDao;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.SmsMessage;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsMessagesDaoTest {
    private static Logger logger = Logger.getLogger(SmsMessagesDaoTest.class);
    private static MybatisDaoManager manager;

    public SmsMessagesDaoTest() {
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
        final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final URI url = URI.create("2012-04-24/Accounts/Acoount/SMS/Messages/unique-id.json");
        final SmsMessage.Builder builder = SmsMessage.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setRecipient("+12223334444");
        builder.setSender("+17778889999");
        builder.setBody("Hello World!");
        builder.setStatus(SmsMessage.Status.SENDING);
        builder.setDirection(SmsMessage.Direction.INBOUND);
        builder.setPrice(new BigDecimal("0.00"));
        builder.setPriceUnit(Currency.getInstance("USD"));
        builder.setUri(url);
        SmsMessage message = builder.build();
        final SmsMessagesDao messages = manager.getSmsMessagesDao();
        // Create a new sms message in the data store.
        messages.addSmsMessage(message);
        // Read the message from the data store.
        SmsMessage result = messages.getSmsMessage(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(message.getSid()));
        assertTrue(result.getAccountSid().equals(message.getAccountSid()));
        assertTrue(result.getApiVersion().equals(message.getApiVersion()));
        assertTrue(result.getDateSent() == message.getDateSent());
        assertTrue(result.getRecipient().equals(message.getRecipient()));
        assertTrue(result.getSender().equals(message.getSender()));
        assertTrue(result.getBody().equals(message.getBody()));
        assertTrue(result.getStatus() == message.getStatus());
        assertTrue(result.getDirection() == message.getDirection());
        assertTrue(result.getPrice().equals(message.getPrice()));
        assertTrue(result.getPriceUnit().equals(message.getPriceUnit()));
        assertTrue(result.getUri().equals(message.getUri()));
        // Update the message.
        final DateTime now = DateTime.now();
        message = message.setDateSent(now);
        message = message.setStatus(SmsMessage.Status.SENT);
        messages.updateSmsMessage(message);
        // Read the updated message from the data store.
        result = messages.getSmsMessage(sid);
        // Validate the results.
        assertTrue(result.getDateSent().equals(message.getDateSent()));
        assertTrue(result.getStatus() == message.getStatus());
        // Delete the message.
        messages.removeSmsMessage(sid);
        // Validate that the CDR was removed.
        assertTrue(messages.getSmsMessage(sid) == null);
    }

    private SmsMessage createSms(Sid account, SmsMessage.Direction direction, int i) {
        final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
        final URI url = URI.create("2012-04-24/Accounts/Acoount/SMS/Messages/unique-id.json");
        final SmsMessage.Builder builder = SmsMessage.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setRecipient("+12223334444");
        builder.setSender("+17778889999");
        builder.setBody("Hello World - " + i);
        builder.setStatus(SmsMessage.Status.SENDING);
        builder.setDirection(direction);
        builder.setPrice(new BigDecimal("0.00"));
        builder.setPriceUnit(Currency.getInstance("USD"));
        builder.setUri(url);
        SmsMessage message = builder.build();
        return message;
    }

    @Test
    public void testGetSmsMessagesLastMinute() throws InterruptedException, ParseException {
        final SmsMessagesDao messages = manager.getSmsMessagesDao();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        for (int i = 0; i < 2; i++) {
            SmsMessage message = createSms(account, SmsMessage.Direction.OUTBOUND_API, i);
            // Create a new sms message in the data store.
            messages.addSmsMessage(message);
            logger.info("Created message: " + message);
        }
        for (int i = 0; i < 2; i++) {
            SmsMessage message = createSms(account, SmsMessage.Direction.OUTBOUND_CALL, i);
            // Create a new sms message in the data store.
            messages.addSmsMessage(message);
            logger.info("Created message: " + message);
        }
        for (int i = 0; i < 2; i++) {
            SmsMessage message = createSms(account, SmsMessage.Direction.OUTBOUND_REPLY, i);
            // Create a new sms message in the data store.
            messages.addSmsMessage(message);
            logger.info("Created message: " + message);
        }
        int lastMessages = messages.getSmsMessagesPerAccountLastPerMinute(account.toString());
        logger.info("SMS Messages last minutes: " + lastMessages);
        assertEquals(6, lastMessages);
        Thread.sleep(70000);
        for (int i = 0; i < 3; i++) {
            SmsMessage message = createSms(account, SmsMessage.Direction.OUTBOUND_CALL, i);
            // Create a new sms message in the data store.
            messages.addSmsMessage(message);
            logger.info("Created message: " + message);
        }
        lastMessages = messages.getSmsMessagesPerAccountLastPerMinute(account.toString());
        logger.info("SMS Messages last minutes: " + lastMessages);
        assertEquals(3, lastMessages);
        messages.removeSmsMessages(account);
    }

    @Test
    public void testReadDeleteByAccount() {
        final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        DateTime now = DateTime.now();
        final URI url = URI.create("2012-04-24/Accounts/Acoount/SMS/Messages/unique-id.json");
        final SmsMessage.Builder builder = SmsMessage.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setDateSent(now);
        builder.setRecipient("+12223334444");
        builder.setSender("+17778889999");
        builder.setBody("Hello World!");
        builder.setStatus(SmsMessage.Status.SENDING);
        builder.setDirection(SmsMessage.Direction.INBOUND);
        builder.setPrice(new BigDecimal("0.00"));
        builder.setPriceUnit(Currency.getInstance("GBP"));
        builder.setUri(url);
        SmsMessage message = builder.build();
        final SmsMessagesDao messages = manager.getSmsMessagesDao();
        // Create a new sms message in the data store.
        messages.addSmsMessage(message);
        // Validate the results.
        assertTrue(messages.getSmsMessages(account).size() == 1);
        // Delete the message.
        messages.removeSmsMessages(account);
        // Validate the results.
        assertTrue(messages.getSmsMessages(account).size() == 0);
    }
}

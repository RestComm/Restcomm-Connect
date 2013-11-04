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
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsMessagesDaoTest {
  private static MybatisDaoManager manager;
  
  public SmsMessagesDaoTest() {
    super();
  }

  @Before public void before() {
    final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
    final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
    final SqlSessionFactory factory = builder.build(data);
    manager = new MybatisDaoManager();
    manager.start(factory);
  }
  
  @After public void after() {
    manager.shutdown();
  }
  
  @Test public void createReadUpdateDelete() {
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
  
  @Test public void testReadDeleteByAccount() {
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

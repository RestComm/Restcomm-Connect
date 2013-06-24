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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class CallDetailRecordsDaoTest {
  private static MybatisDaoManager manager;

  public CallDetailRecordsDaoTest() {
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
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Read the CDR from the data store.
    CallDetailRecord result = cdrs.getCallDetailRecord(sid);
    // Validate the results.
    assertTrue(result.getSid().equals(cdr.getSid()));
    assertTrue(result.getParentCallSid().equals(cdr.getParentCallSid()));
    assertTrue(result.getDateCreated().equals(cdr.getDateCreated()));
    assertTrue(result.getAccountSid().equals(cdr.getAccountSid()));
    assertTrue(result.getTo().equals(cdr.getTo()));
    assertTrue(result.getFrom().equals(cdr.getFrom()));
    assertTrue(result.getPhoneNumberSid().equals(cdr.getPhoneNumberSid()));
    assertTrue(result.getStatus().equals(cdr.getStatus()));
    assertTrue(result.getStartTime().equals(cdr.getStartTime()));
    assertTrue(result.getEndTime().equals(cdr.getEndTime()));
    assertTrue(result.getDuration().equals(cdr.getDuration()));
    assertTrue(result.getPrice().equals(cdr.getPrice()));
    assertTrue(result.getDirection().equals(cdr.getDirection()));
    assertTrue(result.getApiVersion().equals(cdr.getApiVersion()));
    assertTrue(result.getCallerName().equals(cdr.getCallerName()));
    assertTrue(result.getUri().equals(cdr.getUri()));
    // Update the CDR.
    cdr = cdr.setDuration(2);
    cdr = cdr.setPrice(new BigDecimal("1.00"));
    cdr = cdr.setStatus("in-progress");
    cdrs.updateCallDetailRecord(cdr);
    // Read the updated CDR from the data store.
    result = cdrs.getCallDetailRecord(sid);
    // Validate the results.
    assertTrue(result.getStatus().equals(cdr.getStatus()));
    assertTrue(result.getDuration().equals(cdr.getDuration()));
    assertTrue(result.getPrice().equals(cdr.getPrice()));
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDR was removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
  
  @Test public void testReadDeleteByAccount() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecords(account).size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecords(account);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecords(account).size() == 0);
  }
  
  public void testReadByRecipient() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecordsByRecipient("+12223334444").size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
  
  public void testReadBySender() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecordsByRecipient("+17778889999").size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
  
  public void testReadByStatus() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecordsByStatus("queued").size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
  
  public void testReadByStartTime() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    final DateTime now = DateTime.now();
    builder.setStartTime(now);
    builder.setEndTime(now);
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecordsByStartTime(now).size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
  
  public void testReadByParentCall() {
    final Sid sid = Sid.generate(Sid.Type.CALL);
    final Sid account = Sid.generate(Sid.Type.ACCOUNT);
    final Sid parent = Sid.generate(Sid.Type.CALL);
    final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
    final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
    builder.setSid(sid);
    builder.setParentCallSid(parent);
    builder.setDateCreated(DateTime.now());
    builder.setAccountSid(account);
    builder.setTo("+12223334444");
    builder.setFrom("+17778889999");
    builder.setPhoneNumberSid(phone);
    builder.setStatus("queued");
    builder.setStartTime(DateTime.now());
    builder.setEndTime(DateTime.now());
    builder.setDuration(1);
    builder.setPrice(new BigDecimal("0.00"));
    builder.setDirection("outbound-api");
    builder.setApiVersion("2012-04-24");
    builder.setCallerName("Alice");
    builder.setUri(url);
    CallDetailRecord cdr = builder.build();
    final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
    // Create a new CDR in the data store.
    cdrs.addCallDetailRecord(cdr);
    // Validate the results.
    assertTrue(cdrs.getCallDetailRecordsByParentCall(parent).size() == 1);
    // Delete the CDR.
    cdrs.removeCallDetailRecord(sid);
    // Validate that the CDRs were removed.
    assertTrue(cdrs.getCallDetailRecord(sid) == null);
  }
}

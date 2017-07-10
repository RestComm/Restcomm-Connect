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

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.CallDetailRecordFilter;

import junit.framework.Assert;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class CallDetailRecordsDaoTest extends DaoTest {
    private static MybatisDaoManager manager;

    public CallDetailRecordsDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("cdrTest");
        String mybatisFilesPath = getClass().getResource("/callDetailRecordsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
        removeTempDir(sandboxRoot.getAbsolutePath());
    }

    @Test
    public void createReadUpdateDelete() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId.toString());
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
        builder.setPriceUnit(Currency.getInstance("USD"));
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
        assertTrue(result.getPriceUnit().equals(cdr.getPriceUnit()));
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
        assertTrue(result.getPriceUnit().equals(cdr.getPriceUnit()));
        // Delete the CDR.
        cdrs.removeCallDetailRecord(sid);
        // Validate that the CDR was removed.
        assertTrue(cdrs.getCallDetailRecord(sid) == null);
    }

    @Test
    public void testReadDeleteByAccount() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("JPY"));
        builder.setDirection("outbound-api");
        builder.setApiVersion("2012-04-24");
        builder.setCallerName("Alice");
        builder.setUri(url);
        CallDetailRecord cdr = builder.build();
        final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
        // Create a new CDR in the data store.
        cdrs.addCallDetailRecord(cdr);
        // Validate the results.
        assertTrue(cdrs.getCallDetailRecordsByAccountSid(account).size() == 1);
        // Delete the CDR.
        cdrs.removeCallDetailRecords(account);
        // Validate that the CDRs were removed.
        assertTrue(cdrs.getCallDetailRecordsByAccountSid(account).size() == 0);
    }

    public void testReadByRecipient() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("EUR"));
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
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("USD"));
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
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("CZK"));
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

    @Test
    public void testReadByStartTime() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("AUD"));
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

    @Test
    public void testReadByEndTime() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("AUD"));
        builder.setDirection("outbound-api");
        builder.setApiVersion("2012-04-24");
        builder.setCallerName("Alice");
        builder.setUri(url);
        CallDetailRecord cdr = builder.build();
        final CallDetailRecordsDao cdrs = manager.getCallDetailRecordsDao();
        // Create a new CDR in the data store.
        cdrs.addCallDetailRecord(cdr);
        // Validate the results.
        assertTrue(cdrs.getCallDetailRecordsByEndTime(now).size() == 1);
        assertTrue(cdrs.getCallDetailRecordsByStarTimeAndEndTime(now).size() == 1);
        // Delete the CDR.
        cdrs.removeCallDetailRecord(sid);
        // Validate that the CDRs were removed.
        assertTrue(cdrs.getCallDetailRecord(sid) == null);
    }

    public void testReadByParentCall() {
        final Sid sid = Sid.generate(Sid.Type.CALL);
        final String instanceId = Sid.generate(Sid.Type.INSTANCE).toString();
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid parent = Sid.generate(Sid.Type.CALL);
        final Sid phone = Sid.generate(Sid.Type.PHONE_NUMBER);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
        builder.setSid(sid);
        builder.setInstanceId(instanceId);
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
        builder.setPriceUnit(Currency.getInstance("GBP"));
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

    @Test
    public void retrieveAccountCdrsRecursively() throws ParseException {
        CallDetailRecordsDao dao = manager.getCallDetailRecordsDao();

        /*
            Sample data summary

                100 calls in total
                12 calls belong to AC00000000000000000000000000000000
                5 calls belong to AC11111111111111111111111111111111
                8 calls belong to AC22222222222222222222222222222222
         */

        // read from a single account but using the 'accountSidSet' interface
        List<String> accountSidSet = new ArrayList<String>();
        accountSidSet.add("AC00000000000000000000000000000000");
        CallDetailRecordFilter filter = new CallDetailRecordFilter(null, accountSidSet, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(12, dao.getTotalCallDetailRecords(filter).intValue());
        // read cdrs of three accounts
        accountSidSet.add("AC00000000000000000000000000000000");
        accountSidSet.add("AC11111111111111111111111111111111");
        accountSidSet.add("AC22222222222222222222222222222222");
        Assert.assertEquals(25, dao.getTotalCallDetailRecords(filter).intValue());
        // pass an empty accountSid set
        accountSidSet.clear();
        Assert.assertEquals(0, dao.getTotalCallDetailRecords(filter).intValue());
        // if both an accountSid and a accountSid set are passed, only accountSidSet is taken into account
        filter = new CallDetailRecordFilter("ACae6e420f425248d6a26948c17a9e2acf", accountSidSet, null, null, null, null, null, null, null, null, null);
        accountSidSet.add("AC00000000000000000000000000000000");
        accountSidSet.add("AC11111111111111111111111111111111");
        accountSidSet.add("AC22222222222222222222222222222222");
        Assert.assertEquals(25, dao.getTotalCallDetailRecords(filter).intValue());
        // if no (null) accountSidSet is passed the method still works
        filter = new CallDetailRecordFilter("AC00000000000000000000000000000000", null, null, null, null, null, null, null, null, null, null);
        Assert.assertEquals(12, dao.getTotalCallDetailRecords(filter).intValue());
    }
}

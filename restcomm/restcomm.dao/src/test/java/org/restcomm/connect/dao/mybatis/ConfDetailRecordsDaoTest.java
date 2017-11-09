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

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceRecordCountFilter;

public class ConfDetailRecordsDaoTest extends DaoTest {

    @Rule
    public TestName testName = new TestName();

    //RUNNING_INITIALIZING, RUNNING_MODERATOR_ABSENT, RUNNING_MODERATOR_PRESENT, STOPPING, COMPLETED, FAILED
    @Rule
    public TestName name = new TestName();

    private static MybatisDaoManager manager;

    public ConfDetailRecordsDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        Properties props = new Properties();
        props.setProperty("testcase", testName.getMethodName());
        final SqlSessionFactory factory = builder.build(data, props);
        manager = new MybatisDaoManager();
        manager.start(factory);

    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void testCountByMsIdAnStatus() throws Exception {
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);

        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");

        final Sid sid = Sid.generate(Sid.Type.CONFERENCE);
        final ConferenceDetailRecord.Builder builder = ConferenceDetailRecord.builder();
        builder.setAccountSid(accountSid);
        builder.setApiVersion("2012-04-24");
        builder.setDateCreated(DateTime.now());
        builder.setFriendlyName("myconf");
        builder.setMasterConfernceEndpointId("masterConEndId");
        builder.setMasterIVREndpointId("masterIvrId");
        builder.setMasterMsId("masterMsId");
        builder.setSid(sid);
        builder.setStatus("RUNNING_INITIALIZING");
        builder.setUri(url);
        ConferenceDetailRecord cdr = builder.build();
        manager.getConferenceDetailRecordsDao().addConferenceDetailRecord(cdr);

        final Sid sid2 = Sid.generate(Sid.Type.CONFERENCE);
        final ConferenceDetailRecord.Builder builder2 = ConferenceDetailRecord.builder();
        builder2.setAccountSid(accountSid);
        builder2.setApiVersion("2012-04-24");
        builder2.setDateCreated(DateTime.now());
        builder2.setFriendlyName("myconf");
        builder2.setMasterConfernceEndpointId("masterConEndId");
        builder2.setMasterIVREndpointId("masterIvrId");
        builder2.setMasterMsId("masterMsId");
        builder2.setSid(sid2);
        builder2.setStatus("RUNNING_MODERATOR_PRESENT");
        builder2.setUri(url);
        ConferenceDetailRecord cdr2 = builder2.build();
        manager.getConferenceDetailRecordsDao().addConferenceDetailRecord(cdr2);

        final Sid sid3 = Sid.generate(Sid.Type.CONFERENCE);
        final ConferenceDetailRecord.Builder builder3 = ConferenceDetailRecord.builder();
        builder3.setAccountSid(accountSid);
        builder3.setApiVersion("2012-04-24");
        builder3.setDateCreated(DateTime.now());
        builder3.setFriendlyName("myconf");
        builder3.setMasterConfernceEndpointId("masterConEndId");
        builder3.setMasterIVREndpointId("masterIvrId");
        builder3.setMasterMsId("masterMsId");
        builder3.setSid(sid3);
        builder3.setStatus("COMPLETED");
        builder3.setUri(url);
        ConferenceDetailRecord cdr3 = builder3.build();
        manager.getConferenceDetailRecordsDao().addConferenceDetailRecord(cdr3);

        ConferenceRecordCountFilter filter = ConferenceRecordCountFilter.builder().
                byAccountSid(accountSid.toString()).
                byMasterMsId("masterMsId")
                .byStatus("RUNNING%").build();
        Integer confCount = manager.getConferenceDetailRecordsDao().countByFilter(filter);
        assertEquals(Integer.valueOf(2), confCount);
    }

    @Test
    public void testAddRead() throws Exception {
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.CONFERENCE);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");

        final ConferenceDetailRecord.Builder builder = ConferenceDetailRecord.builder();
        builder.setAccountSid(sid);
        builder.setApiVersion("2012-04-24");
        builder.setDateCreated(DateTime.now());
        builder.setFriendlyName("myconf");
        builder.setMasterConfernceEndpointId("masterConEndId");
        builder.setMasterIVREndpointId("masterIvrId");
        builder.setMasterMsId("masterMsId");
        builder.setSid(sid);
        builder.setStatus("RUNNING_INITIALIZING");
        builder.setUri(url);
        ConferenceDetailRecord cdr = builder.build();
        manager.getConferenceDetailRecordsDao().addConferenceDetailRecord(cdr);
        ConferenceDetailRecord cdrResult = manager.getConferenceDetailRecordsDao().getConferenceDetailRecord(sid);
        assertEquals(cdr.getFriendlyName(), cdrResult.getFriendlyName());
    }

}

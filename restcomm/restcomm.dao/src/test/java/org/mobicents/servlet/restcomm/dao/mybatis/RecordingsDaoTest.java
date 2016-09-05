package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class RecordingsDaoTest {
    private static MybatisDaoManager manager;

    public RecordingsDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
         org.apache.ibatis.logging.LogFactory.useSlf4jLogging();
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() throws Exception {
        manager.shutdown();
    }

    @Test
    public void createReadUpdateDelete() {
        Sid sid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.RECORDING);
        Sid accountSid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.ACCOUNT);
        Sid callSid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.CALL);
        DateTime dateCreated= new DateTime();
        DateTime dateUpdated= new DateTime();
        String friendlyName="mobicents";
        String phoneNumber="+12222211";
        URI uri= URI.create("http://www.mobicents.com");
        URI fileUri= URI.create("/");
        String apiVersion="v1";
        Double duration=60.0;
    	Recording recording= new Recording(sid, dateCreated, dateUpdated, accountSid, callSid, duration, apiVersion, uri, fileUri);
    	RecordingsDao dao=manager.getRecordingsDao();
    	//dao.addRecording(recording);
    }
}

package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.OutgoingCallerIdsDao;
import org.mobicents.servlet.restcomm.entities.OutgoingCallerId;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class OutgoingCallerIdsDaoTest {
    private static MybatisDaoManager manager;

    public OutgoingCallerIdsDaoTest() {
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
    public void createReadDelete() {
        Sid sid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.CALL);
        Sid accountSid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.ACCOUNT);
        DateTime dateCreated= new DateTime();
        DateTime dateUpdated= new DateTime();
        String friendlyName="mobicents";
        String phoneNumber="+12222211";
        URI uri= URI.create("http://www.mobicents.com");
        OutgoingCallerId ogci= new OutgoingCallerId(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, uri);
        //test insert
        OutgoingCallerIdsDao dao=manager.getOutgoingCallerIdsDao();
        dao.addOutgoingCallerId(ogci);
        ogci=dao.getOutgoingCallerId(sid);
        assertEquals(accountSid.toString(),ogci.getAccountSid().toString());
        assertEquals(friendlyName,ogci.getFriendlyName());
        List<OutgoingCallerId> list=dao.getOutgoingCallerIds(accountSid);
        assertEquals(1,list.size());
        ogci=list.get(0);
        assertEquals(accountSid.toString(),ogci.getAccountSid().toString());
        assertEquals(friendlyName,ogci.getFriendlyName());
        dao.removeOutgoingCallerId(sid);
        ogci=dao.getOutgoingCallerId(sid);
        assertNull(ogci);
    }
}

package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.InstanceIdDao;
import org.mobicents.servlet.restcomm.entities.InstanceId;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class InstanceIdDaoTest {

       private static MybatisDaoManager manager;

	    public InstanceIdDaoTest() {
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
	    	final Sid sid = Sid.generate(Sid.Type.INSTANCE);
	    	String host="www.mobicents.com";
	    	DateTime dateCreated= new DateTime();
	    	DateTime dateUpdated= new DateTime();
	        InstanceIdDao dao=manager.getInstanceIdDao();
	        InstanceId instanceId=new InstanceId(sid,host,dateCreated,dateUpdated);
	        dao.addInstancecId(instanceId);
	        // test get
	        instanceId=dao.getInstanceId();
	        assertEquals(sid.toString(),instanceId.getId().toString());
	        assertEquals(host,instanceId.getHost());
	        assertEquals(dateCreated.getMillis(),instanceId.getDateCreated().getMillis());
	        // test get by host
	        instanceId=dao.getInstanceIdByHost(host);
	        assertEquals(sid.toString(),instanceId.getId().toString());
	        assertEquals(host,instanceId.getHost());
	        assertEquals(dateCreated.getMillis(),instanceId.getDateCreated().getMillis());
	    }
}

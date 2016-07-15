package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.ShortCodesDao;
import org.mobicents.servlet.restcomm.entities.ShortCode;
import org.mobicents.servlet.restcomm.entities.Sid;

public class ShortCodesDaoTest {
	   private static MybatisDaoManager manager;

	    public ShortCodesDaoTest() {
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
	    public void createReadUpdateDelete() throws URISyntaxException {
	        ShortCodesDao dao=manager.getShortCodesDao();
	        final Sid sid = Sid.generate(Sid.Type.SHORT_CODE);
	        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
	        DateTime dateCreated= new DateTime();
	        DateTime dateUpdated= new DateTime();
	        String friendlyName="Service1";
	        Integer shortCode=1010;
	        String apiVersion="v1";
	        URI smsUrl= new URI("http://api.mobicents/shortsms");
	        String smsMethod="POST";
	        URI smsFallbackUrl=new URI("http://api.serviceprovider.com");
	        String smsFallbackMethod="POST";
	        URI uri= new URI("http://www.mobicents.com");
	        ShortCode sc= new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
	        dao.addShortCode(sc);
	        sc=dao.getShortCode(sid);
	        assertEquals(sid.toString(),sc.getSid().toString());
	        assertEquals(accountSid.toString(),sc.getAccountSid().toString());
	        assertEquals(friendlyName,sc.getFriendlyName());
	        assertEquals(smsMethod,sc.getSmsMethod());
	        List<ShortCode> list=dao.getShortCodes(accountSid);
	        assertEquals(1,list.size());
	        sc=list.get(0);
	        assertEquals(sid.toString(),sc.getSid().toString());
	        assertEquals(accountSid.toString(),sc.getAccountSid().toString());
	        assertEquals(friendlyName,sc.getFriendlyName());
	        assertEquals(smsMethod,sc.getSmsMethod());
	        dao.removeShortCode(sid);
	        list=dao.getShortCodes(accountSid);
	        assertEquals(0,list.size());
	        
	    }
}

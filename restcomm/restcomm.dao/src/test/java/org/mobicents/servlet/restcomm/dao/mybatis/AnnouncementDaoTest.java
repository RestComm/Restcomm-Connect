package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.AnnouncementsDao;
import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.Sid;

public class AnnouncementDaoTest {
	private static MybatisDaoManager manager;
	
    public AnnouncementDaoTest() {
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
    	Sid sid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.ANNOUNCEMENT);
    	Sid accountSid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.ACCOUNT);
    	String gender="male";
    	String language="en";
    	String text="this is a test";
    	URI uri= URI.create("http://www.mobicents.com");
    	Announcement announcement= new Announcement(sid, accountSid, gender, language, text, uri);
        final AnnouncementsDao dao=manager.getAnnouncementsDao();
    	
        dao.addAnnouncement(announcement);
    	
    	
    	//test get announcement by sid
       announcement=dao.getAnnouncement(sid);  
       assertEquals(sid.toString(),announcement.getSid().toString());
	   assertEquals(accountSid.toString(),announcement.getAccountSid().toString());
	   assertEquals(gender,announcement.getGender());
       assertEquals(language,announcement.getLanguage());
       assertEquals(uri,announcement.getUri());
       
       List<Announcement> list=dao.getAnnouncements(accountSid);
       assertEquals(1, list.size());
       
       dao.removeAnnouncement(sid);
       list=dao.getAnnouncements(accountSid);
       assertEquals(0, list.size());
    	
    }
    
}

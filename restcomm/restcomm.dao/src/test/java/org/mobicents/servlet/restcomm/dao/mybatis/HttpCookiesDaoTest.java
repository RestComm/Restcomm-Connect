package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.assertEquals;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.HttpCookiesDao;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class HttpCookiesDaoTest {

	private static MybatisDaoManager manager;

    public HttpCookiesDaoTest() {
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
        Sid sid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.APPLICATION);
        final String comment = "This is a test cookies";
        final String domain = "www.mobicents.com";
        final Date expirationDate = new Date();
        final String name = "COOKIES";
        final String path = "/opt/init.d";
        final String value = "Test values";
        final int version = 12;
        final BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setComment(comment);
        cookie.setDomain(domain);
        cookie.setExpiryDate(expirationDate);
        cookie.setPath(path);
        cookie.setVersion(version);

        HttpCookiesDao dao=manager.getHttpCookiesDao();

        dao.addCookie(sid, cookie);
        
        List<Cookie> list=dao.getCookies(sid);
        assertEquals(1,list.size());
        Cookie ck=list.get(0);
        assertEquals(comment,ck.getComment());
        assertEquals(domain,ck.getDomain());
        assertEquals(expirationDate,ck.getExpiryDate());
        assertEquals(path,ck.getPath());
        
        boolean result=dao.hasCookie(sid, cookie);
        assertEquals(true,result);
        
        
        
    }
}

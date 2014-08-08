package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AccountsDaoTest {
    private static MybatisDaoManager manager;

    public AccountsDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
//        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
//        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
//        final SqlSessionFactory factory = builder.build(data);
//        manager = new MybatisDaoManager();
//        manager.start(factory);
    }

    @After
    public void after() throws Exception {
//        manager.shutdown();
    }

    @Test
    public void createReadUpdateDelete() {
        assertTrue(Pattern.matches("((67565)+).*", "6756532355"));
        assertTrue(Pattern.matches("((501555\\d\\d\\d\\d)+).*", "5015554883"));
    }
}

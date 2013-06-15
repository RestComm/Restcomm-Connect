package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class AccountsDaoTest {
  private static MybatisDaoManager manager;
  
  public AccountsDaoTest() {
    super();
  }

  @Before public void before() throws Exception {
    final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
    final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
    final SqlSessionFactory factory = builder.build(data);
    manager = new MybatisDaoManager();
    manager.start(factory);
  }

  @After public void after() throws Exception {
    manager.shutdown();
  }

  @Test public void createReadUpdateDelete() {
    
  }
}

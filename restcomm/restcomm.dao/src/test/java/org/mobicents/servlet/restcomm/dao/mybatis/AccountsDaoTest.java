package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Assert;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

public class AccountsDaoTest extends DaoTest {
    private static MybatisDaoManager manager;

    public AccountsDaoTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("accountsTest");
        String mybatisFilesPath = getClass().getResource("/accountsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() throws Exception {
        manager.shutdown();
        removeTempDir(sandboxRoot.getAbsolutePath());
    }

    @Test
    public void readAccount() {
        AccountsDao dao = manager.getAccountsDao();
        Account account = dao.getAccount(new Sid("AC00000000000000000000000000000000"));
        Assert.assertNotNull("Account not found",account);
    }

}

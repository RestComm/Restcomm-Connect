package org.restcomm.connect.dao.mybatis;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.commons.dao.Sid;

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

    @Test
    public void nestedSubAccountRetrieval() {
        // retrieve all sub-accounts of AC00000000000000000000000000000000
        AccountsDao dao = manager.getAccountsDao();
        Sid parentSid = new Sid("AC00000000000000000000000000000000");
        List<String> sidList = dao.getSubAccountSidsRecursive(parentSid);
        Assert.assertEquals("Invalid number of subaccounts returned",5, sidList.size());
        // a parent with no sub-accounts should get zero
        parentSid = new Sid("AC99999999999999999999999999999999");
        sidList = dao.getSubAccountSidsRecursive(parentSid);
        Assert.assertEquals("No sub-account sids should be returned", 0, sidList.size());
        // check 2nd-level parents too
        parentSid = new Sid("AC10000000000000000000000000000000");
        sidList = dao.getSubAccountSidsRecursive(parentSid);
        Assert.assertEquals("Invalid number of sub-account for 2nd level perent", 3, sidList.size());
        // check third-level perent too
        parentSid = new Sid("AC11000000000000000000000000000000");
        sidList = dao.getSubAccountSidsRecursive(parentSid);
        Assert.assertEquals("Invalid number of sub-account for 3rd level perent", 1, sidList.size());
        // test behaviour for non-existing parents. An empty list should be returned
        parentSid = new Sid("AC59494830204948392023934839392092"); // this does not exist
        sidList = dao.getSubAccountSidsRecursive(parentSid);
        Assert.assertEquals("Invalid number of sub-account for 3rd level perent", 0, sidList.size());
    }

    @Test
    public void accountAncestorsRetrieval() throws AccountHierarchyDepthCrossed {
        AccountsDao dao = manager.getAccountsDao();

        List<String> ancestorSids = dao.getAccountLineage(new Sid("AC11000000000000000000000000000000"));
        Assert.assertEquals(2, ancestorSids.size());
        // check last account returned is the top-level
        Assert.assertEquals("AC00000000000000000000000000000000", ancestorSids.get(ancestorSids.size()-1));
        // also check the overloaded version
        Account account = dao.getAccount("AC11000000000000000000000000000000");
        ancestorSids = dao.getAccountLineage(account);
        Assert.assertEquals(2, ancestorSids.size());
        Assert.assertEquals("AC00000000000000000000000000000000", ancestorSids.get(ancestorSids.size()-1));

        // for top level accounts an empty list should be returned
        ancestorSids = dao.getAccountLineage(new Sid("AC00000000000000000000000000000000"));
        Assert.assertEquals(0, ancestorSids.size());
        Account topLevelAccount = dao.getAccount("AC00000000000000000000000000000000");
        ancestorSids = dao.getAccountLineage(topLevelAccount);
        Assert.assertEquals(0, ancestorSids.size());

        Assert.assertNull(dao.getAccountLineage((Sid)null));
    }

    @Test(expected=AccountHierarchyDepthCrossed.class)
    public void checkAccountRecursionLimit() throws AccountHierarchyDepthCrossed {
        AccountsDao dao = manager.getAccountsDao();
        // try to retrieve the lineage for an account that is in the forth level
        List<String> ancestorSids = dao.getAccountLineage(new Sid("AC11100000000000000000000000000000"));
    }

}

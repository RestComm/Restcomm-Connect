
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Account.Status;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Account.Type;
import static org.junit.Assert.*;

/**
 * @author zahid.med@gmail.com (Mohammed ZAHID)
 */
public class AccountsDaoTest {
    private static MybatisDaoManager manager;

    public AccountsDaoTest() {
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
        Sid sid=  Sid.generate(org.mobicents.servlet.restcomm.entities.Sid.Type.ACCOUNT);
        String emailAddress="test@mobicents.com";
        String friendlyName="testAcccount";
        String authToken="0x11111111111111112222222";
        String role="user";
        URI uri= URI.create("http://www.mobicents.com");
        final Account.Builder builder=Account.builder();
        builder.setSid(sid);
        builder.setAccountSid(sid);
        builder.setEmailAddress(emailAddress);
        builder.setFriendlyName(friendlyName);
        builder.setRole(role);
        builder.setAuthToken(authToken);
        builder.setStatus(Status.ACTIVE);
        builder.setType(Type.FULL);
        builder.setUri(uri);
        Account account= builder.build();
        final AccountsDao dao=manager.getAccountsDao();
        dao.addAccount(account);

        //test get account by sid
        account=dao.getAccount(sid);
        assertEquals(sid.toString(), account.getSid().toString());
        assertEquals(emailAddress, account.getEmailAddress());
        assertEquals(friendlyName, account.getFriendlyName());
        assertEquals(authToken,account.getAuthToken() );
        assertEquals(role, account.getRole());
        assertEquals(uri.toString(), account.getUri().toString());

        //test get account by email address
        account=dao.getAccount(emailAddress);
        assertEquals(sid.toString(), account.getSid().toString());
        assertEquals(emailAddress, account.getEmailAddress());
        assertEquals(friendlyName, account.getFriendlyName());
        assertEquals(authToken,account.getAuthToken() );
        assertEquals(role, account.getRole());
        assertEquals(uri.toString(), account.getUri().toString());

        //test get account by friendly name
        account=dao.getAccount(friendlyName);
        assertEquals(sid.toString(), account.getSid().toString());
        assertEquals(emailAddress, account.getEmailAddress());
        assertEquals(friendlyName, account.getFriendlyName());
        assertEquals(authToken,account.getAuthToken() );
        assertEquals(role, account.getRole());
        assertEquals(uri.toString(), account.getUri().toString());

        // test get accounts
        List<Account> accounts=dao.getAccounts(sid);
        assertEquals(1, accounts.size());
        
        account=accounts.get(0);
        assertEquals(sid.toString(), account.getSid().toString());
        assertEquals(emailAddress, account.getEmailAddress());
        assertEquals(friendlyName, account.getFriendlyName());
        assertEquals(authToken,account.getAuthToken() );
        assertEquals(role, account.getRole());
        assertEquals(uri.toString(), account.getUri().toString());
        
        //test update account
        String newEmail="test2@mobicents.com";
        String newAuthTocken="0x1234567890";
        builder.setSid(sid);
        builder.setAccountSid(sid);
        builder.setEmailAddress(newEmail);
        builder.setFriendlyName(friendlyName);
        builder.setRole(role);
        builder.setAuthToken(newAuthTocken);
        builder.setStatus(Status.ACTIVE);
        builder.setType(Type.FULL);
        builder.setUri(uri);
        account= builder.build();
        dao.updateAccount(account);
        
        account=dao.getAccount(sid);
        assertEquals(newEmail, account.getEmailAddress());
        assertEquals(newAuthTocken,account.getAuthToken() );
        
        //test remove account
        dao.removeAccount(sid);
        accounts=dao.getAccounts(sid);
        assertEquals(0, accounts.size());
        
    }
    
    
   

}

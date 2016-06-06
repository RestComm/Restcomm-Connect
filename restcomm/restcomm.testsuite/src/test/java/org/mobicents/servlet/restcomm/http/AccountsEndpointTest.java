package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.UniformInterfaceException;

import javax.sip.address.SipURI;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 * @author <a href="mailto:lyhungthinh@gmail.com">Thinh Ly</a>
 */

@RunWith(Arquillian.class)
public class AccountsEndpointTest {
    private final static Logger logger = Logger.getLogger(AccountsEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    static boolean accountUpdated = false;
    static boolean accountCreated = false;

    private String adminUsername = "administrator@company.com";
    private String adminFriendlyName = "Default Administrator Account";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String newAdminPassword = "mynewpassword";
    private String newAdminAuthToken = "8e70383c69f7a3b7ea3f71b02f3e9731";
    private String userEmailAddress = "gvagenas@restcomm.org";
    private String userPassword = "1234";
    private String unprivilegedSid = "AC00000000000000000000000000000000";
    private String unprivilegedUsername = "unprivileged@company.com";
    private String unprivilegedAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    static SipStackTool tool1;

    SipStack thinhSipStack;
    SipPhone thinhPhone;
    String thinhContact = "sip:lyhungthinh@127.0.0.1:5090";

    @BeforeClass
    public static void beforeClass() {
        tool1 = new SipStackTool("AccountsEndpointTest");
    }

    @Before
    public void before() throws Exception {
        thinhSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        thinhPhone = thinhSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, thinhContact);
    }

    @After
    public void after() throws InterruptedException {
        if (thinhPhone != null) {
            thinhPhone.dispose();
        }
        if (thinhSipStack != null) {
            thinhSipStack.dispose();
        }
        Thread.sleep(1000);
    }

    @Test
    public void testGetAccount() {
        // Get Account using admin email address and user email address
        JsonObject adminAccount = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminUsername);
        assertTrue(adminAccount.get("sid").getAsString().equals(adminAccountSid));

    }

    @Test
    public void testGetAccountByFriendlyName() {
        // Try to get Account using admin friendly name and user email address
        int code = 0;
        try {
            JsonObject adminAccount = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                    adminFriendlyName, adminAuthToken, adminUsername);
        } catch (UniformInterfaceException e) {
            code = e.getResponse().getStatus();
        }
        // Logins using friendly name are not allowed anymore
        assertTrue(code == 401);
    }

    @Test
    public void testCreateAccount() {
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminUsername, newAdminPassword, adminAccountSid, null);
        accountUpdated = true;
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, userEmailAddress, userPassword);
        accountCreated = true;
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, userEmailAddress);

        String usernameHashed = "AC" + (new Md5Hash(userEmailAddress).toString());
        assertTrue(createAccountResponse.get("sid").getAsString().equals(usernameHashed));

        assertTrue(createAccountResponse.get("auth_token").equals(getAccountResponse.get("auth_token")));
        String userPasswordHashed = new Md5Hash(userPassword).toString();
        assertTrue(getAccountResponse.get("auth_token").getAsString().equals(userPasswordHashed));
    }

    @Test
    public void testCreateAdministratorAccount() {
        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, "administrator@company.com", "1234");
        assertNull(createAccountResponse);
    }

    @Test
    public void testCreateAccountTwice() {
        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }
        if (!accountCreated) {
            JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                    adminUsername, newAdminAuthToken, userEmailAddress, userPassword);
            accountCreated = true;
            assertNotNull(createAccountResponse);
        }
        JsonObject createAccountResponseSecondTime = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, userEmailAddress, userPassword);
        assertNull(createAccountResponseSecondTime);
    }

    @Test
    public void testCreateAccountCheckClientExisted() throws ClientProtocolException, IOException, ParseException {
        String subAccountPassword = "mynewpassword";
        String subAccountEmail = "lyhungthinh@gmail.com";

        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }
        JsonObject subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, newAdminAuthToken, subAccountEmail, subAccountPassword);

        JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        assertNotNull(clientOfAccount);

        CreateClientsTool.getInstance().updateClientVoiceUrl(deploymentUrl.toString(), subAccountResponse,
                clientOfAccount.get("sid").getAsString(), "http://127.0.0.1:8080/restcomm/demos/welcome.xml",
                adminUsername, newAdminPassword);

        JsonObject clientOfAccountUpdated = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        System.out.println(clientOfAccountUpdated);

        // Use the new client to register with Restcomm
        SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        assertTrue(thinhPhone.register(reqUri, "lyhungthinh", subAccountPassword, thinhContact, 1800, 1800));
        assertTrue(thinhPhone.unregister(thinhContact, 0));

        RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, newAdminAuthToken,
                subAccountResponse.get("sid").getAsString());
    }

    @Test
    public void testUpdateAccountCheckClient() throws IOException, ParseException {
        String subAccountPassword = "mynewpassword";
        String subAccountEmail = "lyhungthinh@gmail.com";
        String subAccountNewPassword = "latestpassword";
        String subAccountNewAuthToken = "fa1930301afe5ed93a2dec29a922728e";
        JsonObject subAccountResponse;

        SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }

        subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, subAccountEmail, subAccountPassword);
        assertNotNull(subAccountResponse);
        JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        assertNotNull(clientOfAccount);
        // Use the new client to register with Restcomm
        assertTrue(thinhPhone.register(reqUri, "lyhungthinh", subAccountPassword, thinhContact, 1800, 1800));
        assertTrue(thinhPhone.unregister(thinhContact, 0));

        subAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, subAccountEmail, subAccountNewPassword, subAccountResponse.get("sid").getAsString(),
                null);
        assertTrue(subAccountResponse.get("auth_token").getAsString().equals(subAccountNewAuthToken));
        assertTrue(thinhPhone.register(reqUri, "lyhungthinh", subAccountNewPassword, thinhContact, 1800, 1800));
        assertTrue(thinhPhone.unregister(thinhContact, 0));

        clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        assertTrue(clientOfAccount.get("password").getAsString().equals(subAccountNewPassword));

        RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, newAdminAuthToken,
                subAccountResponse.get("sid").getAsString());
    }

    @Test
    public void testRemoveAccountCheckClient() throws IOException, ParseException {
        String subAccountPassword = "mynewpassword";
        String subAccountEmail = "lyhungthinh@gmail.com";
        JsonObject subAccountResponse;

        SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        if (!accountUpdated) {
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }
        subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, subAccountEmail, subAccountPassword);
        assertNotNull(subAccountResponse);
        JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        assertNotNull(clientOfAccount);
        assertTrue(thinhPhone.register(reqUri, "lyhungthinh", subAccountPassword, thinhContact, 1800, 1800));
        assertTrue(thinhPhone.unregister(thinhContact, 0));

        RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, newAdminAuthToken,
                subAccountResponse.get("sid").getAsString());
        JsonObject clientOfAccount2 = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                subAccountResponse, adminUsername, newAdminPassword);
        assertTrue(clientOfAccount2 == null);
        assertFalse(thinhPhone.register(reqUri, "lyhungthinh", subAccountPassword, thinhContact, 1800, 1800));
    }

    @Test
    public void testGetAccounts() throws InterruptedException {
        if (!accountUpdated){
            RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
                    adminUsername, newAdminPassword, adminAccountSid, null);
        }

        if (!accountCreated) {
            // Create account
            RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername, newAdminAuthToken,
                    userEmailAddress, userPassword);
        }
        // Get Account using admin email address and user email address
        JsonObject account1 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                newAdminAuthToken, userEmailAddress);
        // Get Account using admin account sid and user sid
        JsonObject account2 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminAccountSid,
                newAdminAuthToken, account1.get("sid").getAsString());

        assertTrue(account1.toString().equals(account2.toString()));

    }

    /**
     * Test access without administrator type of access. Instead a common role and permissions is used.
     */
    @Test
    public void testAccountAcccessUsingRoles() {
        ClientResponse response = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(),unprivilegedUsername, unprivilegedAuthToken, unprivilegedSid);
        Assert.assertEquals(200,response.getStatus());
    }

    @Deployment(name = "ClientsEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_accounts_test", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}

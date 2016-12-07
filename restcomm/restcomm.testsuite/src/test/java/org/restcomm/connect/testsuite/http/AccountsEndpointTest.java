package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import javax.sip.address.SipURI;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 * @author <a href="mailto:lyhungthinh@gmail.com">Thinh Ly</a>
 */

@RunWith(Arquillian.class)
public class AccountsEndpointTest extends EndpointTest {
    private final static Logger logger = Logger.getLogger(AccountsEndpointTest.class.getName());

    private static final String version = Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminFriendlyName = "Default Administrator Account";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String adminPassword = "RestComm";

    private String childUsername = "child@company.com";
    private String childSid = "AC574d775522c96f9aacacc5ca60c8c74f";
    private String childAuthToken ="77f8c12cc7b8f8423e5c38b035249166";

    private String createdUsernanme = "created@company.com";
    private String createdAccountSid = "AC5ee3b351401804c2d064a33f762146fb";
    private String createdPassword = "RestComm12";
    private String createdAuthToken = "28f96b0fea1f9e33646f42026abdf305";

    private String updatedUsername = "updated@company.com";
    private String updatedAccountSid = "AC6b53c6ffa9fa7c4682dbcf4dec73012f";

    private String userEmailAddress = "gvagenas@restcomm.org";
    private String userPassword = "1234";

    private String unprivilegedSid = "AC00000000000000000000000000000000";
    private String unprivilegedUsername = "unprivileged@company.com";
    private String unprivilegedAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private String guestSid = "AC11111111111111111111111111111111";
    private String guestUsername = "guest@company.com";
    private String guestAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private String removedSid = "AC22222222222222222222222222222222";
    private String removedUsername = "removed@company.com";
    private String removedAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private String updatedRoleUsername = "updatedRole@company.com";
    private String updatedRoleAccountSid = "AC33333333333333333333333333333333";
    private String updateRoleAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String nonSubAccountSid = "AC44444444444444444444444444444444";

    private String commonAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    static SipStackTool tool1;

    SipStack thinhSipStack;
    SipPhone thinhPhone;
    //String thinhContact = "sip:lyhungthinh@127.0.0.1:5090";
 
    @BeforeClass
    public static void beforeClass() {
        tool1 = new SipStackTool("AccountsEndpointTest");
    }

    public void startSipStack(String thinhContact) throws Exception {
        thinhSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        thinhPhone = thinhSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, thinhContact);
    }

    @After
    public void stopSipStack() throws InterruptedException {
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
    public void testGetAccountAccess(){
        // check non-existent user receives a 401
        ClientResponse response = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), "nonexisting@company.com", "badpassword", adminAccountSid);
        Assert.assertEquals("Non-existing user should get a 401", 401, response.getStatus());
        // check InsufficientPerimssion errors- 403. Try to get administrator account with unprivileged accoutn creds
        response = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), unprivilegedUsername, unprivilegedAuthToken, adminAccountSid);
        Assert.assertEquals("Unpriveleged access to account did not return 403", 403, response.getStatus());
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
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, createdUsernanme, createdPassword);
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                adminAuthToken, createdUsernanme);
        assertTrue(getAccountResponse.get("sid").getAsString().equals(createdAccountSid));
        Assert.assertEquals(createdAuthToken, getAccountResponse.get("auth_token").getAsString());
        assertTrue(createAccountResponse.get("sid").getAsString().equals(createdAccountSid));
        Assert.assertEquals(createdAuthToken, createAccountResponse.get("auth_token").getAsString());
    }

    @Test
    public void testUpdateAccount() {
        JsonObject updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedAccountSid, "updated2", "Restcomm2", null, "Developer", "active" );
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);

        Assert.assertEquals("FriendlyName field is not updated",  "updated2", updateAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());
       
        Assert.assertEquals("FriendlyName field is not updated",  "updated2", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());
        
        // role update test revert it back to Administrator
        updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedAccountSid, "updated3", "Restcomm2", null, "Administrator", "active" );
         getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);
         
         Assert.assertEquals("FriendlyName field is not updated",  "updated3", updateAccountResponse.get("friendly_name").getAsString());
         Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
         Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
         Assert.assertEquals("Role field is not updated", "Administrator", updateAccountResponse.get("role").getAsString());
         
        Assert.assertEquals("FriendlyName field is not updated",  "updated3", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Administrator", getAccountResponse.get("role").getAsString());
    }

    @Test
    public void testUpdateAuthTokenIgnored() {
        // try to update directly the AuthToken
        JsonObject updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, childSid, null, null, "RestComm12", null, null );
        // make sure the AuthToken has not changed
        Assert.assertEquals("77f8c12cc7b8f8423e5c38b035249166", updateAccountResponse.get("auth_token").getAsString());
    }

    @Test
    public void testUpdateAccountStatusAllCapital() {
        JsonObject updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedAccountSid, "updated2", "Restcomm2", null, "Developer", "ACTIVE" );
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);

        Assert.assertEquals("FriendlyName field is not updated",  "updated2", updateAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());

        Assert.assertEquals("FriendlyName field is not updated",  "updated2", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());

        // role update test revert it back to Administrator
        updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedAccountSid, "updated3", "Restcomm2", null, "Administrator", "active" );
        getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);

        Assert.assertEquals("FriendlyName field is not updated",  "updated3", updateAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Administrator", updateAccountResponse.get("role").getAsString());

        Assert.assertEquals("FriendlyName field is not updated",  "updated3", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Administrator", getAccountResponse.get("role").getAsString());
    }

    @Test
    public void testUpdateAccountByEmail() {
        JsonObject updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername, "updated2", "Restcomm2", null, "Developer", "active" );
        JsonObject getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);

        Assert.assertEquals("FriendlyName field is not updated",  "updated2", updateAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());

        Assert.assertEquals("FriendlyName field is not updated",  "updated2", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Developer", updateAccountResponse.get("role").getAsString());

        // role update test revert it back to Administrator
        updateAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedAccountSid, "updated3", "Restcomm2", null, "Administrator", "active" );
        getAccountResponse = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedUsername);

        Assert.assertEquals("FriendlyName field is not updated",  "updated3", updateAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), updateAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", updateAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Administrator", updateAccountResponse.get("role").getAsString());

        Assert.assertEquals("FriendlyName field is not updated",  "updated3", getAccountResponse.get("friendly_name").getAsString());
        Assert.assertEquals("AuthToken field is not updated", new Md5Hash("Restcomm2").toString(), getAccountResponse.get("auth_token").getAsString());
        Assert.assertEquals("Status field is not updated", "active", getAccountResponse.get("status").getAsString());
        Assert.assertEquals("Role field is not updated", "Administrator", getAccountResponse.get("role").getAsString());
    }

    // special account-update policy when updating roles
    @Test public void testUpdateAccountRoleAccessControl() {
        // non-admins should not be able to change their role
        ClientResponse response = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(),
                updatedRoleUsername, updateRoleAuthToken, updatedRoleAccountSid, null, null, null, "Administrator", null );
        Assert.assertEquals("Should return a 403 when non-admin tries to update role", 403, response.getStatus() );
        // admin, should be able to change their sub-account role
        response = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(),
                adminUsername, adminAuthToken, updatedRoleAccountSid, null, null, null, "Administrator", null );
        Assert.assertEquals("Should return a 200 when admin tries to update role of sub-account", 200, response.getStatus() );
        // admin, should not be able to change role of other account that is not their sub-account
        response = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(),
                adminUsername, adminAuthToken, nonSubAccountSid, null, null, null, "Administrator", null );
        Assert.assertEquals("Should get a 403 when admin tries to modify role of non-sub-accounts", 403, response.getStatus() );

    }

    @Test public void testCreateAccountAccess(){
        // 'unprivilaged should not be able to create accounts and receive a 403
        ClientResponse response = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(),
                unprivilegedUsername, unprivilegedAuthToken, "notcreated@company.com", "not-created-password");
        Assert.assertEquals("403 not returned", 403, response.getStatus());
    }

    @Test
    public void testCreateAdministratorAccountFails() {
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                adminUsername, adminAuthToken, "administrator@company.com", "RestComm12");
        assertNull(createAccountResponse);
    }

    @Test
    public void testCreateAccountTwiceFails() {
        ClientResponse createResponse1 = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(), adminUsername, adminAuthToken,
                "twice@company.com", "RestComm12");
        Assert.assertEquals("Account twice@company.com could not be created even once", 200, createResponse1.getStatus());
        ClientResponse createResponse2 = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(), adminUsername, adminAuthToken,
                "twice@company.com", "RestComm12");
        Assert.assertEquals("Did not retrieve a conflict HTTP status (409) while creating accounts with same email address", 409, createResponse2.getStatus());
    }

    /**
     * When creating a new account and the client with the same name is already there
     * TODO [...]
     *
     * @throws Exception
     */
    @Test
    public void testCreateAccountCheckClientExisted() throws Exception {
        try {
            String thinhContact = "sip:lyhungthinh@127.0.0.1:5090";
            startSipStack(thinhContact);

            String subAccountPassword = "mynewpassword12";
            String subAccountEmail = "lyhungthinh@gmail.com";

            JsonObject subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(),
                    adminUsername, adminAuthToken, subAccountEmail, subAccountPassword);

            JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            assertNotNull(clientOfAccount);

            CreateClientsTool.getInstance().updateClientVoiceUrl(deploymentUrl.toString(), subAccountResponse,
                    clientOfAccount.get("sid").getAsString(), "http://127.0.0.1:8080/restcomm/demos/welcome.xml",
                    adminUsername, adminPassword);

            JsonObject clientOfAccountUpdated = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            System.out.println(clientOfAccountUpdated);

            // Use the new client to register with Restcomm
            SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

            assertTrue(thinhPhone.register(reqUri, "lyhungthinh", subAccountPassword, thinhContact, 1800, 1800));
            assertTrue(thinhPhone.unregister(thinhContact, 0));

            //RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
            //        subAccountResponse.get("sid").getAsString());
        } finally {
            stopSipStack();
        }
    }

    @Test
    public void testCreateAccountFourthLevelFails() {
        ClientResponse response = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(), "grandchild@company.com", commonAuthToken, "fourthlevelAccount@company.com", "RestComm12");
        Assert.assertEquals(400, response.getStatus());
        //Assert.assertTrue(response.getEntity(String.class).contains(""))
    }

    @Test
    public void testUpdateAccountCheckClient() throws Exception {
        try {
            String thinhContact = "sip:lyhungthinh2@127.0.0.1:5090";
            startSipStack(thinhContact);

            String subAccountPassword = "mynewpassword12";
            String subAccountEmail = "lyhungthinh2@gmail.com";
            String subAccountNewPassword = "latestpassword12";
            String subAccountNewAuthToken = "81eb94645c485fc39eedc5d46522e5ba";
            JsonObject subAccountResponse;

            SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

            subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername,
                    adminAuthToken, subAccountEmail, subAccountPassword);
            assertNotNull(subAccountResponse);
            JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            assertNotNull(clientOfAccount);
            // Use the new client to register with Restcomm
            assertTrue(thinhPhone.register(reqUri, "lyhungthinh2", subAccountPassword, thinhContact, 1800, 1800));
            assertTrue(thinhPhone.unregister(thinhContact, 0));

            subAccountResponse = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminUsername,
                    adminAuthToken,subAccountResponse.get("sid").getAsString(), null, subAccountNewPassword, null, null, null);
            assertTrue(subAccountResponse.get("auth_token").getAsString().equals(subAccountNewAuthToken));
            assertTrue(thinhPhone.register(reqUri, "lyhungthinh2", subAccountNewPassword, thinhContact, 1800, 1800));
            assertTrue(thinhPhone.unregister(thinhContact, 0));

            clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            assertTrue(clientOfAccount.get("password").getAsString().equals(subAccountNewPassword));

            //RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
            //        subAccountResponse.get("sid").getAsString());
        } finally {
            stopSipStack();
        }
    }
    
    @Test
    public void testCloseAccountCheckClient() throws Exception {
        try {
            String thinhContact = "sip:lyhungthinh3@127.0.0.1:5090";
            startSipStack(thinhContact);

            String subAccountPassword = "mynewpassword12";
            String subAccountEmail = "lyhungthinh3@gmail.com";
            JsonObject subAccountResponse;

            SipURI reqUri = thinhSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

            subAccountResponse = RestcommAccountsTool.getInstance().createAccount(deploymentUrl.toString(), adminUsername,
                    adminAuthToken, subAccountEmail, subAccountPassword);
            assertNotNull(subAccountResponse);
            JsonObject clientOfAccount = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            assertNotNull(clientOfAccount);
            assertTrue(thinhPhone.register(reqUri, "lyhungthinh3", subAccountPassword, thinhContact, 1800, 1800));
            assertTrue(thinhPhone.unregister(thinhContact, 0));
            // close the account
            Client jersey = getClient(adminUsername, adminPassword);
            WebResource resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts.json/"+subAccountEmail) );
            MultivaluedMap<String,String> params = new MultivaluedMapImpl();
            params.add("Status","closed");
            ClientResponse response = resource.put(ClientResponse.class,params);
            Assert.assertEquals(200, response.getStatus());
            //RestcommAccountsTool.getInstance().removeAccount(deploymentUrl.toString(), adminUsername, adminAuthToken,
            //        subAccountResponse.get("sid").getAsString());
            // and make sure the client is gone and can't register
            JsonObject clientOfAccount2 = CreateClientsTool.getInstance().getClientOfAccount(deploymentUrl.toString(),
                    subAccountResponse, adminUsername, adminPassword);
            assertTrue(clientOfAccount2 == null);
            assertFalse(thinhPhone.register(reqUri, "lyhungthinh3", subAccountPassword, thinhContact, 1800, 1800));
        } finally {
            stopSipStack();
        }
    }

    @Test
    public void testCloseAccountNested() {
        String topLevelSid = "AC12300000000000000000000000000000";
        String removed1Sid = "AC12300000000000000000000000000001";
        String removed11Sid = "AC12300000000000000000000000000011";

        Client jersey = getClient("removed-top@company.com", commonAuthToken);

        WebResource resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts.json/"+removed1Sid) );
        MultivaluedMap<String,String> params = new MultivaluedMapImpl();
        params.add("Status","closed");
        ClientResponse response = resource.put(ClientResponse.class,params);
        // the closed account should be available
        Assert.assertEquals(200, response.getStatus());
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response.getEntity(String.class)).getAsJsonObject();
        // make sure the account status is set to closed
        Assert.assertEquals("closed", jsonObject.get("status").getAsString());
        // assert removed accounts children are closed too
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts.json/"+removed11Sid) );
        response = resource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        parser = new JsonParser();
        jsonObject = parser.parse(response.getEntity(String.class)).getAsJsonObject();
        Assert.assertEquals("closed", jsonObject.get("status").getAsString());
        // assert the applications of the account are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/Applications/AP00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert IncomingPhoneNumbers of the account are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/IncomingPhoneNumbers/PN00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert notification are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/Notifications/NO00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert recordings are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/Recordings/RE00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert transcriptions are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/Transcriptions/TR00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert outgoing caller ids are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/OutgoingCallerIds/PN00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // assert clients are removed
        resource = jersey.resource( getResourceUrl("/2012-04-24/Accounts/"+removed1Sid+"/Clients/CL00000000000000000000000000000001.json" ) );
        Assert.assertEquals(404, resource.get(ClientResponse.class).getStatus());
        // we won't test Announcements removal . There is no retrieval method yet.
    }

    @Test
    public void testGetAccounts() throws InterruptedException {
        // Get Account using admin email address and user email address
        JsonObject account1 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminUsername,
                adminAuthToken, childUsername);
        // Get Account using admin account sid and user sid
        JsonObject account2 = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, account1.get("sid").getAsString());

        assertTrue(account1.toString().equals(account2.toString()));
    }

    @Test
    public void testGetAccountsAccess() {
        ClientResponse response = RestcommAccountsTool.getInstance().getAccountsResponse(deploymentUrl.toString(), guestUsername, guestAuthToken);
        Assert.assertEquals("Guest account should get get a 403 when retrieving accounts", 403, response.getStatus());
    }

    @Ignore // ignored since account removal is disables as of #1270
    @Test
    public void testRemoveAccountAccess(){
        ClientResponse response = RestcommAccountsTool.getInstance().removeAccountResponse(deploymentUrl.toString(), unprivilegedUsername, unprivilegedAuthToken, removedSid + ".json" );
        Assert.assertEquals("Unprivileged account should receive a 403 while removing an account", 403, response.getStatus());
        response = RestcommAccountsTool.getInstance().removeAccountResponse(deploymentUrl.toString(), adminUsername, adminAuthToken, removedSid + ".json");
        Assert.assertEquals("Administrator should receive a 200 OK when removing an account", 200, response.getStatus());
    }

    /**
     * Test access without administrator type of access. Instead a common role and permissions is used.
     */
    @Test
    public void testAccountAcccessUsingRoles() {
        ClientResponse response = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(),unprivilegedUsername, unprivilegedAuthToken, unprivilegedSid);
        Assert.assertEquals(200,response.getStatus());
    }

    @Test
    public void testPasswordStrengthForAccountCreate() {
        ClientResponse response = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(),
                adminUsername, adminAuthToken, "weak@company.com", "1234");
        Assert.assertEquals("Weak password account creation should fail with 400", 400, response.getStatus());
        Assert.assertTrue("Response error message should contain 'weak' term", response.getEntity(String.class).toLowerCase().contains("weak"));
        response = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(),
                adminUsername, adminAuthToken, "weak@company.com", "1234asdf!@#");
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPasswordStrengthForAccountUpdate() {
        // updating an account with weak password should fail
        ClientResponse response = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(), adminUsername, adminAuthToken, "updated-weak@company.com", null, "1234", null, null, null );
        Assert.assertEquals(400, response.getStatus());
        Assert.assertTrue("Response should contain 'weak' term", response.getEntity(String.class).toLowerCase().contains("weak"));
        // updating an account with strong password should succeed
        response = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(), adminUsername, adminAuthToken, "updated-weak@company.com", null, "RestComm12", null, null, null );
        Assert.assertEquals(200, response.getStatus());
    }

    @Deployment(name = "ClientsEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
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

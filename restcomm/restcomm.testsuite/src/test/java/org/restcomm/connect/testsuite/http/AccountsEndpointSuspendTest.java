/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.testsuite.http;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.dao.entities.Account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import junit.framework.Assert;

/**
 * @author ddh.huy@gmail.com - Huy Dang
 */
@RunWith(Arquillian.class)
public class AccountsEndpointSuspendTest extends EndpointTest {

    private static class TestAccount {
        private String Username;
        private String Password;
        private String FriendlyName;
        private String AuthToken;
        private String Sid;

        public TestAccount ( String username, String password, String friendlyName, String authToken, String sid ) {
            this.setUsername(username);
            this.setPassword(password);
            this.setFriendlyName(friendlyName);
            this.setAuthToken(authToken);
            this.setSid(sid);
        }

        public String getUsername ( ) {
            return Username;
        }

        public void setUsername ( String username ) {
            Username = username;
        }

        public String getPassword ( ) {
            return Password;
        }

        public void setPassword ( String password ) {
            Password = password;
        }

        public String getFriendlyName ( ) {
            return FriendlyName;
        }

        public void setFriendlyName ( String friendlyName ) {
            FriendlyName = friendlyName;
        }

        public String getAuthToken ( ) {
            return AuthToken;
        }

        public void setAuthToken ( String authToken ) {
            AuthToken = authToken;
        }

        public String getSid ( ) {
            return Sid;
        }

        public void setSid ( String sid ) {
            Sid = sid;
        }

    }

    @ArquillianResource
    URL deploymentUrl;

    private final static Logger logger = Logger.getLogger(AccountsEndpointSuspendTest.class);

    /*
     * Accounts tree for testing: administrator |-- SolarSystem |-- Mercury |--
     * Venus |-- Earth |-- Mars
     */
    private static String adminUsername = "administrator@company.com";
    private static String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private static String adminSid = "ACae6e420f425248d6a26948c17a9e2acf";
    /* Parent accounts */
    private static TestAccount parentAccount;
    /* Child accounts */
    private static List<TestAccount> subAccounts;
    /* Applications */
    private static String applicationSid = "AP00000000000000000000000000000001";

    @BeforeClass
    public static void createAccountsTree ( ) {
        // create parent account
        parentAccount = new TestAccount("solarsystem@email.com", "abcd@1234", "Solar System",
                                        "e0a8aa81eb1762d529783cf587f6f422", "AC12300000000000000000000000000217");
        // create sub-accounts
        subAccounts = new ArrayList<TestAccount>();
        subAccounts.add(new TestAccount("mercury@solar.system", "abcd@1234", "Mercury planet",
                                        "e0a8aa81eb1762d529783cf587f6f422", "AC12300000000000000000000000000130"));
        subAccounts.add(new TestAccount("venus@solar.system", "abcd@1234", "Venus planet",
                                        "e0a8aa81eb1762d529783cf587f6f422", "AC12300000000000000000000000000131"));
        subAccounts.add(new TestAccount("earth@solar.system", "abcd@1234", "Earth planet",
                                        "e0a8aa81eb1762d529783cf587f6f422", "AC12300000000000000000000000000132"));
        subAccounts.add(new TestAccount("mars@solar.system", "abcd@1234", "Mars planet",
                                        "e0a8aa81eb1762d529783cf587f6f422", "AC12300000000000000000000000000133"));
    }

    ClientResponse clientResponse;
    JsonObject accountJson;

    private JsonObject getAsJsonObject ( ClientResponse clientResponse ) {
        JsonParser parser = new JsonParser();
        JsonObject object = null;
        try {
            object = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        } catch (Exception ex) {
            logger.info(ex);
        }
        return object;
    }

    private JsonArray getAsJsonArray ( ClientResponse clientResponse ) {
        JsonParser parser = new JsonParser();
        JsonArray array = null;
        try {
            array = parser.parse(clientResponse.getEntity(String.class)).getAsJsonArray();
        } catch (Exception ex) {
            logger.info("getAsJsonArray: ");
            logger.info(ex);
        }
        return array;
    }

    private JsonObject testGetAccount ( String operatorUsername, String operatorAuthToken, String accountUsername,
                                        int expectedHttpCode ) {
        clientResponse = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(),
                                                                               operatorUsername, operatorAuthToken,
                                                                               accountUsername);
        logger.info("[AccountsEndpointSuspendTest] testGetAccount(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testStatus ( String operatorUsername, String operatorAuthToken, String accountUsername,
                                    String expectedStatus ) {
        accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), operatorUsername,
                                                                    operatorAuthToken, accountUsername);
        logger.info("[AccountsEndpointSuspendTest] testStatus: " + accountJson.get("status").getAsString());
        Assert.assertEquals(expectedStatus, accountJson.get("status").getAsString());
        return accountJson;
    }

    private JsonObject testUpdateStatus ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                          String newStatus ) {
        accountJson = RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), operatorUsername,
                                                                       operatorAuthToken, operatorSid, null, null, null,
                                                                       null, newStatus);
        logger.info("[AccountsEndpointSuspendTest] testUpdateStatus: " + accountJson.toString());
        Assert.assertEquals(operatorSid, accountJson.get("sid").getAsString());
        Assert.assertEquals(newStatus, accountJson.get("status").getAsString());
        return accountJson;
    }

    private JsonObject testCreateAccount ( String operatorUsername, String operatorAuthToken, String accountUsername,
                                           String accountPassword, int expectedHttpCode ) {
        clientResponse = RestcommAccountsTool.getInstance().createAccountResponse(deploymentUrl.toString(),
                                                                                  operatorUsername, operatorAuthToken,
                                                                                  accountUsername, accountPassword);
        logger.info("[AccountsEndpointSuspendTest] testCreateAccount(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testUpdateAccount ( String operatorUsername, String operatorAuthToken, String accountUsername,
                                           String accountPassword, String accountAuthToken, String accountFriendlyName,
                                           String accountRole, String accountStatus, int expectedHttpCode ) {
        clientResponse = RestcommAccountsTool.getInstance().updateAccountResponse(deploymentUrl.toString(),
                                                                                  operatorUsername, operatorAuthToken,
                                                                                  accountUsername, accountFriendlyName,
                                                                                  accountPassword, accountAuthToken,
                                                                                  accountRole, accountStatus);
        logger.info("[AccountsEndpointSuspendTest] testUpdateAccount(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonArray testGetApplications ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                             int expectedHttpCode ) {
        clientResponse = RestcommApplicationsTool.getInstance().getApplicationsResponse(deploymentUrl.toString(),
                                                                                        operatorUsername,
                                                                                        operatorAuthToken, operatorSid);
        logger.info("[AccountsEndpointSuspendTest] testGetApplications(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonArray(clientResponse);
    }

    private JsonObject testCreateApplication ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                               int expectedHttpCode ) {
        MultivaluedMap<String, String> applicationParams = new MultivaluedMapImpl();
        applicationParams.add("FriendlyName", "APPCreateGet");
        applicationParams.add("VoiceCallerIdLookup", "true");
        applicationParams.add("RcmlUrl", "/restcomm/rcmlurl/test");
        applicationParams.add("Kind", "voice");
        clientResponse = RestcommApplicationsTool.getInstance().createApplicationResponse(deploymentUrl.toString(),
                                                                                          operatorSid, operatorUsername,
                                                                                          operatorAuthToken,
                                                                                          applicationParams);
        logger.info("[AccountsEndpointSuspendTest] testCreateApplication(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testUpdateApplication ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                               String applicationSid, int expectedHttpCode ) {
        MultivaluedMap<String, String> applicationParamsUpdate = new MultivaluedMapImpl();
        applicationParamsUpdate.add("FriendlyName", "APPUpdate2");
        applicationParamsUpdate.add("VoiceCallerIdLookup", "false");
        applicationParamsUpdate.add("RcmlUrl", "/restcomm/rcmlurl/test2");
        applicationParamsUpdate.add("Kind", "voice");
        clientResponse = RestcommApplicationsTool.getInstance()
                                                 .updateApplicationResponse(deploymentUrl.toString(), operatorUsername,
                                                                            operatorAuthToken, operatorSid,
                                                                            applicationSid, applicationParamsUpdate,
                                                                            false);
        logger.info("[AccountsEndpointSuspendTest] testUpdateApplication(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testDeleteApplication ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                               String applicationSid, int expectedHttpCode ) throws IOException {
        clientResponse = RestcommApplicationsTool.getInstance().deleteApplicationResponse(deploymentUrl.toString(),
                                                                                          operatorUsername,
                                                                                          operatorAuthToken,
                                                                                          operatorSid, applicationSid);
        logger.info("[AccountsEndpointSuspendTest] testDeleteApplication(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonArray testGetClients ( String operatorUsername, String operatorAuthToken, int expectedHttpCode ) {
        clientResponse = CreateClientsTool.getInstance().getClientsResponse(deploymentUrl.toString(), accountJson);
        logger.info("[AccountsEndpointSuspendTest] testGetClients(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonArray(clientResponse);
    }

    private JsonObject testCreateClient ( String operatorUsername, String operatorAuthToken, String operatorSid, 
                                      String clientUsername, String clientPassword, String clientVoiceUrl,
                                      int expectedHttpCode ) {
        clientResponse = CreateClientsTool.getInstance().createClientResponse(deploymentUrl.toString(),
                                                                              operatorUsername, operatorAuthToken,
                                                                              operatorSid, clientUsername,
                                                                              clientPassword, clientVoiceUrl);
        logger.info("[AccountsEndpointSuspendTest] testCreateClient(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testUpdateClient ( String operatorUsername, String operatorAuthToken, String operatorSid, 
                                          String clientSid, String clientPassword, String clientVoiceUrl,
                                          int expectedHttpCode ) {
            clientResponse = CreateClientsTool.getInstance().updateClientResponse(deploymentUrl.toString(),
                                                                                  operatorUsername, operatorAuthToken, operatorSid,
                                                                                  clientSid, clientPassword, clientVoiceUrl);
            logger.info("[AccountsEndpointSuspendTest] testUpdateClient(): " + clientResponse.getStatus());
            Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
            return this.getAsJsonObject(clientResponse);
        }

    private JsonObject testDeleteClient ( String operatorUsername, String operatorAuthToken, String operatorSid,
                                          String clientSid, int expectedHttpCode ) {
        clientResponse = CreateClientsTool.getInstance().deleteClientResponse(deploymentUrl.toString(),
                                                                              operatorUsername, operatorAuthToken,
                                                                              operatorSid, clientSid);
        logger.info("[AccountsEndpointSuspendTest] testDeleteClient(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testGetNotificationList ( String operatorUsername, String operatorAuthToken,
                                                 int expectedHttpCode ) {
        clientResponse = NotificationEndpointTool.getInstance().getNotificationListResponse(deploymentUrl.toString(),
                                                                                            operatorUsername,
                                                                                            operatorAuthToken);
        logger.info("[AccountsEndpointSuspendTest] testGetNotificationList(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testGetRecordingList ( String operatorUsername, String operatorAuthToken,
                                              int expectedHttpCode ) {
        clientResponse = RecordingEndpointTool.getInstance().getRecordingListResponse(deploymentUrl.toString(),
                                                                                      operatorUsername,
                                                                                      operatorAuthToken);
        logger.info("[AccountsEndpointSuspendTest] testGetRecordingList(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }

    private JsonObject testGetTranscriptionList ( String operatorUsername, String operatorAuthToken,
                                                  int expectedHttpCode ) {
        clientResponse = TranscriptionEndpointTool.getInstance().getTranscriptionListResponse(deploymentUrl.toString(),
                                                                                              operatorUsername,
                                                                                              operatorAuthToken);
        logger.info("[AccountsEndpointSuspendTest] testGetTranscriptionList(): " + clientResponse.getStatus());
        Assert.assertTrue(expectedHttpCode == clientResponse.getStatus());
        return this.getAsJsonObject(clientResponse);
    }
    
    private JsonArray testGetIncomingPhoneNumbers ( String operatorUsername, String operatorAuthToken,
                                                     int expectedHttpCode ) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthToken));
        String url = getResourceUrl("/2012-04-24/Accounts/" + operatorUsername + "/IncomingPhoneNumbers");
        WebResource webResource = jerseyClient.resource(url);
        clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        logger.info("[AccountsEndpointSuspendTest] testGetIncomingPhoneNumbers(): " + clientResponse.getStatus());
        return this.getAsJsonArray(clientResponse);
    }

    private JsonArray testGetOutgoingCallerIds ( String operatorUsername, String operatorAuthToken,
                                                     int expectedHttpCode ) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthToken));
        String url = getResourceUrl("/2012-04-24/Accounts/" + operatorUsername + "/OutgoingCallerIds");
        WebResource webResource = jerseyClient.resource(url);
        clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        logger.info("[AccountsEndpointSuspendTest] testGetOutgoingCallerIds(): " + clientResponse.getStatus());
        return this.getAsJsonArray(clientResponse);
    }
    @Test
    public void testSuspendAccount ( ) throws IOException {
        // test an account can be "suspended" or not
        this.testUpdateStatus(adminUsername, adminAuthToken, parentAccount.getSid(), Account.Status.SUSPENDED.toString());
        this.testStatus(adminUsername, adminAuthToken, parentAccount.getSid(), Account.Status.SUSPENDED.toString());

        /***
         * No REST API request is allowed for a SUSPENDED account. Account cannot login,
         * create calls, search DID etc.
         */
        this.testCreateAccount(parentAccount.getUsername(), parentAccount.getAuthToken(),
                               UUID.randomUUID().toString().replace("-", "") + "@email.com", "5tr0n9P@ssw0rd", 403);
        this.testGetAccount(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(), 403);
        this.testUpdateAccount(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getUsername(),
                               parentAccount.getPassword(), parentAccount.getAuthToken(),
                               parentAccount.getFriendlyName(), "administrator", Account.Status.ACTIVE.toString(), 403);
        this.testCreateApplication(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                   403);
        this.testGetApplications(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                 403);
        this.testUpdateApplication(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                   applicationSid, 403);
        this.testDeleteApplication(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                   applicationSid, 403);
        this.testCreateClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                              "any_name", "5tr0n9P@ssw0rd", null, 403);
        this.testGetClients(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);
        this.testUpdateClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                              "any_name", "NEW5tr0n9P@ssw0rd", null, 403);
        this.testDeleteClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                              "any_name", 403);
        this.testGetNotificationList(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);
        this.testGetOutgoingCallerIds(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);
        this.testGetTranscriptionList(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);
        this.testGetRecordingList(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);
        this.testGetIncomingPhoneNumbers(parentAccount.getUsername(), parentAccount.getAuthToken(), 403);

        /***
         * Check if all sub-accounts of suspended account cannot log-in, access
         * resource...
         */
        for (TestAccount account : subAccounts) {
            this.testCreateAccount(account.getUsername(), account.getAuthToken(),
                                   UUID.randomUUID().toString().replace("-", "") + "@email.com", "5tr0n9P@ssw0rd", 403);
            this.testGetAccount(account.getUsername(), account.getAuthToken(), account.getSid(), 403);
            this.testUpdateAccount(account.getUsername(), account.getAuthToken(), null, null, null, "new friendly name",
                                   null, Account.Status.ACTIVE.toString(), 403);
            this.testCreateApplication(account.getUsername(), account.getAuthToken(), account.getSid(), 403);
            this.testGetApplications(account.getUsername(), account.getAuthToken(), account.getSid(), 403);
            this.testUpdateApplication(account.getUsername(), account.getAuthToken(), account.getSid(), "Sid", 403);
            this.testDeleteApplication(account.getUsername(), account.getAuthToken(), account.getSid(), "Sid", 403);
            this.testCreateClient(account.getUsername(), account.getAuthToken(), account.getSid(), "any_name",
                                  "5tr0n9P@ssw0rd", null, 403);
            this.testGetClients(account.getUsername(), account.getAuthToken(), 403);
            this.testUpdateClient(account.getUsername(), account.getAuthToken(), account.getSid(), "any_name",
                                  "NEW5tr0n9P@ssw0rd", null, 403);
            this.testDeleteClient(account.getUsername(), account.getAuthToken(), account.getSid(), "any_name", 403);
            this.testGetNotificationList(account.getUsername(), account.getAuthToken(), 403);
            this.testGetOutgoingCallerIds(account.getUsername(), account.getAuthToken(), 403);
            this.testGetTranscriptionList(account.getUsername(), account.getAuthToken(), 403);
            this.testGetRecordingList(account.getUsername(), account.getAuthToken(), 403);
            this.testGetIncomingPhoneNumbers(account.getUsername(), account.getAuthToken(), 403);
        }
    }

    @Test
    public void testActivateAccount ( ) throws IOException {
        JsonObject jsonObj = null;

        // suspend account first then activate it
        this.testUpdateStatus(adminUsername, adminAuthToken, parentAccount.getSid(), Account.Status.SUSPENDED.toString());
        this.testUpdateStatus(adminUsername, adminAuthToken, parentAccount.getSid(), Account.Status.ACTIVE.toString());
        this.testStatus(adminUsername, adminAuthToken, parentAccount.getSid(), Account.Status.ACTIVE.toString());
        /***
         * Moving to ACTIVE state means that Account and sub-accounts are active and can
         * be used again, same for all related resources
         */
        this.testCreateAccount(parentAccount.getUsername(), parentAccount.getAuthToken(),
                               UUID.randomUUID().toString().replace("-", "") + "@email.com", "5tr0n9P@ssw0rd", 200);
        this.testGetAccount(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(), 200);
        this.testUpdateAccount(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getUsername(),
                               null, null, parentAccount.getFriendlyName() + " New", null, null, 200);
        jsonObj = this.testCreateApplication(parentAccount.getUsername(), parentAccount.getAuthToken(),
                                             parentAccount.getSid(), 200);
        this.testGetApplications(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                 200);
        this.testUpdateApplication(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                   jsonObj.get("sid").getAsString(), 200);
        this.testDeleteApplication(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                   jsonObj.get("sid").getAsString(), 200);
        jsonObj = this.testCreateClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                                        "client01", "5tr0n9P@ssw0rd", null, 200);
        this.testGetClients(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);
        this.testUpdateClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                              jsonObj.get("sid").getAsString(), "NEW5tr0n9P@ssw0rd", null, 200);
        this.testDeleteClient(parentAccount.getUsername(), parentAccount.getAuthToken(), parentAccount.getSid(),
                              jsonObj.get("sid").getAsString(), 200);
        this.testGetNotificationList(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);
        this.testGetOutgoingCallerIds(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);
        this.testGetTranscriptionList(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);
        this.testGetRecordingList(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);
        this.testGetIncomingPhoneNumbers(parentAccount.getUsername(), parentAccount.getAuthToken(), 200);

        /***
         * Check if all sub-accounts are back to normal operations
         */
        for (TestAccount account : subAccounts) {
//            this.testCreateAccount(account.getUsername(), account.getAuthToken(),
//                                   UUID.randomUUID().toString().replace("-", "") + "@email.com", "5tr0n9P@ssw0rd", 200);
            this.testGetAccount(account.getUsername(), account.getAuthToken(), account.getSid(), 200);
            this.testUpdateAccount(account.getUsername(), account.getAuthToken(), account.getUsername(), null, null,
                                   "new friendly name", null, Account.Status.ACTIVE.toString(), 200);
            jsonObj = this.testCreateApplication(account.getUsername(), account.getAuthToken(), account.getSid(), 200);
            this.testGetApplications(account.getUsername(), account.getAuthToken(), account.getSid(), 200);
            this.testUpdateApplication(account.getUsername(), account.getAuthToken(), account.getSid(),
                                       jsonObj.get("sid").getAsString(), 200);
            this.testDeleteApplication(account.getUsername(), account.getAuthToken(), account.getSid(),
                                       jsonObj.get("sid").getAsString(), 200);
            jsonObj = this.testCreateClient(account.getUsername(), account.getAuthToken(), account.getSid(),
                                             UUID.randomUUID().toString().replace("-", ""), "5tr0n9P@ssw0rd", null, 200);
            this.testGetClients(account.getUsername(), account.getAuthToken(), 200);
            this.testUpdateClient(account.getUsername(), account.getAuthToken(), account.getSid(),
                                  jsonObj.get("sid").getAsString(), "NEW5tr0n9P@ssw0rd", null, 200);
            this.testDeleteClient(account.getUsername(), account.getAuthToken(), account.getSid(),
                                  jsonObj.get("sid").getAsString(), 200);
            this.testGetNotificationList(account.getUsername(), account.getAuthToken(), 200);
            this.testGetOutgoingCallerIds(account.getUsername(), account.getAuthToken(), 200);
            this.testGetTranscriptionList(account.getUsername(), account.getAuthToken(), 200);
            this.testGetRecordingList(account.getUsername(), account.getAuthToken(), 200);
            this.testGetIncomingPhoneNumbers(account.getUsername(), account.getAuthToken(), 200);

        }
    }

    @Deployment(name = "AccountEndpointSuspendTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw ( ) {
        logger.info("Packaging Test App version " + version);
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                                                          .resolve("org.restcomm:restcomm-connect.application:war:"
                                                                   + version)
                                                          .withoutTransitivity().asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_accounts_test", "data/hsql/restcomm.script");
        logger.info("AccountEndpointSuspendTest created");
        return archive;
    }
}

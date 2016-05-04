package org.mobicents.servlet.restcomm.telephony;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.http.CreateClientsTool;
import org.mobicents.servlet.restcomm.http.RestcommCallsTool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mobicents.servlet.restcomm.provisioning.number.vi.IncomingPhoneNumbersEndpointTestUtils;
import org.mobicents.servlet.restcomm.tools.MonitoringServiceTool;

/**
 * Test for clients with or without VoiceURL (Bitbucket issue 115). Clients without VoiceURL can dial anything.
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class ClientsDialTest {

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
        53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
        48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
        13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
        86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    static boolean accountUpdated = false;
    
    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;
    private static SipStackTool tool5;
    private static SipStackTool tool6;
    private static SipStackTool tool7;

    private String pstnNumber = "+151261006100";

    // Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack receiverSipStack;
    private SipStack mariaSipStack;
    private SipPhone mariaPhone;
    private String mariaContact = "sip:maria@127.0.0.1:5092";
    private String mariaRestcommClientSid;

    // Dimitris is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack dimitriSipStack;
    private SipPhone dimitriPhone;
    private String dimitriContact = "sip:dimitri@127.0.0.1:5093";
    private String dimitriRestcommClientSid;

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    private SipStack aliceSipStack2;
    private SipPhone alicePhone2;
    private String aliceContact2 = "sip:alice@127.0.0.1:5094";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:"+pstnNumber+"@127.0.0.1:5070";

    private SipStack clientWithAppSipStack;
    private SipPhone clientWithAppPhone;
    private String clientWithAppContact = "sip:clientWithApp@127.0.0.1:5095";
    private String clientWithAppClientSid;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String adminUsername = "administrator@company.com";
    private String baseURL = "2012-04-24/Accounts/" + adminAccountSid + "/";
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ClientsDialTest1");
        tool2 = new SipStackTool("ClientsDialTest2");
        tool3 = new SipStackTool("ClientsDialTest3");
        tool4 = new SipStackTool("ClientsDialTest4");
        tool5 = new SipStackTool("ClientsDialTest5");
        tool6 = new SipStackTool("ClientsDialTest6");
        tool7 = new SipStackTool("ClientsDialTest7");
    }

    @Before
    public void before() throws Exception {

        aliceSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        aliceSipStack2 = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
        alicePhone2 = aliceSipStack2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact2);

        mariaSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContact);

        dimitriSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5093", "127.0.0.1:5080");
        dimitriPhone = dimitriSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, dimitriContact);

        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);

        clientWithAppSipStack = tool6.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5095", "127.0.0.1:5080");
        clientWithAppPhone = clientWithAppSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, clientWithAppContact);

        mariaRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "maria", "1234", null);
        dimitriRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "dimitri", "1234", null);
        clientWithAppClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "clientWithApp", "1234", "http://127.0.0.1:8090/1111");
        
        receiverSipStack = tool7.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5071", "127.0.0.1:5080");
    }

    @After
    public void after() throws Exception {
        if (mariaPhone != null) {
            mariaPhone.dispose();
        }
        if (mariaSipStack != null) {
            mariaSipStack.dispose();
        }

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }

        if (dimitriSipStack != null) {
            dimitriSipStack.dispose();
        }
        if (dimitriPhone != null) {
            dimitriPhone.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }
        if (clientWithAppPhone != null) {
            clientWithAppPhone.dispose();
        }
        if (clientWithAppSipStack != null) {
            clientWithAppSipStack.dispose();
        }
        if (receiverSipStack != null) {
            receiverSipStack.dispose();
        }
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
    }
 
    @Test
    public void testRegisterClients() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 3600, 3600));
        assertTrue(dimitriPhone.register(uri, "dimitri", "1234", dimitriContact, 3600, 3600));

        Thread.sleep(1000);

        assertTrue(alicePhone.unregister(aliceContact, 0));
        assertTrue(mariaPhone.unregister(mariaContact, 0));
        assertTrue(dimitriPhone.unregister(dimitriContact, 0));
    }

    @Test
    public void testClientsCallEachOther() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        //
        //        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        //        Thread.sleep(3000);
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 3600, 3600));
        Thread.sleep(3000);
        assertTrue(dimitriPhone.register(uri, "dimitri", "1234", dimitriContact, 3600, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", "1234");
        mariaPhone.addUpdateCredential(c);

        Thread.sleep(1000);

        // Maria initiates a call to Dimitri
        long startTime = System.currentTimeMillis();
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, dimitriContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        final SipCall dimitriCall = dimitriPhone.createSipCall();
        dimitriCall.listenForIncomingCall();

        // Start a new thread for Dimitri to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(dimitriCall.waitForIncomingCall(3000));
                assertTrue(dimitriCall.sendIncomingCallResponse(100, "Trying-Dimitri", 1800));
                assertTrue(dimitriCall.sendIncomingCallResponse(180, "Ringing-Dimitri", 1800));
                String receivedBody = new String(dimitriCall.getLastReceivedRequest().getRawContent());
                assertTrue(dimitriCall.sendIncomingCallResponse(Response.OK, "OK-Dimitri", 3600, receivedBody, "application", "sdp", null,
                        null));
                //                assertTrue(dimitriCall.sendIncomingCallResponse(200, "OK", 1800));
                //                assertTrue(dimitriCall.waitForAck(3000));
            }
        }).run(); //.start();

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
        }

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.getDialog().equals(mariaDialog));
        mariaCall.sendInviteOkAck();
        assertTrue(mariaCall.getDialog().equals(mariaDialog));

        assertTrue(!(mariaCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(dimitriCall.waitForAck(3000));
        
        //Talk time ~ 3sec
        Thread.sleep(3000);
        dimitriCall.listenForDisconnect();
        assertTrue(mariaCall.disconnect());

        assertTrue(dimitriCall.waitForDisconnect(5 * 1000));
        assertTrue(dimitriCall.respondToDisconnect());
        long endTime   = System.currentTimeMillis();
        
        double totalTime = (endTime - startTime)/1000.0;
        assertTrue(3.0 <= totalTime);
        assertTrue(totalTime <= 4.0);
        
        Thread.sleep(3000);

        //Check CDR
        JsonObject cdrs = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertNotNull(cdrs);
        JsonArray cdrsArray = cdrs.get("calls").getAsJsonArray();
        System.out.println("cdrsArray.size(): "+cdrsArray.size());
        assertTrue(cdrsArray.size() == 1);

    }

    @Test
    public void testClientDialOutPstn() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", "1234");
        mariaPhone.addUpdateCredential(c);

        Thread.sleep(1000);

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        georgeCall.waitForIncomingCall(5 * 1000);
        georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600);

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
        }

        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.sendInviteOkAck());

        //        For a reason the ACK will never reach Restcomm. This is only when working with the sipUnit
        //        assertTrue(georgeCall.waitForAck(5 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        assertTrue(mariaCall.disconnect());

        //        assertTrue(georgeCall.waitForDisconnect(5 * 1000));
        //        assertTrue(georgeCall.respondToDisconnect());
    }
    
    @Test
    public void testClientDialToInvalidNumber() throws ParseException, InterruptedException, InvalidArgumentException, SipException {
        String invalidNumber = "+123456789";
        SipPhone outboundProxy = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:"+invalidNumber+"@127.0.0.1:5070");
        
        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", "1234");
        mariaPhone.addUpdateCredential(c);

        Thread.sleep(1000);

        // Maria initiates a call to invalid number
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+invalidNumber+"@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        final SipCall georgeCall = outboundProxy.createSipCall();
        georgeCall.listenForIncomingCall();
        
        georgeCall.waitForIncomingCall(5 * 1000);
        georgeCall.sendIncomingCallResponse(Response.NOT_FOUND, "Not-Found George", 3600);

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.NOT_FOUND);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.NOT_FOUND, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
        }

        outboundProxy.dispose();
    }
    
    @Test
    public void testClientDialOutPstnCancelBefore200() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", "1234");
        mariaPhone.addUpdateCredential(c);

        Thread.sleep(1000);

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        georgeCall.waitForIncomingCall(5 * 1000);
        georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600);

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
        }

        SipTransaction mariaCancelTransaction = mariaCall.sendCancel();
        assertTrue(mariaCancelTransaction != null);

        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(5 * 1000);
        assertTrue(georgeCancelTransaction != null);

        georgeCall.respondToCancel(georgeCancelTransaction, 200, "OK-George", 3600);

        //        Thread.sleep(3000);
        //        georgeCall.listenForDisconnect();
        //        assertTrue(mariaCall.disconnect());

        //        assertTrue(georgeCall.waitForDisconnect(5 * 1000));
        //        assertTrue(georgeCall.respondToDisconnect());
    }

    private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client></Dial></Response>";
    @Test
    public synchronized void testDialClientAlice() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        georgeCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }


    private String dialWebRTCClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>bob</Client></Dial></Response>";
    @Test
    public synchronized void testDialClientWebRTCAliceFromAnotherInstance() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientRcml)));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        georgeCall.listenForDisconnect();

        //Restcomm checks for Bob registration, finds one but this is WebRTC=true and the registration instanceId is not the current Restcomm instance id
        assertTrue(georgeCall.waitForDisconnect(5000));
        assertTrue(georgeCall.respondToDisconnect());
    }

    private String dialWebRTCClientForkRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>bob</Client><Client>alice</Client></Dial></Response>";
    @Test
    public synchronized void testDialClientForkWithWebRTCAliceFromAnotherInstance() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientForkRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        georgeCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }

    private String dialAliceDimitriRcml= "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client><Sip>"+dimitriContact+"</Sip></Dial></Response>";
    @Test
    public synchronized void testDialForkClient_AliceMultipleRegistrations_George() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialAliceDimitriRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(alicePhone2.register(uri, "alice", "1234", aliceContact2, 3600, 3600));


        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        SipCall aliceCall2 = alicePhone2.createSipCall();
        aliceCall2.listenForIncomingCall();

        SipCall dimitriCall = dimitriPhone.createSipCall();
        dimitriCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));

        assertTrue(aliceCall2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice2", 3600));
        aliceCall2.listenForCancel();

        assertTrue(dimitriCall.waitForIncomingCall(30 * 1000));
        assertTrue(dimitriCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Dimitri", 3600));
        dimitriCall.listenForCancel();

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        SipTransaction aliceCall2CancelTransaction = aliceCall2.waitForCancel(5000);
        SipTransaction dimitriCallCancelTransaction = dimitriCall.waitForCancel(5000);
        assertNotNull(aliceCall2CancelTransaction);
        assertNotNull(dimitriCallCancelTransaction);
        aliceCall2.respondToCancel(aliceCall2CancelTransaction, 200, "OK-2-Cancel-Alice2", 3600);
        dimitriCall.respondToCancel(dimitriCallCancelTransaction, 200, "OK-2-Cancel-Dimitr", 3600);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 1);

        Thread.sleep(3000);

        // hangup.
        aliceCall.listenForDisconnect();

        georgeCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 0);
    }

    @Test
    public synchronized void testDialForkClientWebRTCBob_And_AliceWithMultipleRegistrations() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientForkRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(alicePhone2.register(uri, "alice", "1234", aliceContact2, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        SipCall aliceCall2 = alicePhone2.createSipCall();
        aliceCall2.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));


        assertTrue(aliceCall2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice2", 3600));
        aliceCall2.listenForCancel();

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        SipTransaction aliceCall2CancelTransaction = aliceCall2.waitForCancel(5000);
        assertNotNull(aliceCall2CancelTransaction);
        aliceCall2.respondToCancel(aliceCall2CancelTransaction, 200, "OK-2-Cancel-Alice2", 3600);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 1);

        Thread.sleep(3000);

        // hangup.
        aliceCall.listenForDisconnect();

        georgeCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken) == 0);
    }

    private String clientWithAppHostedAppRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Number>+151261006100</Number></Dial></Response>";
    @Test
    public synchronized void testClientWithHostedApplication() throws InterruptedException, ParseException {
        stubFor(post(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(clientWithAppHostedAppRcml)));

        assertNotNull(clientWithAppClientSid);

        SipURI uri = clientWithAppSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(clientWithAppPhone.register(uri, "clientWithApp", "1234", clientWithAppContact, 3600, 3600));
        Credential c = new Credential("127.0.0.1", "clientWithApp", "1234");
        clientWithAppPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        SipCall clientWithAppCall = clientWithAppPhone.createSipCall();
        clientWithAppCall.initiateOutgoingCall(clientWithAppContact, "sip:3090909090@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(clientWithAppCall);
        assertTrue(clientWithAppCall.waitForAuthorisation(5000));
        assertTrue(clientWithAppCall.waitOutgoingCallResponse(5000));
        final int response = clientWithAppCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(clientWithAppCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, clientWithAppCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(clientWithAppCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, clientWithAppCall.getLastReceivedResponse().getStatusCode());

        clientWithAppCall.sendInviteOkAck();
        assertTrue(!(clientWithAppCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        clientWithAppCall.disconnect();

        georgeCall.listenForDisconnect();
        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        assertTrue(georgeCall.respondToDisconnect());
    }
   
    //Non regression test for issue #600.
    @Test
    public void testClientDialToNumber() throws ParseException, InterruptedException, InvalidArgumentException {
        
        stubFor(post(urlPathEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("23456789"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));

        stubFor(post(urlPathEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("23456789"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlPathEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("7654321"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));

        stubFor(post(urlPathEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("7654321"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+123456789");
        formData.add("VoiceUrl", "/restcomm/demos/hello-play.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "POST");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        int status = clientResponse.getStatus();
        
        MultivaluedMap<String, String> formData2 = new MultivaluedMapImpl();
        formData2.add("PhoneNumber", "987654321");
        formData2.add("VoiceUrl", "/restcomm/demos/hello-play.xml");
        formData2.add("FriendlyName", "My Company Line");
        formData2.add("VoiceMethod", "POST");
        ClientResponse clientResponse2 = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData2);
        int status2 = clientResponse2.getStatus();

        String testNumber1 = "123456789";
        String testNumber2 = "+987654321";
        
        SipPhone receiverSipPhone1 = receiverSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:"+testNumber1+"@127.0.0.1:5071");
        SipPhone receiverSipPhone2 = receiverSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:"+testNumber2+"@127.0.0.1:5071");
        
        assertNotNull(mariaRestcommClientSid);
        
        // Register Maria Restcomm client 
        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 14400, 3600));
        Thread.sleep(3000);
        Credential c = new Credential("127.0.0.1", "maria", "1234");
        mariaPhone.addUpdateCredential(c);
        Thread.sleep(1000);

        //Initiate first call
        SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:" + testNumber1 + "@127.0.0.1:5071", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        // Prepare first client to receive call
        SipCall testCall1 = receiverSipPhone1.createSipCall();
        testCall1.listenForIncomingCall();
        testCall1.waitForIncomingCall(5 * 1000);
        testCall1.sendIncomingCallResponse(Response.RINGING, "RINGING-Test1", 3600);

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.sendInviteOkAck());

        Thread.sleep(3000);
        receiverSipPhone1.dispose();
        assertTrue(mariaCall.disconnect());
       
        Thread.sleep(3000);

        //Initiate second call
        mariaCall.initiateOutgoingCall(mariaContact, "sip:" + testNumber2 + "@127.0.0.1:5071", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));
        
        // Prepare second client to receive call
        SipCall testCall2 = receiverSipPhone2.createSipCall();
        testCall2.listenForIncomingCall();
        testCall2.waitForIncomingCall(5 * 1000);
        testCall2.sendIncomingCallResponse(Response.RINGING, "RINGING-Test2", 3600);

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria2 = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria2 == Response.TRYING || responseMaria2 == Response.RINGING);

        if (responseMaria2 == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.sendInviteOkAck());

        Thread.sleep(3000);
        receiverSipPhone2.dispose();
        assertTrue(mariaCall.disconnect());
        
 }
    @Deployment(name = "ClientsDialTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_AvailablePhoneNumbers_Test.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-conference-entry.xml");
        archive.addAsWebResource("dial-fork-entry.xml");
        archive.addAsWebResource("dial-uri-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("dial-number-entry.xml");
        archive.addAsWebResource("hello-play.xml");
        return archive;
    }

}

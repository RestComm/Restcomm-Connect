package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;

import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import java.util.ArrayList;
import java.util.Arrays;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.WebArchiveUtil;

/**
 * Test for Dial verb. Will test Dial Conference, Dial URI, Dial Client, Dial Number and Dial Fork
 *
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author jean.deruelle@telestax.com
 */
@RunWith(Arquillian.class)
public class TestDialVerbPartTwoAnswerDelay {
    private final static Logger logger = Logger.getLogger(TestDialVerbPartTwoAnswerDelay.class.getName());

    private static final String version = Version.getVersion();
    private static final byte[] bytes = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
    private static final String body = new String(bytes);

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static int mediaPort = NetworkPortAssigner.retrieveNextPortByFile();
    
    private static int mockPort = NetworkPortAssigner.retrieveNextPortByFile();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);
    private String dialClientWithRecordingRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\" record=\"true\" action=\"http://127.0.0.1:" + mockPort + "/action\" method=\"GET\"><Client>alice</Client></Dial></Response>";
    private String dialConferenceWithDialActionRcml = "<Response><Dial action=\"http://127.0.0.1:" + mockPort + "/action\" method=\"GET\"><Conference>test</Conference></Dial></Response>";
    private String dialClientWithRecordingRcml2 = "<Response><Dial timeLimit=\"10\" timeout=\"10\" record=\"true\" action=\"http://127.0.0.1:" + mockPort + "/action&sticky_numToDial=00306986971731\" method=\"GET\"><Client>alice</Client></Dial></Response>";
    private String dialRecordWithActionRcml = "<Response><Record action=\"http://127.0.0.1:" + mockPort + "/recordAction\" method=\"GET\" finishOnKey=\"*\" maxLength=\"10\" playBeep=\"true\"/></Response>";
    private String dialClientWithActionRcml = "<Response><Dial action=\"http://127.0.0.1:" + mockPort + "/action\" method=\"GET\"><Client>alice</Client></Dial></Response>";
    private String dialTimeOutClientWithActionRcml = "<Response><Dial timeout=\"3\" action=\"http://127.0.0.1:" + mockPort + "/action\" method=\"GET\"><Client>alice</Client></Dial></Response>";
    
    
    
    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private static String bobPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String bobContact = "sip:bob@127.0.0.1:" + georgePort;

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private static String alicePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String aliceContact = "sip:alice@127.0.0.1:" + alicePort;

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private static String georgePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String georgeContact = "sip:+131313@127.0.0.1:" + georgePort;

    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;        
    private static String restcommContact = "127.0.0.1:" + restcommPort;       
    private static String dialRestcomm = "sip:1111@" + restcommContact;
    private static String dialRestcommWithStatusCallback = "sip:7777@" + restcommContact;
    private static String dialNumberNoCallerId = "<Response><Dial><Number url=\"http://127.0.0.1:" + restcommHTTPPort + "/restcomm/hello-play.xml\">131313</Number></Dial></Response>";
    private static String dialNumberRcml = "<Response><Dial callerId=\"+13055872294\"><Number url=\"http://127.0.0.1:" + restcommHTTPPort + "/restcomm/hello-play.xml\">131313</Number></Dial></Response>";
  
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialTest2Tool1");
        tool2 = new SipStackTool("DialTest2Tool2");
        tool3 = new SipStackTool("DialTest2Tool3");
    }

    public static void reconfigurePorts() { 
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort; 
            dialRestcomm = "sip:1111@" + restcommContact;   
            dialRestcommWithStatusCallback = "sip:7777@" + restcommContact;
        } 
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));
            dialNumberNoCallerId = "<Response><Dial><Number url=\"http://127.0.0.1:" + restcommHTTPPort + "/restcomm/hello-play.xml\">131313</Number></Dial></Response>";
            dialNumberRcml = "<Response><Dial callerId=\"+13055872294\"><Number url=\"http://127.0.0.1:" + restcommHTTPPort + "/restcomm/hello-play.xml\">131313</Number></Dial></Response>";
        }         
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", bobPort, restcommContact);
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort, restcommContact);
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact);

        georgeSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", georgePort, restcommContact);
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, georgeContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
    }

    private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client></Dial></Response>";
    //Test for issue RESTCOMM-617
    @Test
    public synchronized void testDialClientAliceToBigDID() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }

    private String sendSmsActionRcml = "<Response>\n" +
            "\t\t\t<Sms to=\"bob\" from=\"+12223334499\">Hello World!</Sms>\n" +
            "</Response>";
    @Test
    public synchronized void testDialClientAliceWithRecord() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientWithRecordingRcml)));

        stubFor(get(urlPathEqualTo("/action"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sendSmsActionRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(7000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        bobCall.listenForMessage();
        assertTrue(bobCall.waitForMessage(60 * 1000));
        assertTrue(bobCall.sendMessageResponse(200, "OK-Message Received", 3600));
        Request messageReceived = bobCall.getLastReceivedMessageRequest();
        assertTrue(new String(messageReceived.getRawContent()).equalsIgnoreCase("Hello World!"));

        Thread.sleep(5000);

        JsonArray recordings = RestcommCallsTool.getInstance().getRecordings(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertNotNull(recordings);
        assertTrue("7.0".equalsIgnoreCase(((JsonObject)recordings.get(0)).get("duration").getAsString()));
        assertNotNull(((JsonObject)recordings.get(0)).get("uri").getAsString());
    }

    @Test
    public synchronized void testDialClientAliceWithRecord2() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientWithRecordingRcml2)));

        stubFor(get(urlPathEqualTo("/action"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sendSmsActionRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(7000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        Thread.sleep(10000);

        bobCall.listenForMessage();
        assertTrue(bobCall.waitForMessage(60 * 1000));
        assertTrue(bobCall.sendMessageResponse(200, "OK-Message Received", 3600));
        Request messageReceived = bobCall.getLastReceivedMessageRequest();
        assertTrue(new String(messageReceived.getRawContent()).equalsIgnoreCase("Hello World!"));

        Thread.sleep(5000);

        JsonArray recordings = RestcommCallsTool.getInstance().getRecordings(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertNotNull(recordings);
        assertTrue("7.0".equalsIgnoreCase(((JsonObject)recordings.get(0)).get("duration").getAsString()));
        assertNotNull(((JsonObject)recordings.get(0)).get("uri").getAsString());
    }
    
    @Test //Test case for issue 320
    public synchronized void testDialClientAliceWithRecordAndStatusCallbackForApp() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientWithRecordingRcml)));

        stubFor(get(urlPathEqualTo("/action"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sendSmsActionRcml)));

        stubFor(get(urlPathMatching("/StatusCallBack.*"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcommWithStatusCallback, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(7000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        bobCall.listenForMessage();
        assertTrue(bobCall.waitForMessage(60 * 1000));
        assertTrue(bobCall.sendMessageResponse(200, "OK-Message Received", 3600));
        Request messageReceived = bobCall.getLastReceivedMessageRequest();
        assertTrue(new String(messageReceived.getRawContent()).equalsIgnoreCase("Hello World!"));

        Thread.sleep(5000);

        JsonArray recordings = RestcommCallsTool.getInstance().getRecordings(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertNotNull(recordings);
        assertTrue("7.0".equalsIgnoreCase(((JsonObject)recordings.get(0)).get("duration").getAsString()));
        assertNotNull(((JsonObject)recordings.get(0)).get("uri").getAsString());

        logger.info("About to check the Status Callback Requests");
        Map<String, String> statusCallbacks = new HashMap<String,String>();

        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/StatusCallBack.*")));
        assertTrue(requests.size()==3);

//        for (LoggedRequest loggedRequest : requests) {
//            String queryParam = loggedRequest.getUrl().replaceFirst("/StatusCallBack?", "");
//            String[] params = queryParam.split("&");
//            String callSid = params[0].split("=")[1];
//            String callStatus = params[4].split("=")[1];
//            if (statusCallbacks.containsKey(callSid)) {
//                statusCallbacks.remove(callSid);
//            }
//            statusCallbacks.put(callSid, callStatus);
//        }
//        assertTrue(statusCallbacks.size()==1);
//        Iterator<String> iter = statusCallbacks.keySet().iterator();
//        while (iter.hasNext()) {
//            String key = iter.next();
//            assertTrue(statusCallbacks.get(key).equalsIgnoreCase("completed"));
//        }
    }
    
    @Test
    public synchronized void testDialNumberGeorge() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        final SipRequest lastRequest = georgeCall.getLastReceivedRequest();
        String receivedBody = new String(lastRequest.getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));
        // the number dialed uses a callerId of "+13055872294", which is what George should receive
        String contactHeader = georgeCall.getLastReceivedRequest().getMessage().getHeader("Contact").toString().replaceAll("\r\n","");
        assertTrue(contactHeader.equalsIgnoreCase("Contact: \"+13055872294\" <sip:+13055872294@" + restcommContact + ">"));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        // hangup.
        bobCall.disconnect();
        assertTrue(!bobCall.callTimeoutOrError());
        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        assertTrue(georgeCall.respondToDisconnect());
    }
    
  //Non-regression test for https://github.com/Mobicents/RestComm/issues/505
    @Test
    public synchronized void testDialNumberGeorge_403Forbidden() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

//        SipURI uri = bobSipStack.getAddressFactory().createSipURI(null, restcommContact);
//        assertTrue(bobPhone.register(uri, "bob", "1234", bobContact, 3600, 3600));
//
//        Credential c = new Credential("127.0.0.1", "bob", "1234");
//        bobPhone.addUpdateCredential(c);

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.FORBIDDEN, "FORBIDDEN-George", 3600));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.FORBIDDEN, bobCall.getLastReceivedResponse().getStatusCode());
    }

    //Non-regression test for https://github.com/Mobicents/RestComm/issues/505
    @Test
    public synchronized void testDialNumberGeorge_404_OnBye() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        bobCall.disconnect();

        georgeCall.listenForDisconnect();
        georgeCall.waitForDisconnect(5000);
        georgeCall.respondToDisconnect(404, "Not Here");
        georgeCall.disposeNoBye();
    }

    //Test for Issue 210: https://telestax.atlassian.net/browse/RESTCOMM-210
//Bob callerId should pass to the call created by Dial Number
    @Test
    public synchronized void testDialNumberGeorgePassInitialCallerId() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberNoCallerId)));

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        SipRequest georgeInvite = georgeCall.getLastReceivedRequest();
        assertTrue(((FromHeader)georgeInvite.getMessage().getHeader("From")).getAddress().getDisplayName().contains("bob"));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        // hangup.
        bobCall.disconnect();

        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        assertTrue(georgeCall.respondToDisconnect());
    }
    
    @Deployment(name = "TestDialVerbPartTwo", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        reconfigurePorts();

        Map<String,String> replacements = new HashMap();
        //replace mediaport 2727 
        replacements.put("2727", String.valueOf(mediaPort));        
        replacements.put("8080", String.valueOf(restcommHTTPPort));
        replacements.put("8090", String.valueOf(mockPort));
        replacements.put("5080", String.valueOf(restcommPort));
        replacements.put("5070", String.valueOf(georgePort));        
        replacements.put("5090", String.valueOf(bobPort));
        replacements.put("5091", String.valueOf(alicePort));
        List<String> resources = new ArrayList(Arrays.asList("hello-play.xml"));
        return WebArchiveUtil.createWebArchiveNoGw("restcomm-delay.xml", "restcomm.script_dialTest_new",resources, replacements);
    }      

}

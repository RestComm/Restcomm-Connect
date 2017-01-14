package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
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
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for Dial verb. Will test Dial Conference, Dial URI, Dial Client, Dial Number and Dial Fork
 *
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author jean.deruelle@telestax.com
 */
@RunWith(Arquillian.class)
public class TestDialVerbPartOneAnswerDelay {
    private final static Logger logger = Logger.getLogger(TestDialVerbPartOneAnswerDelay.class.getName());

    private static final String version = Version.getVersion();
    private static final byte[] bytes = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
    private static final String body = new String(bytes);
    private static final String sdpForHold = "v=0\n" +
            "o=bob-jitsi.org 0 2 IN IP4 192.168.1.190\n" +
            "s=-\n" +
            "c=IN IP4 192.168.1.190\n" +
            "t=0 0\n" +
            "m=audio 5000 RTP/AVP 0 8 3 101\n" +
            "a=rtpmap:0 PCMU/8000\n" +
            "a=rtpmap:8 PCMA/8000\n" +
            "a=rtpmap:3 GSM/8000\n" +
            "a=rtpmap:101 telephone-event/8000\n" +
            "a=extmap:1 urn:ietf:params:rtp-hdrext:csrc-audio-level\n" +
            "a=extmap:2 urn:ietf:params:rtp-hdrext:ssrc-audio-level\n" +
            "a=rtcp-xr:voip-metrics\n" +
            "m=video 5004 RTP/AVP 96 99\n" +
            "a=recvonly\n" +
            "a=rtpmap:96 H264/90000\n" +
            "a=fmtp:96 profile-level-id=4DE01f;packetization-mode=1\n" +
            "a=imageattr:96 send * recv [x=[0-1440],y=[0-900]]\n" +
            "a=rtpmap:99 H264/90000\n" +
            "a=fmtp:99 profile-level-id=4DE01f\n" +
            "a=imageattr:99 send * recv [x=[0-1440],y=[0-900]]\n";

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    // Fotini is a simple SIP Client. Will not register with Restcomm
    private SipStack fotiniSipStack;
    private SipPhone fotiniPhone;
    private String fotiniContact = "sip:fotini@127.0.0.1";

    private String dialRestcomm = "sip:1111@127.0.0.1:5080";
    private String notFoundDialNumber = "sip:+12223334457@127.0.0.1:5080";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialTest1Tool1");
        tool2 = new SipStackTool("DialTest1Tool2");
        tool3 = new SipStackTool("DialTest1Tool3");
        tool4 = new SipStackTool("DialTest1Tool4");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        georgeSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);

        fotiniSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5080");
        fotiniPhone = fotiniSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, fotiniContact);
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

        if (fotiniPhone != null) {
            fotiniPhone.dispose();
        }
        if (fotiniSipStack != null) {
            fotiniSipStack.dispose();
        }
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
    }
    
    private String dialUriRcml = "<Response><Dial timeLimit=\"100000\" timeout=\"1000000\"><Uri>sip:alice@127.0.0.1:5091</Uri></Dial><Hangup/></Response>";
    @Test
    public synchronized void testDialUriAliceHangup() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialUriRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
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
        String outboundCallSid = aliceCall.getLastReceivedRequest().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        String inboundCallSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        aliceCall.disconnect();

        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(50 * 1000));
        assertTrue(bobCall.respondToDisconnect());

        JsonObject inboundCallCdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, inboundCallSid);
        JsonObject outboundCallCdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, outboundCallSid);
        assertNotNull(inboundCallCdr);
        assertNotNull(outboundCallCdr);

        int inboundCdrDuration = inboundCallCdr.get("duration").getAsInt();
        int outboundCdrDuration = outboundCallCdr.get("duration").getAsInt();
        int outboundCdrRinging = outboundCallCdr.get("ring_duration").getAsInt();
        assertTrue(inboundCdrDuration==3);
        assertTrue(outboundCdrDuration==3);
        assertTrue(outboundCdrRinging==0);
    }

    @Test
    public synchronized void testDialUriBobHangup() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialUriRcml)));

        int initialCdrSize = 0;

        //Check CDR
        JsonObject cdrs = RestcommCallsTool.getInstance().getCalls("http://127.0.0.1:8080/restcomm", adminAccountSid, adminAuthToken);
        if (cdrs != null) {
            initialCdrSize = cdrs.get("calls").getAsJsonArray().size();
        }

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
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
        //Ringing time 5 sec
        Thread.sleep(5000);
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

        Thread.sleep(3000);

        //Check CDR
        cdrs = RestcommCallsTool.getInstance().getCalls("http://127.0.0.1:8080/restcomm", adminAccountSid, adminAuthToken);
        assertNotNull(cdrs);
        JsonArray cdrsArray = cdrs.get("calls").getAsJsonArray();
        System.out.println("cdrsArray.size(): " + cdrsArray.size());
        assertTrue((cdrsArray.size() - initialCdrSize) == 2);
    }

    @Test
    public synchronized void testDialUriBobHangupCheckCDRs() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialUriRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
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
        //X-RestComm-CallSid: CAedaccc8f598b4093b1fc33431e1c9ac9
        String outboundCallSid = aliceCall.getLastReceivedRequest().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        //Ringing time 5 sec
        Thread.sleep(5000);
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
//        X-RestComm-CallSid: CA5c2a775f2ca24003a04b7f7b90e6fabb
        String inboundCallSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        aliceCall.listenForDisconnect();
        // hangup.
        bobCall.disconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        Thread.sleep(6000);

        //Check CDR
        JsonObject inboundCallCdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, inboundCallSid);
        JsonObject outboundCallCdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, outboundCallSid);
        assertNotNull(inboundCallCdr);
        assertNotNull(outboundCallCdr);

        int inboundCdrDuration = inboundCallCdr.get("duration").getAsInt();
        int outboundCdrDuration = outboundCallCdr.get("duration").getAsInt();
        int outboundCdrRinging = outboundCallCdr.get("ring_duration").getAsInt();
        assertTrue(inboundCdrDuration==8);
        assertTrue(outboundCdrDuration==3);
        assertTrue(outboundCdrRinging==5);
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
    
    @Deployment(name = "TestDialVerbPartOneAnswerDelay", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.delete("/WEB-INF/classes/application.conf");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm-delay.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest_new", "data/hsql/restcomm.script");
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        logger.info("Packaged Test App");
        return archive;
    }

}

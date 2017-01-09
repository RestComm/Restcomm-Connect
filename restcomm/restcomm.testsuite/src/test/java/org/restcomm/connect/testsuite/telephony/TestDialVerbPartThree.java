package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import gov.nist.javax.sip.message.MessageExt;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
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
import org.restcomm.connect.testsuite.telephony.security.DigestServerAuthenticationMethod;

import javax.sip.address.SipURI;
import javax.sip.header.Header;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
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
public class TestDialVerbPartThree {
    private final static Logger logger = Logger.getLogger(TestDialVerbPartThree.class.getName());

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

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
//    private static SipStackTool tool3;
//    private static SipStackTool tool4;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    private String dialRestcomm = "sip:1111@127.0.0.1:5060";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialTest3Tool1");
        tool2 = new SipStackTool("DialTest3Tool2");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5060");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5060, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5060");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5060, aliceContact);
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
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
    }


    private String recordWithActionRcml = "<Response><Record action=\"http://127.0.0.1:8090/recordAction\" method=\"GET\" finishOnKey=\"#\" maxLength=\"10\" playBeep=\"true\"/></Response>";
    private String actionUrlRcml = "<Dial timeout=\"50\"><Uri>sip:alice@127.0.0.1:5091</Uri></Dial>";
//Non regression test for https://github.com/Mobicents/RestComm/issues/612
    @Test
    public synchronized void testRecord_ExecuteRCML_ReturnedFromActionURL() throws InterruptedException, ParseException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(recordWithActionRcml)));

        stubFor(get(urlPathEqualTo("/recordAction"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(actionUrlRcml)));

        //Prepare Fotini phone to receive a call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Initiate a call using Bob
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

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        //At this point bob leaves a voicemail

        //Now Fotini should receive a call
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(100, "Trying-Fotini", 600));
        assertTrue(aliceCall.sendIncomingCallResponse(180, "Ringing-Fotini", 600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Fotini", 3600, receivedBody, "application", "sdp", null, null));
        assertTrue(aliceCall.waitForAck(5000));
        aliceCall.listenForDisconnect();

        Thread.sleep(2000);

        // hangup.

        assertTrue(bobCall.disconnect());

        assertTrue(aliceCall.waitForDisconnect(50 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }

    private String playRcml = "<Play>/restcomm/audio/demo-prompt.wav</Play>";
    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialWithCustomHeaders() throws InterruptedException, ParseException {
        //Received request: GET /rcml?CallSid=CA154c8c93d7eb439989a6ea42915b6c1b&AccountSid=ACae6e420f425248d6a26948c17a9e2acf&From=bob&To=%2B17778&
        //CallStatus=ringing&ApiVersion=2012-04-24&Direction=inbound&CallerName&ForwardedFrom&SipHeader_X-MyCustom-Header1=Value1&SipHeader_X-MyCustom-Header2=Value2 HTTP/1.1
        stubFor(get(urlPathEqualTo("/1111"))
                .withQueryParam("SipHeader_X-MyCustom-Header1", containing("Value1"))
                .withQueryParam("SipHeader_X-MyCustom-Header2", containing("Value2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(playRcml)));

        ArrayList<String> additionalHeaders = new ArrayList<String>();
        additionalHeaders.add(bobPhone.getParent().getHeaderFactory().createHeader("X-MyCustom-Header1", "Value1").toString());
        additionalHeaders.add(bobPhone.getParent().getHeaderFactory().createHeader("X-MyCustom-Header2", "Value2").toString());

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", additionalHeaders, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));

        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);
        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        bobCall.listenForDisconnect();

        Thread.sleep(1000);

        assertTrue(bobCall.waitForDisconnect(5 * 1000));
        assertTrue(bobCall.respondToDisconnect());
    }

    private String dialSipRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Sip>sip:alice@127.0.0.1:5091?mycustomheader=foo&myotherheader=bar</Sip></Dial></Response>";
    @Test
// Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    public synchronized void testDialSip() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialSipRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
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
        MessageExt invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite);
        assertEquals(Request.INVITE, invite.getCSeqHeader().getMethod());
        Header mycustomheader = invite.getHeader("X-mycustomheader");
        Header myotherheader = invite.getHeader("X-myotherheader");
        assertNotNull(mycustomheader);
        assertNotNull(myotherheader);

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

        aliceCall.disconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
    }


    private String dialSipAuthRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Sip username=\"alice\" password=\"1234\">sip:alice@127.0.0.1:5091?mycustomheader=foo&myotherheader=bar</Sip></Dial></Response>";
//    @Ignore
    @Test
// Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
// in auth manner
    public synchronized void testDialSipAuth() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialSipAuthRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
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
        MessageExt invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite);
        assertEquals(Request.INVITE, invite.getCSeqHeader().getMethod());
        Header mycustomheader = invite.getHeader("X-mycustomheader");
        Header myotherheader = invite.getHeader("X-myotherheader");
        assertNotNull(mycustomheader);
        assertNotNull(myotherheader);

        DigestServerAuthenticationMethod dsam = new DigestServerAuthenticationMethod();
        dsam.initialize(); // it should read values from file, now all static

        ProxyAuthenticateHeader proxyAuthenticate = aliceSipStack.getHeaderFactory().createProxyAuthenticateHeader(
                dsam.getScheme());
        proxyAuthenticate.setParameter("realm", dsam.getRealm(null));
        proxyAuthenticate.setParameter("nonce", dsam.generateNonce());
        // proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
        proxyAuthenticate.setParameter("opaque", "");

        proxyAuthenticate.setParameter("algorithm", dsam.getAlgorithm());
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(proxyAuthenticate);
        assertTrue(aliceCall.sendIncomingCallResponse(Response.PROXY_AUTHENTICATION_REQUIRED, "Non authorized", 3600, headers,
                null, null));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite.getHeader(ProxyAuthorizationHeader.NAME));

        ProxyAuthorizationHeader proxyAuthorization = (ProxyAuthorizationHeader) invite
                .getHeader(ProxyAuthorizationHeader.NAME);

        boolean res = dsam.doAuthenticate("alice", "1234", proxyAuthorization, (Request) invite);
        assertTrue(res);

        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(aliceCall.waitForAck(50 * 1000));
        aliceCall.listenForDisconnect();

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
    }

    private String dialSipTagScreeningRcml = "<Response>\n" +
            "\t<Dial timeLimit=\"10\" timeout=\"10\" action=\"http://127.0.0.1:8080/restcomm/sip-dial-url-screening-test.jsp\" method=\"GET\">\n" +
            "\t  <Sip url=\"http://127.0.0.1:8090/screening\" method=\"GET\">sip:alice@127.0.0.1:5091?mycustomheader=foo&myotherheader=bar</Sip>\n" +
            "\t</Dial>\n" +
            "</Response>";
    private String screeningRcml = "<Response><Hangup/></Response>";
    @Test
// Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
// with URL screening
    public synchronized void testDialSipTagScreening() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialSipTagScreeningRcml)));

        stubFor(get(urlPathEqualTo("/screening"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(screeningRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
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
        MessageExt invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite);
        assertEquals(Request.INVITE, invite.getCSeqHeader().getMethod());
        Header mycustomheader = invite.getHeader("X-mycustomheader");
        Header myotherheader = invite.getHeader("X-myotherheader");
        assertNotNull(mycustomheader);
        assertNotNull(myotherheader);

        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        ArrayList<String> headers = new ArrayList<String>();
        Header customHeader = aliceSipStack.getHeaderFactory().createHeader("X-mycustomheader", "customValue");
        Header otherHeader = aliceSipStack.getHeaderFactory().createHeader("X-myothereader", "customOtherValue");
        headers.add(customHeader.toString());
        headers.add(otherHeader.toString());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp",
                headers, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
    }

    private String dialSipDialScreeningRcml = "<Response>\n" +
            "\t<Dial timeLimit=\"10\" timeout=\"10\" action=\"http://127.0.0.1:8090/action\" method=\"GET\">\n" +
            "\t  <Sip url=\"http://127.0.0.1:8090/screening\" method=\"GET\">sip:alice@127.0.0.1:5091?mycustomheader=foo&myotherheader=bar</Sip>\n" +
            "\t</Dial>\n" +
            "</Response>";
    private String sipDialUrlActionRcml = "<Response><Hangup/></Response>";
    @Test
// Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
// with Dial Action screening
    public synchronized void testDialSipDialTagScreening() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialSipDialScreeningRcml)));

        stubFor(get(urlPathEqualTo("/screening"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(screeningRcml)));

        stubFor(get(urlPathEqualTo("/action"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sipDialUrlActionRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
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
        MessageExt invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite);
        assertEquals(Request.INVITE, invite.getCSeqHeader().getMethod());
        Header mycustomheader = invite.getHeader("X-mycustomheader");
        Header myotherheader = invite.getHeader("X-myotherheader");
        assertNotNull(mycustomheader);
        assertNotNull(myotherheader);

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        ArrayList<String> headers = new ArrayList<String>();
        Header customHeader = aliceSipStack.getHeaderFactory().createHeader("X-mycustomheader", "customValue");
        Header otherHeader = aliceSipStack.getHeaderFactory().createHeader("X-myothereader", "customOtherValue");
        headers.add(customHeader.toString());
        headers.add(otherHeader.toString());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.NOT_FOUND, "Not-Found", 3600, receivedBody, "application",
                "sdp", headers, null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.NOT_FOUND, bobCall.getLastReceivedResponse().getStatusCode());
    }

    @Test
// Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
// with Dial Action screening
    public synchronized void testDialSipDialTagScreening180Decline() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialSipDialScreeningRcml)));

        stubFor(get(urlPathEqualTo("/screening"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(screeningRcml)));

        stubFor(get(urlPathEqualTo("/action"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sipDialUrlActionRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
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
        MessageExt invite = (MessageExt) aliceCall.getLastReceivedRequest().getMessage();
        assertNotNull(invite);
        assertEquals(Request.INVITE, invite.getCSeqHeader().getMethod());
        Header mycustomheader = invite.getHeader("X-mycustomheader");
        Header myotherheader = invite.getHeader("X-myotherheader");
        assertNotNull(mycustomheader);
        assertNotNull(myotherheader);

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());

        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing", 3600, receivedBody, "application", "sdp",
                null, null));

        ArrayList<String> headers = new ArrayList<String>();
        Header customHeader = aliceSipStack.getHeaderFactory().createHeader("X-mycustomheader", "customValue");
        Header otherHeader = aliceSipStack.getHeaderFactory().createHeader("X-myothereader", "customOtherValue");
        headers.add(customHeader.toString());
        headers.add(otherHeader.toString());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.DECLINE, "Declined", 3600, receivedBody, "application", "sdp",
                headers, null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.DECLINE, bobCall.getLastReceivedResponse().getStatusCode());
    }

    private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client></Dial></Response>";
    @Test //For github issue #600, At the DB the IncomingPhoneNumber is '1111' and we dial '+1111', Restcomm should find this number even with the '+'
    public synchronized void testDialClientAliceWithPlusSign() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:+1111@127.0.0.1:5060", null, body, "application", "sdp", null, null);
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

    @Test //For github issue #600, At the DB the IncomingPhoneNumber is '+2222' and we dial '2222', Restcomm should find this number even without the '+'
    public synchronized void testDialClientAliceWithoutPlusSign() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5060");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:2222@127.0.0.1:5060", null, body, "application", "sdp", null, null);
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

    @Deployment(name = "TestDialVerbPartThree", managed = true, testable = false)
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
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest_new", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}

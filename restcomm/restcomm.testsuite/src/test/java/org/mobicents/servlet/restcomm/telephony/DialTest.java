package org.mobicents.servlet.restcomm.telephony;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.message.MessageExt;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipResponse;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.http.RestcommCallsTool;
import org.mobicents.servlet.restcomm.provisioning.number.vi.AvailablePhoneNumbersEndpointTestUtils;
//import org.mobicents.servlet.restcomm.telephony.Version;
import org.mobicents.servlet.restcomm.telephony.security.DigestServerAuthenticationMethod;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonObject;

/**
 * Test for Dial verb. Will test Dial Conference, Dial URI, Dial Client, Dial Number and Dial Fork
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author jean.deruelle@telestax.com
 */
@RunWith(Arquillian.class)
public class DialTest {
    private final static Logger logger = Logger.getLogger(DialTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();
    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    private Deployer deployer;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;
    private static SipStackTool tool5;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@127.0.0.1:5092";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";
    
    // Fotini is a simple SIP Client. Will not register with Restcomm
    private SipStack fotiniSipStack;
    private SipPhone fotiniPhone;
    private String fotiniContact = "sip:fotini@127.0.0.1";
    
    private String dialConf = "sip:+12223334451@127.0.0.1:5080";
    private String dialFork = "sip:+12223334452@127.0.0.1:5080";
    private String dialFork_with_RCML = "sip:+12223334462@127.0.0.1:5080";
    private String dialURI = "sip:+12223334454@127.0.0.1:5080";
    private String dialClient = "sip:+12223334455@127.0.0.1:5080";
    private String dialClientWithRecord = "sip:+12223334499@127.0.0.1:5080";
    private String dialNumber = "sip:+12223334456@127.0.0.1:5080";
    private String notFoundDialNumber = "sip:+12223334457@127.0.0.1:5080";
    private String dialSip = "sip:+12223334458@127.0.0.1:5080";
    private String dialSipSecurity = "sip:+12223334459@127.0.0.1:5080";
    private String dialSipTagScreening = "sip:+12223334460@127.0.0.1:5080";
    private String dialSipDialTagScreening = "sip:+12223334461@127.0.0.1:5080";
    private String dialDIDGreaterThan15Digits = "sip:+12345678912345678912@127.0.0.1:5080";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("CallTestDial1");
        tool2 = new SipStackTool("CallTestDial2");
        tool3 = new SipStackTool("CallTestDial3");
        tool4 = new SipStackTool("CallTestDial4");
        tool5 = new SipStackTool("CallTestDial5");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        henriqueSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        henriquePhone = henriqueSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, henriqueContact);

        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);
        
        fotiniSipStack = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5080");
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

        if (henriqueSipStack != null) {
            henriqueSipStack.dispose();
        }
        if (henriquePhone != null) {
            henriquePhone.dispose();
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
        deployer.undeploy("DialTest");
        Thread.sleep(1000);
    }

    @Test
    public synchronized void testDialConference() throws InterruptedException {
        deployer.deploy("DialTest");

        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialConf, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        // George calls to the conference
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, dialConf, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        int responseGeorge = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseGeorge == Response.TRYING || responseGeorge == Response.RINGING);

        if (responseGeorge == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());
        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        // Wait for the media to play and the call to hangup.
        bobCall.listenForDisconnect();
        georgeCall.listenForDisconnect();

        // Start a new thread for george to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(georgeCall.waitForDisconnect(30 * 1000));
            }
        }).start();

        // Start a new thread for bob to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(bobCall.waitForDisconnect(30 * 1000));
            }
        }).start();

        // assertTrue(bobCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    @Test
    public synchronized void testDialConferenceWithContactHeaderPortNull() throws InterruptedException {
        deployer.deploy("DialTest");

        final SipCall fotiniCall = fotiniPhone.createSipCall();
        fotiniCall.initiateOutgoingCall(fotiniContact, dialConf, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(fotiniCall);
        assertTrue(fotiniCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = fotiniCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        if (responseBob == Response.TRYING) {
            assertTrue(fotiniCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, fotiniCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(fotiniCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, fotiniCall.getLastReceivedResponse().getStatusCode());
        fotiniCall.sendInviteOkAck();
        assertTrue(!(fotiniCall.getLastReceivedResponse().getStatusCode() >= 400));

        // George calls to the conference
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, dialConf, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        int responseGeorge = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseGeorge == Response.TRYING || responseGeorge == Response.RINGING);

        if (responseGeorge == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());
        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        // Wait for the media to play and the call to hangup.
        fotiniCall.listenForDisconnect();
        georgeCall.listenForDisconnect();

        // Start a new thread for george to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(georgeCall.waitForDisconnect(30 * 1000));
            }
        }).start();

        // Start a new thread for bob to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(fotiniCall.waitForDisconnect(30 * 1000));
            }
        }).start();

        // assertTrue(bobCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    // Non regression test for
    // https://bitbucket.org/telestax/telscale-restcomm/issue/113/when-restcomm-cannot-find-an-app-url-it
    public synchronized void testDialApplicationInvalidURL() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, notFoundDialNumber, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);

        // wait for 100 Trying
        // assertTrue(bobCall.waitOutgoingCallResponse(10000));
        // Thread.sleep(3000);

        // wait for 180 Ringing
        assertTrue(bobCall.waitOutgoingCallResponse(10000));
        // wait for 404 Not Found
        assertTrue(bobCall.waitOutgoingCallResponse(10000));
        ArrayList<SipResponse> responses = bobCall.getAllReceivedResponses();
        for (SipResponse sipResponse : responses) {
            logger.info("response received : " + sipResponse.getStatusCode());
            if (sipResponse.getStatusCode() == Response.NOT_FOUND) {
                return;
            }
        }
        assertTrue("we didn't get a 404 as we should have", false);
    }

    @Test
    public synchronized void testDialUriAliceHangup() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialURI, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        aliceCall.disconnect();

        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        assertTrue(bobCall.respondToDisconnect());
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    public synchronized void testDialUriBobHangup() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialURI, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    public synchronized void testDialClientAlice() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialClient, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    //Test for issue RESTCOMM-617
    @Test
    public synchronized void testDialClientAliceToBigDID() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialDIDGreaterThan15Digits, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    public synchronized void testDialClientAliceWithRecord() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");
        
        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialClientWithRecord, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
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
        
        Thread.sleep(3000);
        
        final String deploymentUrl = "http://127.0.0.1:8080/restcomm/";
        JsonObject recordings = RestcommCallsTool.getInstance().getRecordings(deploymentUrl, adminAccountSid, adminAuthToken);
        assertNotNull(recordings);
        assertTrue("7.0".equalsIgnoreCase(recordings.get("duration").getAsString()));
        assertNotNull(recordings.get("uri").getAsString());
    }

    
    @Test
    public synchronized void testDialNumberGeorge() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialNumber, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        // hangup.
        bobCall.disconnect();

        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    //Test for Issue 210: https://telestax.atlassian.net/browse/RESTCOMM-210
    //Bob callerId should pass to the call created by Dial Number
    @Test
    public synchronized void testDialNumberGeorgePassInitialCallerId() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Prepare George phone to receive call
        georgePhone.setLoopback(true);
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialNumber, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        SipRequest georgeInvite = georgeCall.getLastReceivedRequest();
        assertTrue(((FromHeader)georgeInvite.getMessage().getHeader("From")).getAddress().getURI().toString().contains("bob"));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        // hangup.
        bobCall.disconnect();

        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    @Test
    public synchronized void testDialFork() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork, null, body, "application", "sdp", null, null);
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

        // Start a new thread for George
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
                assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "Trying-George", 3600));
                assertTrue(georgeCall.sendIncomingCallResponse(486, "Busy Here-George", 3600));
                assertTrue(georgeCall.waitForAck(50 * 1000));
            }
        }).start();

        // Start a new thread for Alice
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
                assertTrue(aliceCall.sendIncomingCallResponse(Response.TRYING, "Trying-Alice", 3600));
                assertTrue(aliceCall.sendIncomingCallResponse(486, "Busy Here-Alice", 3600));
                assertTrue(aliceCall.waitForAck(50 * 1000));
            }
        }).start();

        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique-1", 3600));
        String receivedBody = new String(henriqueCall.getLastReceivedRequest().getRawContent());
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.OK, "OK-Henrique", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(henriqueCall.waitForAck(50 * 1000));

        henriqueCall.listenForDisconnect();

        Thread.sleep(8000);

        // hangup.

        bobCall.disconnect();

        assertTrue(henriqueCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialForkNoAnswerButFromHenrique() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(100, "Trying-George", 600));
        assertTrue(georgeCall.sendIncomingCallResponse(180, "Ringing-George", 600));
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(100, "Trying-Alice", 600));
        assertTrue(aliceCall.sendIncomingCallResponse(180, "Ringing-Alice", 600));
        
        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique-1", 3600));
        String receivedBody = new String(henriqueCall.getLastReceivedRequest().getRawContent());
        
        Thread.sleep(1000);
        
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.OK, "OK-Henrique", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(henriqueCall.waitForAck(50 * 1000));
        
        assertTrue(georgeCall.listenForCancel());
        assertTrue(aliceCall.listenForCancel());
        
        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(30 * 1000);
        SipTransaction aliceCancelTransaction = aliceCall.waitForCancel(30 * 1000);
        assertNotNull(georgeCancelTransaction);
        assertNotNull(aliceCancelTransaction);
        georgeCall.respondToCancel(georgeCancelTransaction, 200, "OK - George", 600);
        aliceCall.respondToCancel(aliceCancelTransaction, 200, "OK - Alice", 600);
        
        henriqueCall.listenForDisconnect();

        Thread.sleep(8000);

        // hangup.

        bobCall.disconnect();

        assertTrue(henriqueCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialForkNoAnswerButFromGeorgePSTN() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "Trying-George", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.TRYING, "Trying-Alice", 600));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 600));
        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.TRYING, "Trying-Henrique", 600));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique", 600));

        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        
        Thread.sleep(2000);
        
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(50 * 1000));
        
        assertTrue(henriqueCall.listenForCancel());
        assertTrue(aliceCall.listenForCancel());

        SipTransaction aliceCancelTransaction = aliceCall.waitForCancel(30 * 1000);
        SipTransaction henriqueCancelTransaction = henriqueCall.waitForCancel(30 * 1000);
        assertNotNull(aliceCancelTransaction);
        assertNotNull(henriqueCancelTransaction);
        henriqueCall.respondToCancel(henriqueCancelTransaction, 200, "OK - Henrique", 600);
        aliceCall.respondToCancel(aliceCancelTransaction, 200, "OK - Alice", 600);
        
        georgeCall.listenForDisconnect();

        Thread.sleep(8000);

        // hangup.

        bobCall.disconnect();

        assertTrue(georgeCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialForkNoAnswerButFromAliceClient() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "Trying-George", 600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 600));
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.TRYING, "Trying-Alice", 3600));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.TRYING, "Trying-Henrique", 600));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique", 600));

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        
        Thread.sleep(2000);
        
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(aliceCall.waitForAck(50 * 1000));
        
        assertTrue(henriqueCall.listenForCancel());
        assertTrue(georgeCall.listenForCancel());

        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(30 * 1000);
        SipTransaction henriqueCancelTransaction = henriqueCall.waitForCancel(30 * 1000);
        assertNotNull(georgeCancelTransaction);
        assertNotNull(henriqueCancelTransaction);
        henriqueCall.respondToCancel(henriqueCancelTransaction, 200, "OK - Henrique", 600);
        aliceCall.respondToCancel(georgeCancelTransaction, 200, "OK - Alice", 600);
        
        aliceCall.listenForDisconnect();

        Thread.sleep(8000);

        // hangup.

        bobCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialForkNoAnswerMoveToTheNextVerbAndCallFotini() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();
        
        //Prepare Fotini phone to receive a call
        final SipCall fotiniCall = fotiniPhone.createSipCall();
        fotiniCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(100, "Trying-George", 600));
        assertTrue(georgeCall.sendIncomingCallResponse(180, "Ringing-George", 600));
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(100, "Trying-Alice", 600));
        assertTrue(aliceCall.sendIncomingCallResponse(180, "Ringing-Alice", 600));        
        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(100, "Trying-Henrique", 600));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique-1", 3600));

        //No one will answer the call and RCML will move to the next verb to call Fotini
        
        assertTrue(georgeCall.listenForCancel());
        assertTrue(aliceCall.listenForCancel());
        assertTrue(henriqueCall.listenForCancel());
        
        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(20 * 1000);
        SipTransaction aliceCancelTransaction = aliceCall.waitForCancel(20 * 1000);
        SipTransaction henriqueCancelTransaction = henriqueCall.waitForCancel(20 * 1000);
        assertNotNull(georgeCancelTransaction);
        assertNotNull(aliceCancelTransaction);
        assertNotNull(henriqueCancelTransaction);
        georgeCall.respondToCancel(georgeCancelTransaction, 200, "OK - George", 600);
        aliceCall.respondToCancel(aliceCancelTransaction, 200, "OK - Alice", 600);
        henriqueCall.respondToCancel(henriqueCancelTransaction, 200, "OK - Henrique", 600);
        
        assertTrue(alicePhone.unregister(aliceContact, 3600));
        
        //Now Fotini should receive a call
        assertTrue(fotiniCall.waitForIncomingCall(30 * 1000));
        assertTrue(fotiniCall.sendIncomingCallResponse(100, "Trying-Fotini", 600));
        assertTrue(fotiniCall.sendIncomingCallResponse(180, "Ringing-Fotini", 600));
        String receivedBody = new String(fotiniCall.getLastReceivedRequest().getRawContent());
        assertTrue(fotiniCall.sendIncomingCallResponse(Response.OK, "OK-Fotini", 3600, receivedBody, "application", "sdp", null, null));
        
        fotiniCall.listenForDisconnect();

        Thread.sleep(4000);

        // hangup.

        bobCall.disconnect();

        assertTrue(fotiniCall.waitForDisconnect(50 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    } 
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080
    private String rcmlToReturn = "<Dial timeout=\"5\"><Uri>sip:fotini@127.0.0.1:5060</Uri></Dial>";
    //Non regression test for https://telestax.atlassian.net/browse/RESTCOMM-585
    @Test
    public synchronized void testDialForkNoAnswerExecuteRCML_ReturnedFromActionURL() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");
        
        stubFor(post(urlEqualTo("/test"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(rcmlToReturn)));

        // Register Alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Prepare George phone to receive call
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Prepare Henrique phone to receive call
        // henriquePhone.setLoopback(true);
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.listenForIncomingCall();
        
        //Prepare Fotini phone to receive a call
        final SipCall fotiniCall = fotiniPhone.createSipCall();
        fotiniCall.listenForIncomingCall();

        // Initiate a call using Bob
        final SipCall bobCall = bobPhone.createSipCall();

        bobCall.initiateOutgoingCall(bobContact, dialFork_with_RCML, null, body, "application", "sdp", null, null);
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

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(100, "Trying-George", 600));
        assertTrue(georgeCall.sendIncomingCallResponse(180, "Ringing-George", 600));
        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(100, "Trying-Alice", 600));
        assertTrue(aliceCall.sendIncomingCallResponse(180, "Ringing-Alice", 600));        
        assertTrue(henriqueCall.waitForIncomingCall(30 * 1000));
        assertTrue(henriqueCall.sendIncomingCallResponse(100, "Trying-Henrique", 600));
        assertTrue(henriqueCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Henrique-1", 3600));

        //No one will answer the call and RCML will move to the next verb to call Fotini
        
        assertTrue(georgeCall.listenForCancel());
        assertTrue(aliceCall.listenForCancel());
        assertTrue(henriqueCall.listenForCancel());
        
        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(50 * 1000);
        SipTransaction henriqueCancelTransaction = henriqueCall.waitForCancel(50 * 1000);
        SipTransaction aliceCancelTransaction = aliceCall.waitForCancel(50 * 1000);
        assertNotNull(georgeCancelTransaction);
        assertNotNull(aliceCancelTransaction);
        assertNotNull(henriqueCancelTransaction);
        georgeCall.respondToCancel(georgeCancelTransaction, 200, "OK - George", 600);
        aliceCall.respondToCancel(aliceCancelTransaction, 200, "OK - Alice", 600);
        henriqueCall.respondToCancel(henriqueCancelTransaction, 200, "OK - Henrique", 600);
        
        assertTrue(alicePhone.unregister(aliceContact, 3600));
        
        //Now Fotini should receive a call
        assertTrue(fotiniCall.waitForIncomingCall(30 * 1000));
        assertTrue(fotiniCall.sendIncomingCallResponse(100, "Trying-Fotini", 600));
        assertTrue(fotiniCall.sendIncomingCallResponse(180, "Ringing-Fotini", 600));
        String receivedBody = new String(fotiniCall.getLastReceivedRequest().getRawContent());
        assertTrue(fotiniCall.sendIncomingCallResponse(Response.OK, "OK-Fotini", 3600, receivedBody, "application", "sdp", null, null));
        
        fotiniCall.listenForDisconnect();

        Thread.sleep(2000);

        // hangup.

        bobCall.disconnect();

        assertTrue(fotiniCall.waitForDisconnect(50 * 1000));

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    @Test
    // Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    public synchronized void testDialSip() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSip, null, body, "application", "sdp", null, null);
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
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        // assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }


    @Ignore
    @Test
    // Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    // in auth manner
    public synchronized void testDialSipAuth() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSipSecurity, null, body, "application", "sdp", null, null);
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
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        // assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    // Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    // with URL screening
    public synchronized void testDialSipTagScreening() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSipTagScreening, null, body, "application", "sdp", null, null);
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
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        // assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    // Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    // with Dial Action screening
    public synchronized void testDialSipDialTagScreening() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");
        
        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSipDialTagScreening, null, body, "application", "sdp", null, null);
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

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        // assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Test
    // Non regression test for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    // with Dial Action screening
    public synchronized void testDialSipDialTagScreening180Decline() throws InterruptedException, ParseException {
        deployer.deploy("DialTest");
        
        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSipDialTagScreening, null, body, "application", "sdp", null, null);
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

        Thread.sleep(3000);

        // hangup.
        bobCall.disconnect();

        aliceCall.disconnect();
        // assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Deployment(name = "DialTest", managed = false, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        String testversion = version; 
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
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-conference-entry.xml");
        archive.addAsWebResource("dial-fork-entry.xml");
        archive.addAsWebResource("dial-fork-with-action-entry.xml");
        archive.addAsWebResource("dial-uri-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("dial-client-entry-with-recording.xml");
        archive.addAsWebResource("dial-sip.xml");
        archive.addAsWebResource("dial-sip-auth.xml");
        archive.addAsWebResource("dial-sip-screening.xml");
        archive.addAsWebResource("dial-sip-dial-screening.xml");
        archive.addAsWebResource("dial-number-entry.xml");
        archive.addAsWebResource("sip-url-screening-test.jsp");
        archive.addAsWebResource("sip-dial-url-screening-test.jsp");
        archive.addAsWebResource("hello-play.xml");
        archive.addAsWebResource("send-sms.xml");
        logger.info("Packaged Test App");
        return archive;
    }
}

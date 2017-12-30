package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.restcomm.connect.commons.Version;

import javax.sip.message.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by gvagenas on 4/5/16.
 */
@Ignore public class CallLifecycleTestLive {

    private final static Logger logger = Logger.getLogger(CallLifecycleTest.class.getName());

    private static final String version = Version.getVersion();
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

    //Dial Action URL: http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@192.168.1.190:8080/restcomm/2012-04-24/DialAction Method: POST
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@192.168.1.190:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@192.168.1.190:5091";

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@192.168.1.190:5092";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@192.168.1.190:5070";

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialActionTest1");
        tool2 = new SipStackTool("DialActionTest2");
        tool3 = new SipStackTool("DialActionTest3");
        tool4 = new SipStackTool("DialActionTest4");
    }


    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "192.168.1.190", "5090", "192.168.1.190:5080");
        bobPhone = bobSipStack.createSipPhone("192.168.1.190", SipStack.PROTOCOL_UDP, 5080, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "192.168.1.190", "5091", "192.168.1.190:5080");
        alicePhone = aliceSipStack.createSipPhone("192.168.1.190", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        henriqueSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "192.168.1.190", "5092", "192.168.1.190:5080");
        henriquePhone = henriqueSipStack.createSipPhone("192.168.1.190", SipStack.PROTOCOL_UDP, 5080, henriqueContact);

        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "192.168.1.190", "5070", "192.168.1.190:5080");
        georgePhone = georgeSipStack.createSipPhone("192.168.1.190", SipStack.PROTOCOL_UDP, 5080, georgeContact);
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
        Thread.sleep(2000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
    }

    @Test @Ignore
    public void testDialCancelBeforeDialingClientAliceAfterTryingLive() throws ParseException, InterruptedException, MalformedURLException {

        Credential c = new Credential("192.168.1.190","bob","1234");
        bobPhone.addUpdateCredential(c);

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1234@192.168.1.190:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitForAuthorisation(5 * 1000));
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING);

        SipTransaction cancelTransaction = bobCall.sendCancel();
        assertNotNull(cancelTransaction);
        assertTrue(bobCall.waitForCancelResponse(cancelTransaction,5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.OK);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.REQUEST_TERMINATED);

        Thread.sleep(10000);
    }

    @Test @Ignore
    public void testDialCancelBeforeDialingClientAliceAfterRinging() throws ParseException, InterruptedException, MalformedURLException {

        Credential c = new Credential("192.168.1.190","bob","1234");
        bobPhone.addUpdateCredential(c);

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1234@192.168.1.190:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitForAuthorisation(5 * 1000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

//        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
//        assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.OK);
//        bobCall.sendInviteOkAck();

        Thread.sleep(15);

        SipTransaction cancelTransaction = bobCall.sendCancel();
        assertNotNull(cancelTransaction);
        assertTrue(bobCall.waitForCancelResponse(cancelTransaction,5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.OK);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.REQUEST_TERMINATED);
    }
}

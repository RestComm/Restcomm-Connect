package org.restcomm.connect.testsuite.telephony;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
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
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.WebArchiveUtil;
import org.restcomm.connect.testsuite.http.RestcommAccountsTool;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
import org.restcomm.connect.testsuite.http.TranscriptionEndpointTool;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@RunWith(Arquillian.class)
public class DialRecordingTranscribeTest {
    private final static Logger logger = Logger.getLogger(DialRecordingTranscribeTest.class.getName());

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

    private static int mediaPort = NetworkPortAssigner.retrieveNextPortByFile();

    private static int mockPort = NetworkPortAssigner.retrieveNextPortByFile();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    //private static String bobPort = "5090";//String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private static String bobPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String bobContact = "sip:bob@127.0.0.1:" + bobPort;

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private static String alicePort = "5091";//String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    //private static String alicePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String aliceContact = "sip:alice@127.0.0.1:" + alicePort;

    public static final String recordCall = "<Response><Record timeout=\"15\" maxLength=\"60\" transcribe=\"true\"/><Gather><Say voice=\"man\" language=\"en\" loop=\"1\">Hello World!</Say><Play loop=\"1\">https://127.0.0.1:8080/restcomm/audio/hello-world.wav</Play><Pause length=\"1\"/></Gather></Response>";
    
    //FIXME:    
    //private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\" record=\"true\" transcribe=\"true\"><Client>alice</Client></Dial></Response>";

    //Admin role
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAccountAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    //Admin role with * permissions
    private String accountSid1 = "ACae6e420f425248d6a26948c17a9e2acg";
    private String accountAuthToken1 = "77f8c12cc7b8f8423e5c38b035249166";
    //Dev role without ASR permission
    private String accountSid2 = "ACae6e420f425248d6a26948c17a9e2ach";
    private String accountAuthToken2 = "77f8c12cc7b8f8423e5c38b035249166";

    private String expectedTranscriptionText1 = "";
    private String emptyTranscriptionText;
    private String accountRole1 = "Administrator";
    private String accountRole2 = "Developer";
    private String permissionSid1 = "PE00000000000000000000000000000001";
    private String permissionName1 = "Restcomm:*:ASR";

    private String incomingNumber1 = "1111";
    private String incomingNumber2 = "2222";

    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;
    private static String restcommContact = "127.0.0.1:" + restcommPort;

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialRecordingTranscribeTest1");
        tool2 = new SipStackTool("DialRecordingTranscribeTest2");
    }

    public static void reconfigurePorts() {
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort;
        }
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));
        }
    }

    @Before
    public void before() throws Exception {

        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", bobPort, restcommContact);
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort, restcommContact);
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact);

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

        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    public synchronized void testTranscribeWithClientRolePermissionOnly() {
        JsonArray jo;
        //use admin
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, alicePhone, aliceContact);
        //success
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));

        //change account role from admin to developer
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), accountSid1, accountAuthToken1, accountSid1, null, null, null, accountRole2, null);
        //fail
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, alicePhone, aliceContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));
    }

    public synchronized void testTranscribeWithAdditiveClientAccountPermission() {
        JsonArray jo = null;
        //use developer without asr
        jo = dialRecordRcml(deploymentUrl, accountSid2, accountAuthToken2, incomingNumber1, recordCall, alicePhone, aliceContact);

        //fail
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));

        //add account specific asr permission, true
        RestcommAccountsTool.getInstance().updateAccountPermission(deploymentUrl.toString(), accountSid1, accountAuthToken1, permissionSid1, "true");
        //success
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, alicePhone, aliceContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));
    }

    public synchronized void testTranscribeWithSubtractiveClientAccountPermission() {
        JsonArray jo;
        //use admin without everything
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, alicePhone, aliceContact);
        //success
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));

        //add account specific asr permission, false
        RestcommAccountsTool.getInstance().updateAccountPermission(deploymentUrl.toString(), accountSid1, accountAuthToken1, permissionSid1, "false");
        //fail
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, alicePhone, aliceContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));
    }

    @Test
    public synchronized void testTranscribeWithAppRolePermissionOnly() {
        JsonArray jo;
        //use admin
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, bobPhone, bobContact);
        //success
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));

        //change account role to developer
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), adminAccountSid, adminAccountAuthToken, accountSid1, null, null, null, accountRole2, null);
        //fail
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, bobPhone, bobContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));
    }

    @Test
    public synchronized void testTranscribeWithAdditiveAppAccountPermission() {
        JsonArray jo = null;
        //use App under Account with Developer Role without asr
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber2, recordCall, bobPhone, bobContact);

        //fail
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));

        //add account specific asr permission, true to Account
        JsonObject j = RestcommAccountsTool.getInstance().updateAccountPermission(deploymentUrl.toString(), accountSid2, accountAuthToken2, permissionSid1, "true");
        System.out.print(j);
        //success
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber2, recordCall, bobPhone, bobContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));
    }

    @Test
    public synchronized void testTranscribeWithSubtractiveAppAccountPermission() {
        JsonArray jo;
        //use admin without everything
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, bobPhone, bobContact);
        //success
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(expectedTranscriptionText1));

        //add account specific asr permission, false
        RestcommAccountsTool.getInstance().updateAccountPermission(deploymentUrl.toString(), accountSid1, accountAuthToken1, permissionSid1, "false");
        //fail
        jo = dialRecordRcml(deploymentUrl, accountSid1, accountAuthToken1, incomingNumber1, recordCall, bobPhone, bobContact);
        assert(jo.get(0).isJsonObject());
        assert(jo.get(0).getAsJsonObject().get("transcription_text").getAsString().equals(emptyTranscriptionText));
    }

    private JsonArray dialRecordRcml(URL deployUrl, String accountSid, String accountAuthToken, String appString, 
            String rcmlStr, SipPhone callerPhone, String callerContact) {
        try {
            stubFor(get(urlPathEqualTo("/"+appString)).willReturn(
                    aResponse().withStatus(200)
                            .withHeader("Content-Type", "text/xml")
                            .withBody(rcmlStr)));

            // Create outgoing call with first phone
            final SipCall sipCall = callerPhone.createSipCall();
            String dialAppString = "sip:"+appString+"@" + restcommContact; 
            sipCall.initiateOutgoingCall(callerContact, dialAppString, null, body, "application", "sdp", null, null);
            assertLastOperationSuccess(sipCall);
            assertTrue(sipCall.waitOutgoingCallResponse(5 * 1000));
            final int response = sipCall.getLastReceivedResponse().getStatusCode();
            assertTrue(response == Response.TRYING || response == Response.RINGING);

            if (response == Response.TRYING) {
                assertTrue(sipCall.waitOutgoingCallResponse(5 * 1000));
                assertEquals(Response.RINGING, sipCall.getLastReceivedResponse().getStatusCode());
            }

            assertTrue(sipCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

            sipCall.sendInviteOkAck();
            assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode() >= 400));
            String callSid = sipCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

            //NB: no need to look at lcm?

            Thread.sleep(3000);
            sipCall.disconnect();
            Thread.sleep(3000);

            // Check recording
            JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deployUrl.toString(), accountSid, accountAuthToken, callSid);
            assertNotNull(recording);
            assertEquals(1, recording.size());
            //logger.debug(recording);
            double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
            assertEquals(3.0, duration, 1);
            //have to sleep, expecting AsrResponse to modify transcription
            //TODO: sleep time might have to be tuned
            Thread.sleep(5000);
        } catch (Exception e) {
            //dont need to rethrow, trnascription will just be empty as a result
            e.printStackTrace();
        }
        //check transcription
        JsonArray transcriptions = TranscriptionEndpointTool.getInstance().getTranscriptionListTranscriptions(deploymentUrl.toString(), accountSid, accountAuthToken);
        //logger.debug(transcriptions);
        return transcriptions;
    }

    @Deployment(name = "DialRecordingTranscribeTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        reconfigurePorts();

        Map<String, String> replacements = new HashMap();
        replacements.put("2727", String.valueOf(mediaPort));
        replacements.put("8080", String.valueOf(restcommHTTPPort));
        replacements.put("8090", String.valueOf(mockPort));
        replacements.put("5080", String.valueOf(restcommPort));
        replacements.put("5090", String.valueOf(bobPort));
        //replacements.put("5091", String.valueOf(alicePort));

        List<String> resources = new ArrayList();
        WebArchive wa = WebArchiveUtil.createWebArchiveNoGw("restcomm_dialrecording_asr.xml",
                "restcomm.script_DialRecordingTranscribe", resources, replacements);
        return wa;
    }
}

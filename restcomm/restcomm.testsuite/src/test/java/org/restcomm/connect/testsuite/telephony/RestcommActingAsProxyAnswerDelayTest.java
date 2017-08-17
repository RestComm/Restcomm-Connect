package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.message.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by gvagenas on 26/06/2017.
 */
@RunWith(Arquillian.class)
public class RestcommActingAsProxyAnswerDelayTest {

    private final static Logger logger = Logger.getLogger(RestcommActingAsProxyAnswerDelayTest.class.getName());

    @Deployment(name = "RestcommActingAsProxyTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm_acting_as_proxy_answer_delay.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_acting_as_proxy", "data/hsql/restcomm.script");
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        logger.info("Packaged Test App");
        return archive;
    }

    private static final String version = Version.getVersion();
//    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
//            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
//            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
//            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
//            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
//    private final String body = new String(bytes);

    private final String body = "v=0\n" +
            "o=user1 53655765 2353687637 IN IP4 10.100.10.99\n" +
            "s=- RestcommTestsuite\n" +
            "c=IN IP4 10.100.10.99\n" +
            "t=0 0\n" +
            "m=audio 6000 RTP/AVP 0\n" +
            "a=rtpmap:0 PCMU/8000";

    private final String body2 = "v=0\n" +
            "o=user1 53655765 2353687637 IN IP4 172.10.9.111\n" +
            "s=- RestcommTestsuite\n" +
            "c=IN IP4 172.10.9.111\n" +
            "t=0 0\n" +
            "m=audio 6000 RTP/AVP 0\n" +
            "a=rtpmap:0 PCMU/8000";

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;
//    private static SipStackTool tool5;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack otherRestcommSipStack;
    private SipPhone otherRestcommPhone;
    private String otherRestcommContact = "sip:+15126008888@127.0.0.1:5091";

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@127.0.0.1:5092";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+15126008888@127.0.0.1:5070";

    // subaccountclient is a simple SIP Client. Will register with Restcomm
//    private SipStack subAccountClientSipStack;
//    private SipPhone subAccountClientPhone;
//    private String subAccountClientContact = "sip:subaccountclient@127.0.0.1:5093";

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

//    private String subAccountSid = "ACae6e420f425248d6a26948c17a9e2acg";
//    private String subAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("RestcommActingAsProxyTest1");
        tool2 = new SipStackTool("RestcommActingAsProxyTest2");
        tool3 = new SipStackTool("RestcommActingAsProxyTest3");
        tool4 = new SipStackTool("RestcommActingAsProxyTest4");
//        tool5 = new SipStackTool("DialActionTest5");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        otherRestcommSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        otherRestcommPhone = otherRestcommSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, otherRestcommContact);

        henriqueSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        henriquePhone = henriqueSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, henriqueContact);
//
        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);
//
//        subAccountClientSipStack = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5093", "127.0.0.1:5080");
//        subAccountClientPhone = subAccountClientSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, subAccountClientContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (otherRestcommSipStack != null) {
            otherRestcommSipStack.dispose();
        }
        if (otherRestcommPhone != null) {
            otherRestcommPhone.dispose();
        }

        if (henriqueSipStack != null) {
            henriqueSipStack.dispose();
        }
        if (henriquePhone != null) {
            henriquePhone.dispose();
        }

//        if (georgePhone != null) {
//            georgePhone.dispose();
//        }
//        if (georgeSipStack != null) {
//            georgeSipStack.dispose();
//        }
//
//        if (subAccountClientPhone != null) {
//            subAccountClientPhone.dispose();
//        }
//        if (subAccountClientSipStack != null) {
//            subAccountClientSipStack.dispose();
//        }
        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    @Test
    public void testDialNumberToProxyFromClient() throws ParseException, InterruptedException, MalformedURLException, SdpParseException {
        SipCall otherRestcommCall = otherRestcommPhone.createSipCall();
        otherRestcommCall.listenForIncomingCall();

        // Bob (5090) -> Restcomm (5080) -> proxy out to otherRestcomm (5091)
        // otherRestcomm (5091) 200 OK -> Restcomm (5080) -> Bob (5090)


        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:+15126008888@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(otherRestcommCall.waitForIncomingCall(10000));
        SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(otherRestcommCall.getLastReceivedRequest().getRequestEvent().getRequest().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, body2, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int response = bobCall.getLastReceivedResponse().getStatusCode();
        while (response != Response.OK) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            response = bobCall.getLastReceivedResponse().getStatusCode();
        }
        sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(bobCall.getLastReceivedResponse().getResponseEvent().getResponse().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(bobCall.sendInviteOkAck());

        assertTrue(otherRestcommCall.waitForAck(50000));

        assertEquals(2, MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveIncomingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveOutgoingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(2, MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken));

        Thread.sleep(1000);

        otherRestcommCall.listenForDisconnect();
        assertTrue(bobCall.disconnect());
        assertTrue(otherRestcommCall.waitForDisconnect(5000));
        otherRestcommCall.respondToDisconnect();

        Thread.sleep(10000);

//        logger.info("About to check the Requests");
//        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
//        assertTrue(requests.size() == 1);
//        //        requests.get(0).g;
//        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
//        List<String> params = Arrays.asList(requestBody.split("&"));
//        String callSid = "";
//        for (String param : params) {
//            if (param.contains("CallSid")) {
//                callSid = param.split("=")[1];
//            }
//        }
//        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
//        JsonObject jsonObj = cdr.getAsJsonObject();
//        String status = jsonObj.get("status").getAsString();
//        logger.info("Status: "+status);
//        assertTrue(status.equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void testDialNumberToProxyFromClientThroughLB() throws ParseException, InterruptedException, MalformedURLException, SdpParseException {
        SipCall otherRestcommCall = otherRestcommPhone.createSipCall();
        otherRestcommCall.listenForIncomingCall();

        // Bob (5090) -> Restcomm (5080) -> proxy out to otherRestcomm (5091)
        // otherRestcomm (5091) 200 OK -> Restcomm (5080) -> Bob (5090)


        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        ArrayList<String> additionalHeaders = new ArrayList<String>();
        additionalHeaders.add("X-Sip-Balancer-InitialRemoteAddr: 10.0.0.26");
        additionalHeaders.add("X-Sip-Balancer-InitialRemotePort: 5090");
        bobCall.initiateOutgoingCall(bobContact, "sip:+15126008888@127.0.0.1:5080", null, body, "application", "sdp", additionalHeaders, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(otherRestcommCall.waitForIncomingCall(10000));
        SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(otherRestcommCall.getLastReceivedRequest().getRequestEvent().getRequest().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        assertTrue(otherRestcommCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, body2, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int response = bobCall.getLastReceivedResponse().getStatusCode();
        while (response != Response.OK) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            response = bobCall.getLastReceivedResponse().getStatusCode();
        }
        sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(bobCall.getLastReceivedResponse().getResponseEvent().getResponse().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().equalsIgnoreCase("127.0.0.1"));
        assertTrue(bobCall.sendInviteOkAck());

        assertTrue(otherRestcommCall.waitForAck(50000));

        assertEquals(2, MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveIncomingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveOutgoingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(2, MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken));

        Thread.sleep(1000);

        otherRestcommCall.listenForDisconnect();
        assertTrue(bobCall.disconnect());
        assertTrue(otherRestcommCall.waitForDisconnect(5000));
        otherRestcommCall.respondToDisconnect();

        Thread.sleep(10000);

//        logger.info("About to check the Requests");
//        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
//        assertTrue(requests.size() == 1);
//        //        requests.get(0).g;
//        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
//        List<String> params = Arrays.asList(requestBody.split("&"));
//        String callSid = "";
//        for (String param : params) {
//            if (param.contains("CallSid")) {
//                callSid = param.split("=")[1];
//            }
//        }
//        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
//        JsonObject jsonObj = cdr.getAsJsonObject();
//        String status = jsonObj.get("status").getAsString();
//        logger.info("Status: "+status);
//        assertTrue(status.equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void testDialNumberToProxyFromNonClient() throws ParseException, InterruptedException, MalformedURLException, SdpParseException {
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall henriqueCall = henriquePhone.createSipCall();
        henriqueCall.initiateOutgoingCall(henriqueContact, "sip:+15126008888@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(henriqueCall);

        assertTrue(georgeCall.waitForIncomingCall(10000));
        SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(georgeCall.getLastReceivedRequest().getRequestEvent().getRequest().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().matches("192.168.11.99"));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, body2, "application", "sdp",
                null, null));

        assertTrue(henriqueCall.waitOutgoingCallResponse(5 * 1000));
        int response = henriqueCall.getLastReceivedResponse().getStatusCode();
        while (response != Response.OK) {
            assertTrue(henriqueCall.waitOutgoingCallResponse(5 * 1000));
            response = henriqueCall.getLastReceivedResponse().getStatusCode();
        }
        sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(henriqueCall.getLastReceivedResponse().getResponseEvent().getResponse().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().matches("192.168.11.99"));
        assertTrue(henriqueCall.sendInviteOkAck());

        assertTrue(georgeCall.waitForAck(50000));

        assertEquals(2, MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveIncomingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveOutgoingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(2, MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken));

        Thread.sleep(1000);

        georgeCall.listenForDisconnect();
        assertTrue(henriqueCall.disconnect());
        assertTrue(georgeCall.waitForDisconnect(5000));
        georgeCall.respondToDisconnect();

        Thread.sleep(10000);

//        logger.info("About to check the Requests");
//        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
//        assertTrue(requests.size() == 1);
//        //        requests.get(0).g;
//        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
//        List<String> params = Arrays.asList(requestBody.split("&"));
//        String callSid = "";
//        for (String param : params) {
//            if (param.contains("CallSid")) {
//                callSid = param.split("=")[1];
//            }
//        }
//        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
//        JsonObject jsonObj = cdr.getAsJsonObject();
//        String status = jsonObj.get("status").getAsString();
//        logger.info("Status: "+status);
//        assertTrue(status.equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void testDialNumberToProxyFromNonClientThroughLB() throws ParseException, InterruptedException, MalformedURLException, SdpParseException {
        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall henriqueCall = henriquePhone.createSipCall();
        ArrayList<String> additionalHeaders = new ArrayList<String>();
        additionalHeaders.add("X-Sip-Balancer-InitialRemoteAddr: 10.0.0.26");
        additionalHeaders.add("X-Sip-Balancer-InitialRemotePort: 5090");
        henriqueCall.initiateOutgoingCall(henriqueContact, "sip:+15126008888@127.0.0.1:5080", null, body, "application", "sdp", additionalHeaders, null);
        assertLastOperationSuccess(henriqueCall);

        assertTrue(georgeCall.waitForIncomingCall(10000));
        SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(georgeCall.getLastReceivedRequest().getRequestEvent().getRequest().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().matches("192.168.11.99"));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, body2, "application", "sdp",
                null, null));

        assertTrue(henriqueCall.waitOutgoingCallResponse(5 * 1000));
        int response = henriqueCall.getLastReceivedResponse().getStatusCode();
        while (response != Response.OK) {
            assertTrue(henriqueCall.waitOutgoingCallResponse(5 * 1000));
            response = henriqueCall.getLastReceivedResponse().getStatusCode();
        }
        sessionDescription = SdpFactory.getInstance().createSessionDescription(new String(henriqueCall.getLastReceivedResponse().getResponseEvent().getResponse().getRawContent()));
        assertTrue(sessionDescription.getConnection().getAddress().matches("192.168.11.99"));
        assertTrue(henriqueCall.sendInviteOkAck());

        assertTrue(georgeCall.waitForAck(50000));

        assertEquals(2, MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveIncomingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(1, MonitoringServiceTool.getInstance().getLiveOutgoingCallStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken));
        assertEquals(2, MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken));

        Thread.sleep(1000);

        georgeCall.listenForDisconnect();
        assertTrue(henriqueCall.disconnect());
        assertTrue(georgeCall.waitForDisconnect(5000));
        georgeCall.respondToDisconnect();

        Thread.sleep(10000);

//        logger.info("About to check the Requests");
//        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
//        assertTrue(requests.size() == 1);
//        //        requests.get(0).g;
//        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
//        List<String> params = Arrays.asList(requestBody.split("&"));
//        String callSid = "";
//        for (String param : params) {
//            if (param.contains("CallSid")) {
//                callSid = param.split("=")[1];
//            }
//        }
//        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
//        JsonObject jsonObj = cdr.getAsJsonObject();
//        String status = jsonObj.get("status").getAsString();
//        logger.info("Status: "+status);
//        assertTrue(status.equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }
}

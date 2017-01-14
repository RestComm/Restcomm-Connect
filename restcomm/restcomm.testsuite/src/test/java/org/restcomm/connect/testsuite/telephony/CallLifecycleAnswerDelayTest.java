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

package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
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
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for Dial Action attribute. Reference: https://www.twilio.com/docs/api/twiml/dial#attributes-action The 'action'
 * attribute takes a URL as an argument. When the dialed call ends, Restcomm will make a GET or POST request to this URL
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class CallLifecycleAnswerDelayTest {

    private final static Logger logger = Logger.getLogger(CallLifecycleAnswerDelayTest.class.getName());

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

    //Dial Action URL: http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@127.0.0.1:8080/restcomm/2012-04-24/DialAction Method: POST
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

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@127.0.0.1:5092";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("CallLifecycleDelayAnswerTest1");
        tool2 = new SipStackTool("CallLifecycleDelayAnswerTest2");
        tool3 = new SipStackTool("CallLifecycleDelayAnswerTest3");
        tool4 = new SipStackTool("CallLifecycleDelayAnswerTest");
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
        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    @Test
    public void testDialCancelBeforeDialingClientAliceAfterRinging() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialAliceRcml)));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);

        SipTransaction cancelTransaction = bobCall.sendCancel();
        assertNotNull(cancelTransaction);
        assertTrue(bobCall.waitForCancelResponse(cancelTransaction,5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.OK);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode()==Response.REQUEST_TERMINATED);

//        Thread.sleep(15000);
//
//        int liveCalls = MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
//        logger.info("LiveCalls: "+liveCalls);
//        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
//        logger.info("LiveCallsArraySize: "+liveCallsArraySize);
//        assertTrue(liveCalls==0);
//        assertTrue(liveCallsArraySize==0);

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        String status = jsonObj.get("status").getAsString();
        logger.info("Status: "+status);
        assertTrue(status.equalsIgnoreCase("canceled"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    private String dialAliceRcml = "<Response><Dial><Client>alice</Client></Dial></Response>";
    @Test
    public void testDialClientAlice() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialAliceRcml)));

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);
        logger.info("Last response: "+response);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
            logger.info("Last response: "+bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(5000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.TRYING, "Alice-Trying", 3600));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Alice-Ringing", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "Alice-OK", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        assertTrue(aliceCall.waitForAck(5000));

        Thread.sleep(1000);
        int liveCalls = MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveIncomingCalls = MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveOutgoingCalls = MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertTrue(liveCalls==2);
        assertTrue(liveIncomingCalls==1);
        assertTrue(liveOutgoingCalls==1);
        assertTrue(liveCallsArraySize==2);


        Thread.sleep(3000);
        bobCall.listenForDisconnect();

        assertTrue(aliceCall.disconnect());
        Thread.sleep(500);
        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));

        JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertNotNull(metrics);
        liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
        logger.info("LiveCalls: "+liveCalls);
        liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
        logger.info("LiveCallsArraySize: "+liveCallsArraySize);
        assertTrue(liveCalls==0);
        assertTrue(liveCallsArraySize==0);
        int maxConcurrentCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentCalls").getAsInt();
        int maxConcurrentIncomingCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentIncomingCalls").getAsInt();
        int maxConcurrentOutgoingCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentIncomingCalls").getAsInt();
        assertTrue(maxConcurrentCalls==2);
        assertTrue(maxConcurrentIncomingCalls==1);
        assertTrue(maxConcurrentOutgoingCalls==1);
    }
    
    private String dialNumberRcml = "<Response><Dial><Number>+131313</Number></Dial></Response>";

    @Test
    public void testDialNumberPstn() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        Thread.sleep(3000);
        bobCall.listenForDisconnect();

        assertTrue(georgeCall.disconnect());
        Thread.sleep(500);
        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void testDialNumberPstnRegisteredClientTimesOutCallDisconnects() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential c = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(c);

        // Create outgoing call with first phone
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(aliceCall);
        assertTrue(aliceCall.waitForAuthorisation(5000));
        assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
        final int response = aliceCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall.sendInviteOkAck());
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        aliceCall.disposeNoBye();
        georgeCall.listenForDisconnect();

        Thread.sleep(50000);
        assertTrue(georgeCall.waitForDisconnect(500000));

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void testDialNumberPstnForbidden() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        assertTrue(georgeCall.sendIncomingCallResponse(Response.FORBIDDEN, "George-Forbidden", 3600));
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.FORBIDDEN, bobCall.getLastReceivedResponse().getStatusCode());

        JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
        int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
        assertTrue(liveCalls==0);
        assertTrue(liveCallsArraySize==0);

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));

        metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertNotNull(metrics);
        liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
        logger.info("LiveCalls: "+liveCalls);
        liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
        logger.info("LiveCallsArraySize: "+liveCallsArraySize);
        assertTrue(liveCalls==0);
        assertTrue(liveCallsArraySize==0);
        int maxConcurrentCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentCalls").getAsInt();
        int maxConcurrentIncomingCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentIncomingCalls").getAsInt();
        int maxConcurrentOutgoingCalls = metrics.getAsJsonObject("Metrics").get("MaximumConcurrentIncomingCalls").getAsInt();
        assertTrue(maxConcurrentCalls==2);
        assertTrue(maxConcurrentIncomingCalls==1);
        assertTrue(maxConcurrentOutgoingCalls==1);
    }

    @Test
    public void testDialNumberPstnBobDisconnects() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();

        assertTrue(bobCall.disconnect());
        Thread.sleep(500);
        assertTrue(georgeCall.waitForDisconnect(5000));
        assertTrue(georgeCall.respondToDisconnect());

        Thread.sleep(1000);

        JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertNotNull(metrics);
        int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
        logger.info("LiveCalls: "+liveCalls);
        int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
        logger.info("LiveCallsArraySize: "+liveCallsArraySize);
        assertTrue(liveCalls==0);
        assertTrue(liveCallsArraySize==0);

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }
    
    
    @Test
    public void testDialNumberPstn_404NotHere() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();

        assertTrue(bobCall.disconnect());
        Thread.sleep(500);
        assertTrue(georgeCall.waitForDisconnect(5000));
        assertTrue(georgeCall.respondToDisconnect(Response.NOT_FOUND, "Not Here"));
        georgeCall.disposeNoBye();

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        bobPhone.dispose();
        bobPhone = null;
        georgePhone.dispose();
        georgePhone = null;
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    private String dialNumberRcmlWithTimeout = "<Response><Dial timeout=\"10\"><Number>+131313</Number></Dial></Response>";
    
    @Test
    public void testDialNumberPstnNoAnswer() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcmlWithTimeout)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        assertTrue(georgeCall.listenForCancel());
        SipTransaction cancelTransaction = georgeCall.waitForCancel(100 * 1000);
        assertNotNull(cancelTransaction);
        assertTrue(georgeCall.respondToCancel(cancelTransaction, Response.OK, "George-OK-2-Cancel", 3600));
        
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.REQUEST_TIMEOUT, bobCall.getLastReceivedResponse().getStatusCode());

        Thread.sleep(1000);
        
        assertTrue(bobCall.disconnect());

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        String status = jsonObj.get("status").getAsString();
        logger.info("Status: "+status);
        assertTrue(status.equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }
    
    @Test
    public void testDialNumberPstn_500ServerInternalError() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }
        
        Thread.sleep(200);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.SERVER_INTERNAL_ERROR, "George-Server Internal Error", 3600));
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.SERVER_INTERNAL_ERROR, bobCall.getLastReceivedResponse().getStatusCode());

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }
    
    @Test
    public void testDialNumberPstn_BusyHere() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }
        
        Thread.sleep(200);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveIncomingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveOutgoingCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.BUSY_HERE, "George-Busy Here", 3600));
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.BUSY_HERE, bobCall.getLastReceivedResponse().getStatusCode());

        Thread.sleep(10000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    private String dialAliceRcmlInvalidRCML = "%%<Response><Dial><Client>alice</Client></Dial></Response>";

    @Test
    public void testDialClientAlice_InvalidRCML() throws ParseException, InterruptedException, MalformedURLException {

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialAliceRcmlInvalidRCML)));

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.SERVER_INTERNAL_ERROR, bobCall.getLastReceivedResponse().getStatusCode());
        String reason = bobCall.getLastReceivedResponse().getMessage().getHeader("Reason").toString();
        assertTrue(bobCall.getLastReceivedResponse().getMessage().getHeader("Reason").toString().contains("Problem_to_parse_downloaded_RCML"));


        Thread.sleep(5000);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

        Thread.sleep(5000);

        logger.info("About to check the Requests");
        List<LoggedRequest> requests = findAll(getRequestedFor(urlPathMatching("/1111")));
        assertTrue(requests.size() == 1);
        //        requests.get(0).g;
        String requestBody = new URL(requests.get(0).getAbsoluteUrl()).getQuery();// .getQuery();// .getBodyAsString();
        List<String> params = Arrays.asList(requestBody.split("&"));
        String callSid = "";
        for (String param : params) {
            if (param.contains("CallSid")) {
                callSid = param.split("=")[1];
            }
        }
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, callSid);
        JsonObject jsonObj = cdr.getAsJsonObject();
        assertTrue(jsonObj.get("status").getAsString().equalsIgnoreCase("completed"));
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Deployment(name = "CallLifecycleDelayAnswerTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm_calllifecycle-delay.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_callLifecycleTest", "data/hsql/restcomm.script");
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}

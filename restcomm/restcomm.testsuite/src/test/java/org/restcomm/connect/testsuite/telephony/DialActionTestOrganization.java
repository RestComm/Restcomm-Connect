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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.sip.Dialog;
import javax.sip.address.SipURI;
import javax.sip.header.Header;
import javax.sip.message.Response;

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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Test for Dial Action attribute for organization
 *
 * @author maria
 *
 */
@RunWith(Arquillian.class)
public class DialActionTestOrganization {

    private final static Logger logger = Logger.getLogger(DialActionTestOrganization.class.getName());

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

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@testdomain2.restcomm.com";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@testdomain2.restcomm.com";

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@testdomain2.restcomm.com";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@testdomain2.restcomm.com";

    private String dialClientWithActionUrl = "sip:+12223334455@testdomain2.restcomm.com"; // Application: dial-client-entry_wActionUrl.xml

    private String testDomain2adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acg";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    
	final static String testDomain2 = "testdomain2.restcomm.com";
    final String testDomain2ExpectedIP = "127.0.0.1";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialActionTest1");
        tool2 = new SipStackTool("DialActionTest2");
        tool3 = new SipStackTool("DialActionTest3");
        tool4 = new SipStackTool("DialActionTest4");
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

    @Test
    public void testClientsCallEachOther() throws ParseException, InterruptedException {

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        assertTrue(alicePhone.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        assertTrue(bobPhone.register(uri, "bob", "1234", "sip:bob@127.0.0.1:5090", 3600, 3600));

        Credential c = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(c);

        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        Thread.sleep(1000);

        // Alice initiates a call to Bob
        long startTime = System.currentTimeMillis();
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingCall(aliceContact, bobContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(aliceCall);
        assertTrue(aliceCall.waitForAuthorisation(3000));

        assertTrue(bobCall.waitForIncomingCall(5000));
        assertTrue(bobCall.sendIncomingCallResponse(100, "Trying-Bob", 1800));
        assertTrue(bobCall.sendIncomingCallResponse(180, "Ringing-Bob", 1800));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
        int responseAlice = aliceCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseAlice == Response.TRYING || responseAlice == Response.RINGING);

        Dialog aliceDialog = null;

        if (responseAlice == Response.TRYING) {
            assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall.getLastReceivedResponse().getStatusCode());
            aliceDialog = aliceCall.getDialog();
        }

        assertTrue(aliceCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall.getDialog().equals(aliceDialog));
        aliceCall.sendInviteOkAck();
        assertTrue(aliceCall.getDialog().equals(aliceDialog));

        assertTrue(!(aliceCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(bobCall.waitForAck(3000));

        //Talk time ~ 3sec
        Thread.sleep(3000);
        bobCall.listenForDisconnect();
        assertTrue(aliceCall.disconnect());

        assertTrue(bobCall.waitForDisconnect(5 * 1000));
        assertTrue(bobCall.respondToDisconnect());
        long endTime   = System.currentTimeMillis();

        double totalTime = (endTime - startTime)/1000.0;
        assertTrue(3.0 <= totalTime);
        assertTrue(totalTime <= 4.0);

        Thread.sleep(3000);

        //Check CDR
        JsonObject cdrs = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), testDomain2adminAccountSid, adminAuthToken);
        assertNotNull(cdrs);
        JsonArray cdrsArray = cdrs.get("calls").getAsJsonArray();
        System.out.println("cdrsArray.size(): "+cdrsArray.size());
        assertTrue(cdrsArray.size() == 1);

    }

    @Test
    public void testDialActionAliceAnswers() throws ParseException, InterruptedException, UnknownHostException {

       stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));
        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialClientWithActionUrl, null, body, "application", "sdp", null, null);
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
            Thread.sleep(50 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        Thread.sleep(3000);

        logger.info("About to check the DialAction Requests");
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/DialAction.*")));
        assertEquals(1, requests.size());
        String requestBody = requests.get(0).getBodyAsString();
        String[] params = requestBody.split("&");
        assertTrue(requestBody.contains("DialCallStatus=completed"));
        assertTrue(requestBody.contains("To=%2B12223334455"));
        assertTrue(requestBody.contains("From=bob"));
        assertTrue(requestBody.contains("DialCallDuration=3"));
        Iterator iter = Arrays.asList(params).iterator();
        String dialCallSid = null;
        while (iter.hasNext()) {
            String param = (String) iter.next();
            if (param.startsWith("DialCallSid")) {
                dialCallSid = param.split("=")[1];
                break;
            }
        }
        assertNotNull(dialCallSid);
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), testDomain2adminAccountSid, adminAuthToken, dialCallSid);
        assertNotNull(cdr);
    }

    @Test
    public void testDialActionHangupWithLCM() throws Exception {

        stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialClientWithActionUrl, null, body, "application", "sdp", null, null);
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

        String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
        bobCall.listenForDisconnect();

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        aliceCall.listenForCancel();

        logger.info("About to execute LCM to hangup the call");
        RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(),testDomain2adminAccountSid,adminAuthToken,callSid,"completed", null);

        assertTrue(bobCall.waitForDisconnect(50 * 1000));
        assertTrue(bobCall.respondToDisconnect());

//        logger.info("&&&&&&&&&&&&&&&&&&&&&& Alice about to listen for CANCEL");
//        SipTransaction sipTransaction = aliceCall.waitForCancel(50 * 1000);
//        assertNotNull(sipTransaction);
//        aliceCall.respondToCancel(sipTransaction,200,"Alice-OK-To-Cancel",3600);
//        aliceCall.respondToCancel(sipTransaction,487,"Alice-Request-Terminated",3600);

        Thread.sleep(10 * 1000);

        logger.info("About to check the DialAction Requests");
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/DialAction.*")));
        assertEquals(1, requests.size());
        String requestBody = requests.get(0).getBodyAsString();
        String[] params = requestBody.split("&");
        assertTrue(requestBody.contains("DialCallStatus=completed"));
        assertTrue(requestBody.contains("To=%2B12223334455"));
        assertTrue(requestBody.contains("From=bob"));
        assertTrue(requestBody.contains("DialCallDuration=0"));
        assertTrue(requestBody.contains("CallStatus=completed"));
        Iterator iter = Arrays.asList(params).iterator();
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

    @Deployment(name = "DialAction", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm.script_dialActionTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}

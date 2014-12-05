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

package org.mobicents.servlet.restcomm.telephony;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.http.RestcommCallsTool;
import org.mobicents.servlet.restcomm.telephony.RestResources.DialActionResources;

import com.google.gson.JsonObject;

/**
 * Test for Dial Action attribute. Reference: https://www.twilio.com/docs/api/twiml/dial#attributes-action The 'action'
 * attribute takes a URL as an argument. When the dialed call ends, Restcomm will make a GET or POST request to this URL
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class DialActionTest {

    private final static Logger logger = Logger.getLogger(DialActionTest.class.getName());

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

    private String dialClientWithActionUrl = "sip:+12223334455@127.0.0.1:5080";
    
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

        DialActionResources.resetData();
        
        Thread.sleep(1000);
    }

    @Test
    public void testDialActionInvalidCall() throws ParseException, InterruptedException {

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
        bobCall.listenForDisconnect();

        assertTrue(bobCall.waitForDisconnect(40 * 1000));
        assertTrue(bobCall.respondToDisconnect());

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();

        assertTrue(data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("failed"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("0"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));

        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }

    @Test
    public void testDialActionAliceAnswers() throws ParseException, InterruptedException {

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

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
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();

        assertTrue(!data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("completed"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("3"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));
        
        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }

    @Test
    public void testDialActionAliceAnswersAliceHangup() throws ParseException, InterruptedException {

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

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
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();

        assertTrue(!data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("completed"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("3"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));
        
        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }
    
    @Test
    public void testDialActionAliceAnswersBobDisconnects() throws ParseException, InterruptedException {

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

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
        bobCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();
        
        assertNotNull(data);
        assertTrue(!data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("completed"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("3"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));
        
        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }
    
    @Test
    public void testDialActionAliceNOAnswer() throws ParseException, InterruptedException {

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

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
        assertTrue(aliceCall.listenForCancel());
        SipTransaction cancelTransaction = aliceCall.waitForCancel(30 * 1000);
        assertNotNull(cancelTransaction);
        assertTrue(aliceCall.respondToCancel(cancelTransaction, Response.OK, "Alice-OK-2-Cancel", 3600));

        Thread.sleep(3700);

        assertTrue(bobCall.disconnect());

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();

        assertTrue(!data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("no_answer"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("3"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));
        
        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }

    @Test
    public void testDialActionAliceBusy() throws ParseException, InterruptedException {

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

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
        assertTrue(aliceCall.sendIncomingCallResponse(Response.BUSY_HERE, "Busy-Alice", 3600));
        assertTrue(aliceCall.waitForAck(50 * 1000));


        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        assertTrue(bobCall.respondToDisconnect());

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
        
        MultivaluedMap<String, String> data = DialActionResources.getPostRequestData();
        
        assertTrue(!data.getFirst("DialCallSid").equalsIgnoreCase(""));
        assertTrue(data.getFirst("RecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("PublicRecordingUrl").equalsIgnoreCase(""));
        assertTrue(data.getFirst("DialCallStatus").equalsIgnoreCase("busy"));
        assertTrue(data.getFirst("DialCallDuration").equalsIgnoreCase("0"));

        assertTrue(data.getFirst("To").equalsIgnoreCase("+12223334455"));
        assertTrue(data.getFirst("Direction").equalsIgnoreCase("inbound"));
        assertTrue(data.getFirst("ApiVersion").equalsIgnoreCase("2012-04-24"));
        assertTrue(data.getFirst("From").equalsIgnoreCase("bob"));

        assertTrue(data.containsKey("AccountSid"));
        assertTrue(data.containsKey("CallStatus"));
        assertTrue(data.containsKey("CallerName"));
        assertTrue(data.containsKey("ForwardedFrom"));
        assertTrue(data.containsKey("CallSid"));
        
        String sid = data.getFirst("DialCallSid");
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken, sid);
        assertNotNull(cdr);
    }

    @Deployment(name = "DialAction", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
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
        archive.addAsWebInfResource("restcomm.script_dialActionTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        archive.addPackage("org.mobicents.servlet.restcomm.telephony.RestResources");
        logger.info("Packaged Test App");
        return archive;
    }

}

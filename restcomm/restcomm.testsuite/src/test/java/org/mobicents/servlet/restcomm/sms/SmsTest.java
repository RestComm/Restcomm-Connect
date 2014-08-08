/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.sms;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.header.SIPHeader;

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.sip.address.SipURI;
import javax.sip.header.Header;
import javax.sip.message.Request;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class SmsTest {

    private final static Logger logger = Logger.getLogger(SmsTest.class);
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
    
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";
    
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";
    
    private String dialSendSMS = "sip:+12223334444@127.0.0.1:5080";
    private String dialSendSMS2 = "sip:+12223334445@127.0.0.1:5080";
    private String dialSendSMS2_Greek = "sip:+12223334447@127.0.0.1:5080";
    private String dialSendSMS2_Greek_Huge = "sip:+12223334448@127.0.0.1:5080";
    private String dialSendSMS3 = "sip:+12223334446@127.0.0.1:5080";
    private String dialSendSMSwithCustomHeaders = "sip:+12223334449@127.0.0.1:5080";

    private String greekHugeMessage = "Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα "
            + "Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα "
            + "Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα "
            + "Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα Καλημερα "
            + "Καλημερα Καλημερα Καλημερα";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("SmsTest1");
        tool2 = new SipStackTool("SmsTest2");
    }
    
    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
        
        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);
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
    }
    
    @Test
    public void testAliceActsAsSMSGatewayAndReceivesSMS() throws ParseException {
        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();

        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, dialSendSMS, null, body, "application", "sdp", null, null);
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

        assertTrue(aliceCall.waitForMessage(5 * 1000));
        String msgReceived = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue("Hello World!".equals(msgReceived));
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
        
        assertTrue(bobCall.waitForDisconnect(40 * 1000));
        assertTrue(bobCall.respondToDisconnect());

        try {
            Thread.sleep(10 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    @Test
    public void TestIncomingSmsSendToClientAlice() throws ParseException, InterruptedException {
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingMessage(dialSendSMS2, null, "Hello from Bob!");
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.ACCEPTED);

        //Restcomm receives the SMS message from Bob, matches the DID with an RCML application, and executes it.
        //The new RCML application sends an SMS to Alice with body "Hello World!"
        
        assertTrue(aliceCall.waitForMessage(5 * 1000));
        String msgReceived = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue("Hello World!".equals(msgReceived));
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
    }
    
    @Test
    public void TestIncomingSmsSendToClientAliceGreekHugeMessage() throws ParseException, InterruptedException {
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingMessage(dialSendSMS2_Greek_Huge, null, greekHugeMessage);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.ACCEPTED);

        //Restcomm receives the SMS message from Bob, matches the DID with an RCML application, and executes it.
        //The new RCML application sends an SMS to Alice with body "Hello World!"
        
        assertTrue(aliceCall.waitForMessage(5 * 1000));
        String msgReceived = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue(greekHugeMessage.equals(msgReceived));
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
    }
    
    @Test
    public void TestIncomingSmsSendToClientAliceGreek() throws ParseException, InterruptedException {
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingMessage(dialSendSMS2_Greek, null, "Καλώς τον Γιώργο!");
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.ACCEPTED);

        //Restcomm receives the SMS message from Bob, matches the DID with an RCML application, and executes it.
        //The new RCML application sends an SMS to Alice with body "Hello World!"
        
        assertTrue(aliceCall.waitForMessage(5 * 1000));
        String msgReceived = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue("Καλώς τον Γιώργο!".equals(msgReceived));
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
    }
    
    @Test
    public void TestIncomingSmsSendToNumber1313() throws ParseException, InterruptedException {
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", "sip:1313@127.0.0.1:5091", 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingMessage(dialSendSMS3, null, "Hello from Bob!");
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.ACCEPTED);

        //Restcomm receives the SMS message from Bob, matches the DID with an RCML application, and executes it.
        //The new RCML application sends an SMS to Alice with body "Hello World!"
        
        assertTrue(aliceCall.waitForMessage(5 * 1000));
        String msgReceived = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue("Hello World!".equals(msgReceived));
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
    }

    @Test
    public void TestIncomingSmsSendToNumber1313WithCustomHeaders() throws ParseException, InterruptedException {
        String myFirstHeaderName = "X-Custom-Header-1";
        String myFirstHeaderValue = "X Custom Header Value 1";
        
        String mySecondHeaderName = "X-Custom-Header-2";
        String mySecondHeaderValue = "X Custom Header Value 2";
        
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", "sip:1313@127.0.0.1:5091", 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
        // Create outgoing call with first phone
        final SipCall bobCall = bobPhone.createSipCall();
        
        ArrayList<Header> additionalHeaders = new ArrayList<Header>();
        Header header1 = bobSipStack.getHeaderFactory().createHeader(myFirstHeaderName, myFirstHeaderValue);
        Header header2 = bobSipStack.getHeaderFactory().createHeader(mySecondHeaderName, mySecondHeaderValue);
        additionalHeaders.add(header1);
        additionalHeaders.add(header2);
        
        bobCall.initiateOutgoingMessage(bobContact, dialSendSMSwithCustomHeaders, null, additionalHeaders, null, "Hello from Bob!");
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.ACCEPTED);

        //Restcomm receives the SMS message from Bob, matches the DID with an RCML application, and executes it.
        //The new RCML application sends an SMS to Alice with body "Hello World!"
        
        assertTrue(aliceCall.waitForMessage(5 * 1000));
        Request receivedRequest = aliceCall.getLastReceivedMessageRequest();
        String msgReceived = new String(receivedRequest.getRawContent());
        assertTrue("Hello World!".equals(msgReceived));

        SIPHeader myFirstHeader = (SIPHeader)receivedRequest.getHeader(myFirstHeaderName);
        assertTrue(myFirstHeader != null);
        assertTrue(myFirstHeader.getValue().equalsIgnoreCase(myFirstHeaderValue));
        
        SIPHeader mySecondHeader = (SIPHeader)receivedRequest.getHeader(mySecondHeaderName);
        assertTrue(mySecondHeader != null);
        assertTrue(mySecondHeader.getHeaderValue().equalsIgnoreCase(mySecondHeaderValue));
        
        aliceCall.sendMessageResponse(200, "OK-From-Alice", 3600);
    }
    
    
    @Deployment(name = "SmsTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        restcommArchive.addClass(SmsRcmlServlet.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/web.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("web_for_SmsTest.xml", "web.xml");
        archive.addAsWebInfResource("restcomm_SmsTest.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_SmsTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("send-sms-test.xml");
        archive.addAsWebResource("send-sms-test-greek.xml");
        archive.addAsWebResource("send-sms-test-greek_huge.xml");
        archive.addAsWebResource("send-sms-test2.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        logger.info("Packaged Test App");
        return archive;
    }
}

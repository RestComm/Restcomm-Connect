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
package org.mobicents.servlet.restcomm.ussd;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import javax.sip.DialogState;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class UssdPullTest {

    private final static Logger logger = Logger.getLogger(UssdPullTest.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();
    
    private static SipStackTool tool1;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";
    
    private String ussdPullDid = "sip:5544@127.0.0.1:5080";
    private String ussdPullDid2 = "sip:*777#@127.0.0.1:5080";
    private String ussdPullWithCollectDID = "sip:5555@127.0.0.1:5080";
    private String ussdPullMessageLengthExceeds = "sip:5566@127.0.0.1:5080";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("UssdPullTest");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
            bobPhone = null;
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
            bobSipStack = null;
        }
    }

    @Test
    public void testUssdPull() {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullDid, null, UssdPullTestMessages.ussdClientRequestBody, "application", "vnd.3gpp.ussd+xml", null, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        } else {
            assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        }
        
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        
        assertTrue(bobCall.listenForDisconnect());
        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        String receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(UssdPullTestMessages.ussdRestcommResponse.trim()));
        bobCall.dispose();
    }

    @Test //USSD Pull to *777#
    public void testUssdPull2() {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullDid2, null, UssdPullTestMessages.ussdClientRequestBody, "application", "vnd.3gpp.ussd+xml", null, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        } else {
            assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        }
        
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        
        assertTrue(bobCall.listenForDisconnect());
        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        String receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(UssdPullTestMessages.ussdRestcommResponse.trim()));
        bobCall.dispose();
    }
    
    @Test
    public void testUssdPullWithCollect() throws InterruptedException, SipException, ParseException {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullWithCollectDID, null, UssdPullTestMessages.ussdClientRequestBodyForCollect, "application", "vnd.3gpp.ussd+xml", null, null);
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
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        String toTag = bobCall.getDialog().getLocalTag();
        Address bobAddress = bobPhone.getAddress();
        
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent);  
        assertTrue(requestEvent.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent, 200, "OK", toTag, bobAddress, 0);
        

        String receivedUssdPayload = new String(requestEvent.getRequest().getRawContent());
        System.out.println("receivedUssdPayload: \n"+receivedUssdPayload);
        System.out.println("UssdPullTestMessages.ussdRestcommResponseWithCollect: \n"+UssdPullTestMessages.ussdRestcommResponseWithCollect);
        assertTrue(receivedUssdPayload.equals(UssdPullTestMessages.ussdRestcommResponseWithCollect.trim()));
        
        Request infoResponse = requestEvent.getDialog().createRequest(Request.INFO);
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoResponse.setContent(UssdPullTestMessages.ussdClientResponseBodyToCollect.getBytes(), contentTypeHeader);

        bobPhone.sendRequestWithTransaction(infoResponse, false, requestEvent.getDialog());     

        assertTrue(bobCall.listenForDisconnect());        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(UssdPullTestMessages.ussdRestcommResponse.trim()));
        bobCall.dispose();
    }
    
    @Test
    public void testUssdPullWithCollect_DisconnectFromUser() throws InterruptedException, SipException, ParseException {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullWithCollectDID, null, UssdPullTestMessages.ussdClientRequestBodyForCollect, "application", "vnd.3gpp.ussd+xml", null, null);
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
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        String toTag = bobCall.getDialog().getLocalTag();
        Address bobAddress = bobPhone.getAddress();
        
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent);  
        assertTrue(requestEvent.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent, 200, "OK", toTag, bobAddress, 0);
        

        String receivedUssdPayload = new String(requestEvent.getRequest().getRawContent());
        System.out.println("receivedUssdPayload: \n"+receivedUssdPayload);
        System.out.println("UssdPullTestMessages.ussdRestcommResponseWithCollect: \n"+UssdPullTestMessages.ussdRestcommResponseWithCollect);
        assertTrue(receivedUssdPayload.equals(UssdPullTestMessages.ussdRestcommResponseWithCollect.trim()));
        
        bobCall.disconnect();
        bobCall.waitForAnswer(10000);
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == 200);
        
    }
    
    
    @Test @Ignore
    public void testUssdPullWithCollect_CancelFromUser() throws InterruptedException, SipException, ParseException {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullWithCollectDID, null, UssdPullTestMessages.ussdClientRequestBodyForCollect, "application", "vnd.3gpp.ussd+xml", null, null);
        assertLastOperationSuccess(bobCall);
        bobCall.waitOutgoingCallResponse(1000);
//        Thread.sleep(1000);
        
        SipTransaction cancelTransaction = bobCall.sendCancel();
        assertNotNull(cancelTransaction);

        bobCall.waitForCancel(5 * 1000);
        int lastResponseCode = bobCall.getLastReceivedResponse().getStatusCode();
        if (lastResponseCode != Response.REQUEST_TERMINATED){
            bobCall.waitOutgoingCallResponse(1000);
        }
        assertEquals(Response.REQUEST_TERMINATED, bobCall.getLastReceivedResponse().getStatusCode());
        
        Request ackRequest = cancelTransaction.getServerTransaction().getDialog().createRequest(Request.ACK);
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        ackRequest.setContent(null, contentTypeHeader);

        assertNotNull(bobPhone.sendRequestWithTransaction(ackRequest, false, cancelTransaction.getServerTransaction().getDialog()));
//        
//        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
//        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
//        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);
//
//        if (responseBob == Response.TRYING || responseBob == Response.RINGING) {
//            SipTransaction cancelTransaction = bobCall.sendCancel();
//            assertNotNull(cancelTransaction);
//            
//            bobCall.waitForAnswer(5 * 1000);
//            assertEquals(Response.REQUEST_TERMINATED, bobCall.getLastReceivedResponse().getStatusCode());
//            
//            Request ackRequest = cancelTransaction.getServerTransaction().getDialog().createRequest(Request.ACK);
//            ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
//            ackRequest.setContent(null, contentTypeHeader);
//
//            assertNotNull(bobPhone.sendRequestWithTransaction(ackRequest, false, cancelTransaction.getServerTransaction().getDialog()));
//        } 
        
    }

    @Test
    public void testUssdMessageLengthExceeds() {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullMessageLengthExceeds, null, UssdPullTestMessages.ussdClientRequestBodyForMessageLenghtExceeds, "application", "vnd.3gpp.ussd+xml", null, null);
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
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        
        assertTrue(bobCall.listenForDisconnect());
        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        String receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(UssdPullTestMessages.ussdRestcommResponseForMessageLengthExceeds.trim()));
        bobCall.dispose();
    }
    
    @Deployment(name = "UssdPullTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("org/mobicents/servlet/restcomm/ussd/restcomm.script_ussdPullTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("org/mobicents/servlet/restcomm/ussd/ussd-rcml.xml");
        archive.addAsWebResource("org/mobicents/servlet/restcomm/ussd/ussd-rcml-collect.xml");
        archive.addAsWebResource("org/mobicents/servlet/restcomm/ussd/ussd-rcml-character-limit-exceed.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}

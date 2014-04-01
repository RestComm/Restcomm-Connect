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
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
public class UssdPullTest {

    private final static Logger logger = Logger.getLogger(UssdPullTest.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();
    
    String ussdClientRequestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5544\"/>\n"
            + "</ussd-data>";

    String ussdClientRequestBodyForCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5555\"/>\n"
            + "</ussd-data>";
    
    String ussdRestcommResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"The information you requested is 1234567890\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>processUnstructuredSSRequest_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";

    String ussdRestcommResponseWithCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"Please press\n1 For option1\n2 For option2\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSRequest_Request</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";
    
    String ussdClientResponseBodyToCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"1\"/>\n"
            + "\t<anyExt>\n"
            + "\t\t<message-type>unstructuredSSRequest_Response</message-type>\n"
            + "\t</anyExt>\n"
            + "</ussd-data>";
    
    String ussdClientRequestBodyForMessageLenghtExceeds = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5566\"/>\n"
            + "</ussd-data>";
    
    String ussdRestcommResponseForMessageLengthExceeds = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"Error while preparing the response.\nMessage length exceeds the maximum.\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>processUnstructuredSSRequest_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";
    
    private static SipStackTool tool1;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";
    
    private String ussdPullDid = "sip:5544@127.0.0.1:5080";
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
        bobCall.initiateOutgoingCall(bobContact, ussdPullDid, null, ussdClientRequestBody, "application", "vnd.3gpp.ussd+xml", null, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());
        
        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);
        
        assertTrue(bobCall.listenForDisconnect());
        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        String receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(ussdRestcommResponse));
        bobCall.dispose();
    }

    @Test
    public void testUssdPullWithCollect() throws InterruptedException, SipException, ParseException {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullWithCollectDID, null, ussdClientRequestBodyForCollect, "application", "vnd.3gpp.ussd+xml", null, null);
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
        assertTrue(receivedUssdPayload.equals(ussdRestcommResponseWithCollect));
        
        Request infoResponse = requestEvent.getDialog().createRequest(Request.INFO);
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoResponse.setContent(ussdClientResponseBodyToCollect.getBytes(), contentTypeHeader);

        bobPhone.sendRequestWithTransaction(infoResponse, false, requestEvent.getDialog());     

        assertTrue(bobCall.listenForDisconnect());        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(ussdRestcommResponse));
        bobCall.dispose();
    }

    @Test
    public void testUssdMessageLengthExceeds() {
        final SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingCall(bobContact, ussdPullMessageLengthExceeds, null, ussdClientRequestBodyForMessageLenghtExceeds, "application", "vnd.3gpp.ussd+xml", null, null);
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
        assertTrue(receivedUssdPayload.equalsIgnoreCase(ussdRestcommResponseForMessageLengthExceeds));
        bobCall.dispose();
    }
    
    @Deployment(name = "UssdPullTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        final WebArchive archive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
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

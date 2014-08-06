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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@Ignore //Ignore because this will be running only manually against an EC2 instance
public class UssdPullTestEC2 {

    private final static Logger logger = Logger.getLogger(UssdPullTestEC2.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();
    
    private static SipStackTool tool1;

    private String ec2IPAddress = "54.198.164.153";
    private String myIpAddress = "192.168.1.70";
    
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@"+myIpAddress+":5090";
    
    private String ussdPullWithCollectDID = "sip:*123#@"+ec2IPAddress+":5080";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("UssdPullTest");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, myIpAddress, "5090", ec2IPAddress+":5080");
        bobPhone = bobSipStack.createSipPhone(ec2IPAddress, SipStack.PROTOCOL_UDP, 5080, bobContact);
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
        bobPhone.setLoopback(true);
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent);  
        assertTrue(requestEvent.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent, 200, "OK", toTag, bobAddress, 0);
        

        String receivedUssdPayload = new String(requestEvent.getRequest().getRawContent());
        System.out.println("receivedUssdPayload: \n"+receivedUssdPayload);
        System.out.println("UssdPullTestMessagesEC2.ussdRestcommResponseWithCollect: \n"+UssdPullTestMessagesEC2.ussdRestcommResponseWithCollect);
        assertTrue(receivedUssdPayload.equals(UssdPullTestMessagesEC2.ussdRestcommResponseWithCollect.trim()));
        
        Request infoResponse = requestEvent.getDialog().createRequest(Request.INFO);
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoResponse.setContent(UssdPullTestMessages.ussdClientResponseBodyToCollect.getBytes(), contentTypeHeader);

        bobPhone.sendRequestWithTransaction(infoResponse, false, requestEvent.getDialog());     

        assertTrue(bobCall.listenForDisconnect());        
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        receivedUssdPayload = new String(bye.getRawContent());
        assertTrue(receivedUssdPayload.equalsIgnoreCase(UssdPullTestMessagesEC2.ussdRestcommResponse.trim()));
        bobCall.dispose();
    }

}

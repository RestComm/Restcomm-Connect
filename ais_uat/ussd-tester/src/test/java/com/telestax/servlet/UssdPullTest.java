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
package com.telestax.servlet;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sip.DialogState;
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
import org.junit.Test;

/**
 * USSD Pull Test class, that uses SipUnit to connect to a running Restcomm instance.
 * 
 * UAT Spreadsheet - Test case 1.1
 * 
 * Prerequisites:
 * 
 * 1. Change SIPUNIT_ADDRESS will be the address of the laptop that this test is running.
 * 2. Change RESTCOMM_ADDRESS will be the address of the Restcomm server (if its not running in the same server as this test)
 * 3. Go to RVD and create a USSD Application
 * 4. Go to Admin UI and create a new NUMBER, set the VoiceURL to the USSD Application you created earlier.
 * 5. Run the test as Junit Test
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdPullTest {

    public static Logger logger = Logger.getLogger(UssdPullTest.class);

    //SipUnit IP Address + Port
    private String SIPUNIT_ADDRESS="192.168.1.70";
    private String SIPUNIT_PORT="5090";
    
    //Restcomm IP Address + Port
    private String RESTCOMM_ADDRESS="192.168.1.70";
    private String RESTCOMM_PORT="5080";
    
    //Restcomm USSD Push Application Number. You need to create an RVD USSD Application and use Admin UI to create a number to this application
    private String USSD_PUSH_NUMBER = "*5555";

    //SipUnit specific objects
    private static SipStackTool tool1;
    private SipStack bobSipStack;
    private SipPhone bobPhone;

    private String bobContact = "sip:bob@"+SIPUNIT_ADDRESS+":"+SIPUNIT_PORT;
    private String ussdPullDid = "sip:"+USSD_PUSH_NUMBER+"@"+RESTCOMM_ADDRESS+":"+RESTCOMM_PORT;

    @BeforeClass
    public static void beforeClass() {
        tool1 = new SipStackTool("UssdPullTest");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_TCP, SIPUNIT_ADDRESS, SIPUNIT_PORT, RESTCOMM_ADDRESS+":"+RESTCOMM_PORT);
        bobPhone = bobSipStack.createSipPhone(RESTCOMM_ADDRESS, SipStack.PROTOCOL_TCP, Integer.valueOf(RESTCOMM_PORT), bobContact);
        logger.info("SipUnit initialized");
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
    public void ussdPull() {
        String ussdMessage = UssdMessages.ussdClientRequestBodyGeorge;
        logger.info("About to send USSD Message : \n"+ussdMessage+"\n to Restcomm : "+RESTCOMM_ADDRESS+":"+RESTCOMM_PORT);
        final SipCall bobCall = bobPhone.createSipCall();

        //SipUnit will send the USSD Message to Restcomm
        bobCall.initiateOutgoingCall(bobContact, ussdPullDid, null, ussdMessage , "application", "vnd.3gpp.ussd+xml", null, null);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertTrue(bobCall.getLastReceivedResponse().getStatusCode() == Response.RINGING);
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());

        assertTrue(bobCall.getDialog().getState().getValue()==DialogState._CONFIRMED);

        assertTrue(bobCall.listenForDisconnect());

        //Restcomm USSD response will be received here 
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        bobCall.respondToDisconnect();
        SipRequest bye = bobCall.getLastReceivedRequest();
        String receivedUssdPayload = new String(bye.getRawContent());

        bobCall.dispose();

        logger.info("USSD Response from Restcomm is: "+receivedUssdPayload);
    }

}

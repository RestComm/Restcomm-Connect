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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * USSD Push Test class, that uses SipUnit.
 * 
 * UAT Spreadsheet - Test case 1.2
 * 
 * The test class uses RestcommUssdPushTool to make a RESTful request to a running Restcomm and create a USSD Push request to the SipUnit client
 * 
 * Prerequisites:
 * 
 * 1. Change SIPUNIT_ADDRESS will be the address of the laptop that this test is running.
 * 2. Change RESTCOMM_ADDRESS will be the address of the Restcomm server (if its not running in the same server as this test)
 * 3. Go to RVD and create a USSD Application. The name of the application will be used in this test (See "USSD_TEST_APPLICATION")
 * 4. You need to configure Restcomm restcomm.xml USSD configuration to use the SipUnit IP Address and port:
 *                      <ussd-gateway>
                            <ussd-gateway-uri>SIPUNIT_ADDRESS:SIPUNIT_PORT</ussd-gateway-uri>
                            <ussd-gateway-user></ussd-gateway-user>
                            <ussd-gateway-password></ussd-gateway-password>
                        </ussd-gateway>
 * 5. Run the test as Junit Test
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdPushTest {

    private final static Logger logger = Logger.getLogger(UssdPushTest.class.getName());

    //Default ID for Administrator user, no need to change it
    private String restcommUsername = "ACae6e420f425248d6a26948c17a9e2acf";
    //The Administrator hashed password, get it from Admin UI -> Dashboard
    private String restcommPassword = "53134d7a9914e2b47c8435ebdb50ded3";

    //SipUnit IP Address + Port
    private String SIPUNIT_ADDRESS="192.168.1.70";
    private String SIPUNIT_PORT="5090";
    
    //Restcomm IP Address + Port
    private String RESTCOMM_ADDRESS="192.168.1.70";
    private String RESTCOMM_PORT="5080";
    
    //RVD USSD Application
    private String USSD_TEST_APPLICATION="testUSSD";
    private String USSD_TEST_APPLICATION2="menuUssd";

    private String ussdContentSubType = "vnd.3gpp.ussd+xml";
    
    private static SipStackTool tool1;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@"+SIPUNIT_ADDRESS+":"+SIPUNIT_PORT;

    String deploymentUrl = "http://"+RESTCOMM_ADDRESS+":8080/restcomm/";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("CreateCalls1");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, SIPUNIT_ADDRESS, SIPUNIT_PORT, RESTCOMM_ADDRESS+":"+RESTCOMM_PORT);
        bobPhone = bobSipStack.createSipPhone(RESTCOMM_ADDRESS, SipStack.PROTOCOL_UDP, Integer.valueOf(RESTCOMM_PORT), bobContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }
    }
    
    @Test
    public void createUssdPushTestNotifyOnly() throws InterruptedException, SipException, ParseException {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        
        
        String from = "+15126002188";
        String to = "bob";
        String rcmlUrl = "http://"+RESTCOMM_ADDRESS+":8080/restcomm-rvd/services/apps/"+USSD_TEST_APPLICATION+"/controller";

        //Here we send the REST API Request to Restcomm
        JsonObject callResult = RestcommUssdPushTool.getInstance().createUssdPush(deploymentUrl.toString(), restcommUsername,
                restcommPassword, from, to, rcmlUrl);
        assertNotNull(callResult);

        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob", 3600));
        assertTrue(bobCall
                .sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, null, "application", ussdContentSubType, null, null));

        logger.info("USSD PUSH Response: \n"+receivedBody);

        bobCall.waitForAck(5000);
        
        Request infoRequest = bobCall.getDialog().createRequest(Request.INFO);
        
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoRequest.setContent(UssdPushTestMessages.ussdPushNotifyOnlyClientResponse.getBytes(), contentTypeHeader);

        SipTransaction sipTransaction = bobPhone.sendRequestWithTransaction(infoRequest, false, bobCall.getDialog());
        assertNotNull(sipTransaction);
        ResponseEvent response = (ResponseEvent) bobPhone.waitResponse(sipTransaction, 5000);
        assertNotNull(response);
        assertTrue(response.getResponse().getStatusCode() == Response.OK);
        
        Thread.sleep(3000);

        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(5000));        
        assertTrue(bobCall.respondToDisconnect());

        receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        logger.info("USSD PUSH Response: \n"+receivedBody);
    } 
    
    @Test
    public void createUssdPushTestCollect() throws InterruptedException, SipException, ParseException {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = "bob";
        String rcmlUrl = "http://"+RESTCOMM_ADDRESS+":8080/restcomm-rvd/services/apps/"+USSD_TEST_APPLICATION2+"/controller";

        JsonObject callResult = RestcommUssdPushTool.getInstance().createUssdPush(deploymentUrl.toString(), restcommUsername,
                restcommPassword, from, to, rcmlUrl);
        assertNotNull(callResult);

        //Wait for the USSD Push Tree Message 
        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob", 3600));
        assertTrue(bobCall
                .sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, null, "application", ussdContentSubType, null, null));
        
//        assertTrue(receivedBody.equals(UssdPushTestMessages.ussdPushCollectMessage));

        bobCall.waitForAck(5000);
        String toTag = bobCall.getDialog().getLocalTag();
        Address bobAddress = bobPhone.getAddress();
        
        //Prepare and send USSD Response "1" for the tree menu
        Request infoRequest = bobCall.getDialog().createRequest(Request.INFO);
        
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoRequest.setContent(UssdPushTestMessages.ussdPushCollectClientResponse.getBytes(), contentTypeHeader);

        SipTransaction sipTransaction = bobPhone.sendRequestWithTransaction(infoRequest, false, bobCall.getDialog());
        assertNotNull(sipTransaction);
        ResponseEvent response = (ResponseEvent) bobPhone.waitResponse(sipTransaction, 5000);
        assertNotNull(response);
        assertTrue(response.getResponse().getStatusCode() == Response.OK);
        
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent);  
        assertTrue(requestEvent.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent, 200, "OK", toTag, bobAddress, 0);
        

//        String receivedUssdPayload = new String(requestEvent.getRequest().getRawContent());
//        assertTrue(receivedUssdPayload.equals(UssdPushTestMessages.ussdPushNotifyOnlyMessage));

        //Prepare and send USSD Response "2" for the tree menu
        Request infoRequest2 = bobCall.getDialog().createRequest(Request.INFO);
        
        infoRequest2.setContent(UssdPushTestMessages.ussdPushCollectClientResponse2.getBytes(), contentTypeHeader);

        SipTransaction sipTransaction2 = bobPhone.sendRequestWithTransaction(infoRequest2, false, bobCall.getDialog());
        assertNotNull(sipTransaction2);
        ResponseEvent response2 = (ResponseEvent) bobPhone.waitResponse(sipTransaction2, 5000);
        assertNotNull(response2);
        assertTrue(response2.getResponse().getStatusCode() == Response.OK);
        
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent2 = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent2);  
        assertTrue(requestEvent2.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent2, 200, "OK", toTag, bobAddress, 0);

        
        //Prepare and send USSD Response "1" for the tree menu
        Request infoRequest3 = bobCall.getDialog().createRequest(Request.INFO);
        
        infoRequest3.setContent(UssdPushTestMessages.ussdPushCollectClientResponse2.getBytes(), contentTypeHeader);

        SipTransaction sipTransaction3 = bobPhone.sendRequestWithTransaction(infoRequest3, false, bobCall.getDialog());
        assertNotNull(sipTransaction3);
        ResponseEvent response3 = (ResponseEvent) bobPhone.waitResponse(sipTransaction3, 5000);
        assertNotNull(response3);
        assertTrue(response3.getResponse().getStatusCode() == Response.OK);
        
        assertTrue(bobPhone.listenRequestMessage());
        RequestEvent requestEvent3 = bobPhone.waitRequest(30*1000);
        
        assertNotNull(requestEvent3);  
        assertTrue(requestEvent3.getRequest().getMethod().equalsIgnoreCase("INFO"));
        bobPhone.sendReply(requestEvent3, 200, "OK", toTag, bobAddress, 0);
        
        
        
        //Prepare and send final "OK" USSD message to end the communication
        Request infoRequestFinal = bobCall.getDialog().createRequest(Request.INFO);
        
        infoRequestFinal.setContent(UssdPushTestMessages.ussdPushNotifyOnlyClientResponse.getBytes(), contentTypeHeader);

        SipTransaction sipTransactionFinal = bobPhone.sendRequestWithTransaction(infoRequestFinal, false, bobCall.getDialog());
        assertNotNull(sipTransactionFinal);
        ResponseEvent responseFinal = (ResponseEvent) bobPhone.waitResponse(sipTransactionFinal, 5000);
        assertNotNull(responseFinal);
        assertTrue(responseFinal.getResponse().getStatusCode() == Response.OK);
        
//        Thread.sleep(3000);

        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(5000));        
        assertTrue(bobCall.respondToDisconnect());

        receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
//        assertTrue(receivedBody.trim().equals(UssdPushTestMessages.ussdPushNotifyOnlyFinalResponse));
    }
    
}

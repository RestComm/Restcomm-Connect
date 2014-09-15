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
package org.mobicents.servlet.restcomm.sms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gov.nist.javax.sip.header.SIPHeader;

import java.net.URL;
import java.util.HashMap;

import javax.sip.message.Request;

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

import com.google.gson.JsonObject;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class SmsEndpointTest {
    private static Logger logger = Logger.getLogger(SmsEndpointTest.class);
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    
    private static SipStackTool tool1;
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:1213@127.0.0.1:5090";

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("SmsTest1");
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
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }
    }
    
    @Test
    public void sendSmsTest() {
        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForMessage();
        
        String from = "+15126002188";
        String to = "1213";
        String body = "Hello Bob!";
        
        JsonObject callResult = SmsEndpointTool.getInstance().createSms(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, body, null);
        assertNotNull(callResult);
        
        assertTrue(bobCall.waitForMessage(5000));
        Request messageRequest = bobCall.getLastReceivedMessageRequest();
        assertTrue(bobCall.sendMessageResponse(202, "Accepted", 3600));
        String messageReceived = new String(messageRequest.getRawContent());
        assertTrue(messageReceived.equals(body));
    }

    @Test
    public void sendSmsTestWithCustomHeaders() {
        String myFirstHeaderName = "X-Custom-Header-1";
        String myFirstHeaderValue = "X Custom Header Value 1";
        
        String mySecondHeaderName = "X-Custom-Header-2";
        String mySecondHeaderValue = "X Custom Header Value 2";
        
        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForMessage();
        
        String from = "+15126002188";
        String to = "1213";
        String body = "Hello Bob! This time I sent you the message and some additional headers.";
        HashMap<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put(myFirstHeaderName, myFirstHeaderValue);
        additionalHeaders.put(mySecondHeaderName, mySecondHeaderValue);
        
        JsonObject callResult = SmsEndpointTool.getInstance().createSms(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, body, additionalHeaders);
        assertNotNull(callResult);
        
        assertTrue(bobCall.waitForMessage(5000));
        Request messageRequest = bobCall.getLastReceivedMessageRequest();
        assertTrue(bobCall.sendMessageResponse(202, "Accepted", 3600));
        String messageReceived = new String(messageRequest.getRawContent());
        assertTrue(messageReceived.equals(body));
        
        SIPHeader myFirstHeader = (SIPHeader)messageRequest.getHeader(myFirstHeaderName);
        assertTrue(myFirstHeader != null);
        assertTrue(myFirstHeader.getValue().equalsIgnoreCase(myFirstHeaderValue));
        
        SIPHeader mySecondHeader = (SIPHeader)messageRequest.getHeader(mySecondHeaderName);
        assertTrue(mySecondHeader != null);
        assertTrue(mySecondHeader.getHeaderValue().equalsIgnoreCase(mySecondHeaderValue));
        
    }
    
    @Deployment(name = "LiveCallModificationTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm_for_SMSEndpointTest.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}

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
package org.restcomm.connect.testsuite.sms;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.WebArchiveUtil;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class SmsOutTest {

    private final static Logger logger = Logger.getLogger(SmsOutTest.class);
    private static final String version = Version.getVersion();

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    
    private static int mediaPort = NetworkPortAssigner.retrieveNextPortByFile();    
    
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private static String alicePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String aliceContact = "sip:alice@127.0.0.1:" + alicePort;
    
    private SipStack outboundDestSipStack;
    private SipPhone outboundDestPhone;
    private static String outboundPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String outboundDestContact = "sip:9898989@127.0.0.1:" + outboundPort;

    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;
    private static String restcommContact = "127.0.0.1:" + restcommPort;  
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("SmsTest1");
        tool2 = new SipStackTool("SmsTest2");
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort;
        }
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));
        }          
    }
    
    @Before
    public void before() throws Exception {
        aliceSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort, restcommContact);
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact);

        outboundDestSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", outboundPort, restcommContact);
        outboundDestPhone = outboundDestSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, outboundDestContact);
    }
    
    @After
    public void after() throws Exception {
        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }

        if (outboundDestSipStack != null) {
            outboundDestSipStack.dispose();
        }
        if (outboundDestPhone != null) {
            outboundDestPhone.dispose();
        }
    }
    
    @Test
    public void testSendSmsToInvalidNumber() throws ParseException, InterruptedException {
        SipCall outboundDestCall = outboundDestPhone.createSipCall();
        outboundDestCall.listenForMessage();
        
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        
        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);
        
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingMessage(aliceContact, "sip:9898989@" + restcommContact, null, null, null, "Test message");
        assertTrue(aliceCall.waitForAuthorisation(5000));
        assertTrue(aliceCall.waitOutgoingMessageResponse(5000));
        assertTrue(aliceCall.getLastReceivedResponse().getStatusCode() == 100);

        assertTrue(outboundDestCall.waitForMessage(5000));
        
        assertTrue(outboundDestCall.sendMessageResponse(404, "Not Found", 3600, null));

        assertTrue(aliceCall.waitOutgoingMessageResponse(5000));
        logger.info("Last received response status code: "+aliceCall.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall.getLastReceivedResponse().getStatusCode() == 404);
    }

    @Test
    public void testSendSmsToValidNumber() throws ParseException, InterruptedException {
        SipCall outboundDestCall = outboundDestPhone.createSipCall();
        outboundDestCall.listenForMessage();

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);

        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingMessage(aliceContact, "sip:9898989@" + restcommContact, null, null, null, "Test message");
        assertTrue(aliceCall.waitForAuthorisation(5000));
        assertTrue(aliceCall.waitOutgoingMessageResponse(5000));
        assertTrue(aliceCall.getLastReceivedResponse().getStatusCode() == 100);

        assertTrue(outboundDestCall.waitForMessage(5000));

        assertTrue(outboundDestCall.sendMessageResponse(202, "Accepted", 3600, null));

        assertTrue(aliceCall.waitOutgoingMessageResponse(5000));
        logger.info("Last received response status code: "+aliceCall.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall.getLastReceivedResponse().getStatusCode() == 202);
    }

    @Deployment(name = "SmsTest", managed = true, testable = false)
    public static WebArchive createWebArchive() {

        Map<String, String> webInfResources = new HashMap();
        webInfResources.put("restcomm_SmsTest2.xml", "conf/restcomm.xml");
        webInfResources.put("restcomm.script_SmsTest", "data/hsql/restcomm.script");
        webInfResources.put("sip.xml", "sip.xml");
        webInfResources.put("web_for_SmsTest.xml", "web.xml");
        webInfResources.put("akka_application.conf", "classes/application.conf");        

        Map<String, String> replacements = new HashMap();
        //replace mediaport 2727 
        replacements.put("2727", String.valueOf(mediaPort));
        replacements.put("8080", String.valueOf(restcommHTTPPort));        
        replacements.put("5080", String.valueOf(restcommPort));
        replacements.put("5091", String.valueOf(alicePort));
        replacements.put("5094", String.valueOf(outboundPort));


        List<String> resources = new ArrayList(Arrays.asList(
        ));
        return WebArchiveUtil.createWebArchiveNoGw(webInfResources,
                resources,
                replacements);
    }    
}

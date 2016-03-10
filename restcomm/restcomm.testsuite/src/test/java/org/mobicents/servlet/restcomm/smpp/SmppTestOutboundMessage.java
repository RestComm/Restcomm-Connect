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
package org.mobicents.servlet.restcomm.smpp;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
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

public class SmppTestOutboundMessage {

    private final static Logger logger = Logger.getLogger(SmppTestOutboundMessage.class);
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

    private static SipStackTool tool2;
    private static SipStackTool tool5;

    
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";
    
    private SipStack outboundDestSipStack;
    private SipPhone outboundDestPhone;
    private String outboundDestContact = "sip:9898989@127.0.0.1:5094";


    
    @BeforeClass
    public static void beforeClass() throws Exception {
    	

        tool2 = new SipStackTool("SmsTest2");
        tool5 = new SipStackTool("SmsTest5");
    }
    
    @Before
    public void before() throws Exception {
        
        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);
 
        outboundDestSipStack = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
        outboundDestPhone = outboundDestSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, outboundDestContact);
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
    public void testSendSmsToInvalidNumber() throws ParseException {
    	
        logger.info("************SMPP TEST STARTING*****************");
       	
        try {
            TimeUnit.SECONDS.sleep(90);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     	
        SipCall outboundDestCall = outboundDestPhone.createSipCall();
        outboundDestCall.listenForMessage();
        
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        //assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        
        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);
        
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingMessage(aliceContact, "sip:9898989@127.0.0.1:5080", null, null, null, "Test message");

        aliceCall.waitForAuthorisation(5000);
        outboundDestCall.waitForMessage(5000);
        outboundDestCall.sendMessageResponse(404, "Not Found", 3600, null);
        aliceCall.waitOutgoingMessageResponse(5000);
        
        //assertTrue(aliceCall.waitForAuthorisation(5000));
        
       // assertTrue(outboundDestCall.waitForMessage(5000));
        
        //assertTrue(outboundDestCall.sendMessageResponse(404, "Not Found", 3600, null));
        
        //assertTrue(aliceCall.waitOutgoingMessageResponse(5000));
        //assertTrue(aliceCall.getLastReceivedResponse().getStatusCode() == 404);
        
        assertTrue(MockSmppServer.TestSmppSessionHandler.getSmppOutBoundMessageReceivedByServer());
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
        archive.addAsWebInfResource("restcomm-smpp.xml", "conf/restcomm.xml");
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

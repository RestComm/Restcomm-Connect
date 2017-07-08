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

import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

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
    
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";
    
    private SipStack outboundDestSipStack;
    private SipPhone outboundDestPhone;
    private String outboundDestContact = "sip:9898989@127.0.0.1:5094";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("SmsTest1");
        tool2 = new SipStackTool("SmsTest2");
    }
    
    @Before
    public void before() throws Exception {
        aliceSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        outboundDestSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
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
    public void testSendSmsToInvalidNumber() throws ParseException, InterruptedException {
        SipCall outboundDestCall = outboundDestPhone.createSipCall();
        outboundDestCall.listenForMessage();
        
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        
        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);
        
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingMessage(aliceContact, "sip:9898989@127.0.0.1:5080", null, null, null, "Test message");
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

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);

        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.initiateOutgoingMessage(aliceContact, "sip:9898989@127.0.0.1:5080", null, null, null, "Test message");
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
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        restcommArchive.addClass(SmsRcmlServlet.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/web.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("web_for_SmsTest.xml", "web.xml");
        archive.addAsWebInfResource("restcomm_SmsTest2.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_SmsTest", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}

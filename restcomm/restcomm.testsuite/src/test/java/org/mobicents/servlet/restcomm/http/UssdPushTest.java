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
package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;

import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.ContentTypeHeader;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.ussd.UssdPullTestMessages;

import com.google.gson.JsonObject;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RunWith(Arquillian.class)
public class UssdPushTest {

    private final static Logger logger = Logger.getLogger(CreateCallsTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private String ussdContentSubType = "vnd.3gpp.ussd+xml";
    
    private static SipStackTool tool1;
    private static SipStackTool tool2;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("CreateCalls1");
        tool2 = new SipStackTool("CreaeCalls2");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        georgeSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }
    }
    
    @Test
    public void createUssdPushTestNotifyOnly() throws InterruptedException, SipException, ParseException {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = "bob";
        String rcmlUrl = "http://127.0.0.1:8080/restcomm.application-"+version+"/ussd-rcml.xml";

        JsonObject callResult = RestcommUssdPushTool.getInstance().createUssdPush(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, rcmlUrl);
        assertNotNull(callResult);

        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob", 3600));
        assertTrue(bobCall
                .sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, null, "application", ussdContentSubType, null, null));

        assertTrue(receivedBody.trim().equals(UssdPushTestMessages.ussdPushNotifyOnlyMessage));

        bobCall.waitForAck(5000);
        
        Request infoRequest = bobCall.getDialog().createRequest(Request.INFO);
        
        ContentTypeHeader contentTypeHeader = bobCall.getHeaderFactory().createContentTypeHeader("application", "vnd.3gpp.ussd+xml");
        infoRequest.setContent(UssdPushTestMessages.ussdPushNotifyOnlyResponse.getBytes(), contentTypeHeader);

        bobPhone.sendRequestWithTransaction(infoRequest, false, bobCall.getDialog());     


        Thread.sleep(3000);

        bobCall.listenForDisconnect();
//        assertTrue(georgeCall.disconnect());
//        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());
    }
    
    @Deployment(name = "UssdPushTest", managed = true, testable = false)
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

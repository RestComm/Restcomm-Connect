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
package org.mobicents.servlet.restcomm.telephony.ua;

import gov.nist.javax.sip.message.SIPResponse;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
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
import org.mobicents.servlet.restcomm.tools.MonitoringServiceTool;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
//import org.mobicents.servlet.restcomm.telephony.Version;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */

@RunWith(Arquillian.class)
public final class UserAgentManagerTest {
    private static Logger logger = Logger.getLogger(UserAgentManagerTest.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static SipStackTool tool1;
    private SipStack sipStack;
    private SipPhone phone;
    private String aliceContact = "sip:alice@127.0.0.1:5070;transport=udp";

    private static SipStackTool tool2;
    private SipStack sipStack2;
    private SipPhone phone2;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    public UserAgentManagerTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("UserAgentTest1");
        tool2 = new SipStackTool("UserAgentTest2");
    }

    @Before
    public void before() throws Exception {
        sipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        phone = sipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        sipStack2 = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        phone2 = sipStack2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
    }

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (sipStack != null) {
            sipStack.dispose();
        }
//        deployer.undeploy("UserAgentTest");
    }

    @Test
    public void registerUserAgent() throws Exception {
//        deployer.deploy("UserAgentTest");
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(phone.unregister("sip:127.0.0.1:5070", 0));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithTransport() throws Exception {
//        deployer.deploy("UserAgentTest");
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070;transport=udp", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(phone.unregister("sip:127.0.0.1:5070;transport=udp", 0));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithReRegister() throws Exception {
//        deployer.deploy("UserAgentTest");
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(phone.unregister("sip:127.0.0.1:5070", 0));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithOptionsPing() throws ParseException, InterruptedException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5070;transport=udp", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        // This is necessary for SipUnit to accept unsolicited requests.
//        phone.setLoopback(true);
        phone.listenRequestMessage();
        RequestEvent requestEvent = phone.waitRequest(75000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest().getMethod().equals(SipRequest.OPTIONS));
        logger.info("RequestEvent :"+requestEvent.getRequest().toString());
        Response response = sipStack.getMessageFactory().createResponse(SIPResponse.OK, requestEvent.getRequest());
        phone.sendReply(requestEvent, response);
        Thread.sleep(1000);
        // Clean up (Unregister).
        assertTrue(phone.unregister("sip:127.0.0.1:5070;transport=udp", 0));
    }

    @Test
    public void registerUserAgentWithExceptionOnOptionsPing() throws ParseException, InterruptedException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070;transport=udp", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack.dispose();
        phone = null;
        sipStack = null;

        Thread.sleep(100000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithExceptionOnOptionsPingForGeorge() throws ParseException, InterruptedException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","bob", "1234");
        phone2.addUpdateCredential(c);
        assertTrue(phone2.register(uri, "bob", "1234", bobContact, 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack2.dispose();
        phone2 = null;
        sipStack2 = null;

        Thread.sleep(100000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Deployment(name = "UserAgentTest", managed = true, testable = false)
    public static WebArchive createWebArchive() {
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
        archive.addAsWebInfResource("restcomm.script_UserAgentTest", "data/hsql/restcomm.script");
        return archive;
    }
}

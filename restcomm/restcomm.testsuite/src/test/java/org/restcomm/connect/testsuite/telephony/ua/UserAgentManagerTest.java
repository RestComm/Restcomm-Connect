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
package org.restcomm.connect.testsuite.telephony.ua;

import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPResponse;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.Header;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
//import org.restcomm.connect.telephony.Version;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */

@RunWith(Arquillian.class)
public final class UserAgentManagerTest {
    private static Logger logger = Logger.getLogger(UserAgentManagerTest.class.getName());
    private static final String version = Version.getVersion();

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

    private static SipStackTool tool3;
    private SipStack sipStack3;
    private SipPhone phone3;
    private String aliceContact3 = "sip:alice@127.0.0.1:5071;transport=udp;rc-id=7616";

    private static SipStackTool tool4;
    private SipStack sipStack4;
    private SipPhone phone4;
    private String aliceContact4 = "sip:alice@127.0.0.1:5072";

    private static SipStackTool tool5;
    private SipStack sipStack5;
    private SipPhone phone5;
    private String mariaContact5 = "sip:maria.test%40telestax.com@127.0.0.1:5072";

    private static SipStackTool tool6;
    private SipStack sipStack6;
    private SipPhone phone6;
    private String aliceContact6 = "sip:alice@127.0.0.1:5070";

    public UserAgentManagerTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("UserAgentTest1");
        tool2 = new SipStackTool("UserAgentTest2");
        tool3 = new SipStackTool("UserAgentTest3");
        tool4 = new SipStackTool("UserAgentTest4");
        tool5 = new SipStackTool("UserAgentTest5");
        tool6 = new SipStackTool("UserAgentTest6");
    }

    @Before
    public void before() throws Exception {
        sipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        phone = sipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        sipStack2 = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        phone2 = sipStack2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        sipStack3 = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5071", "127.0.0.1:5080");
        phone3 = sipStack3.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact3);

        sipStack4 = tool4.initializeSipStack(SipStack.PROTOCOL_TCP, "127.0.0.1", "5072", "127.0.0.1:5080");
        phone4 = sipStack4.createSipPhone("127.0.0.1", SipStack.PROTOCOL_TCP, 5080, aliceContact4);

        sipStack5 = tool5.initializeSipStack(SipStack.PROTOCOL_TCP, "127.0.0.1", "5073", "127.0.0.1:5080");
        phone5 = sipStack5.createSipPhone("127.0.0.1", SipStack.PROTOCOL_TCP, 5080, mariaContact5);

        sipStack6 = tool6.initializeSipStack(SipStack.PROTOCOL_TCP, "127.0.0.1", "5074", "127.0.0.1:5080");
        phone6 = sipStack6.createSipPhone("127.0.0.1", SipStack.PROTOCOL_TCP, 5080, aliceContact6);
    }

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (sipStack != null) {
            sipStack.dispose();
        }
        if (phone2 != null) {
            phone2.dispose();
        }
        if (sipStack2 != null) {
            sipStack2.dispose();
        }
        if (phone3 != null) {
            phone3.dispose();
        }
        if (sipStack3 != null) {
            sipStack3.dispose();
        }
        if (phone4 != null) {
            phone4.dispose();
        }
        if (sipStack4 != null) {
            sipStack4.dispose();
        }
        if (sipStack5 != null) {
            sipStack5.dispose();
        }
//        deployer.undeploy("UserAgentTest");
    }

    @Test
    public void registerUserAgent() throws Exception {
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
    public void registerUserAgentWithSecureTransport() throws Exception {
        SipURI uri = sipStack4.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        uri.setSecure(true);
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone4.addUpdateCredential(c);
        assertTrue(phone4.register(uri, "alice", "1234", "sip:127.0.0.1:5072", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        assertTrue(phone4.unregister("sip:127.0.0.1:5072", 0));
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
    public void registerUserAgentWithExtraParamsAndOptionsPing() throws ParseException, InterruptedException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5070;transport=udp;rc-id=7616", 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);
        // This is necessary for SipUnit to accept unsolicited requests.
//        phone.setLoopback(true);
        phone.listenRequestMessage();
        RequestEvent requestEvent = phone.waitRequest(75000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest().getMethod().equals(SipRequest.OPTIONS));
        String extraParam = ((SipUri)requestEvent.getRequest().getRequestURI()).getParameter("rc-id");
        assertNotNull(extraParam);
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
    public void registerUserAgentWith503ErrorResponse() throws ParseException, InterruptedException, InvalidArgumentException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","bob", "1234");
        phone2.addUpdateCredential(c);
        assertTrue(phone2.register(uri, "bob", "1234", bobContact, 3600, 3600));
        Thread.sleep(2000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        phone2.listenRequestMessage();
        RequestEvent request = phone2.waitRequest(10000);
        assertEquals(request.getRequest().getMethod(), SipRequest.OPTIONS);

        ArrayList<Header> additionalHeader = new ArrayList<Header>();
        Header reason = sipStack2.getHeaderFactory().createReasonHeader("udp", 503, "Destination not available");
        additionalHeader.add(reason);
        phone2.sendReply(request, 503, "Service unavailable", null, null, 3600, additionalHeader, null, null);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack2.dispose();
        phone2 = null;
        sipStack2 = null;

        Thread.sleep(1000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithExtraParamsAnd503ToOptionsPing() throws ParseException, InterruptedException, InvalidArgumentException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone3.addUpdateCredential(c);

        assertTrue(phone3.register(uri, "alice", "1234", aliceContact3, 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        phone3.listenRequestMessage();
        RequestEvent requestEvent = phone3.waitRequest(10000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest().getMethod().equals(SipRequest.OPTIONS));
        String extraParam = ((SipUri)requestEvent.getRequest().getRequestURI()).getParameter("rc-id");
        assertNotNull(extraParam);
        logger.info("RequestEvent :"+requestEvent.getRequest().toString());

        ArrayList<Header> additionalHeader = new ArrayList<Header>();
        Header reason = sipStack3.getHeaderFactory().createReasonHeader("udp", 503, "Destination not available");
        additionalHeader.add(reason);
        ArrayList<Header> replaceHeaders = new ArrayList<Header>();
        Header toHeader = sipStack3.getHeaderFactory().createToHeader(sipStack3.getAddressFactory().createAddress(aliceContact3), null);
        replaceHeaders.add(toHeader);
        phone3.sendReply(requestEvent, 503, "Service unavailable", null, null, 3600, additionalHeader, replaceHeaders, null);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack3.dispose();
        phone3 = null;
        sipStack3 = null;

        Thread.sleep(1000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithExtraParamsAnd503ToOptionsPingNoTransport() throws ParseException, InterruptedException, InvalidArgumentException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone3.addUpdateCredential(c);

        assertTrue(phone3.register(uri, "alice", "1234", aliceContact3, 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        phone3.listenRequestMessage();
        RequestEvent requestEvent = phone3.waitRequest(10000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest().getMethod().equals(SipRequest.OPTIONS));
        String extraParam = ((SipUri)requestEvent.getRequest().getRequestURI()).getParameter("rc-id");
        assertNotNull(extraParam);
        logger.info("RequestEvent :"+requestEvent.getRequest().toString());

        ArrayList<Header> additionalHeader = new ArrayList<Header>();
        Header reason = sipStack3.getHeaderFactory().createReasonHeader("udp", 503, "Destination not available");
        additionalHeader.add(reason);
        ArrayList<Header> replaceHeaders = new ArrayList<Header>();
        //The To Header will not contain the transport
        Header toHeader = sipStack3.getHeaderFactory().createToHeader(sipStack3.getAddressFactory().createAddress("sip:alice@127.0.0.1:5071;rc-id=7616"), null);
        replaceHeaders.add(toHeader);
        phone3.sendReply(requestEvent, 503, "Service unavailable", null, null, 3600, additionalHeader, replaceHeaders, null);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack3.dispose();
        phone3 = null;
        sipStack3 = null;

        Thread.sleep(1000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWithExtraParamsAnd408ToOptionsPing() throws ParseException, InterruptedException, InvalidArgumentException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone3.addUpdateCredential(c);

        assertTrue(phone3.register(uri, "alice", "1234", aliceContact3, 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        phone3.listenRequestMessage();
        RequestEvent requestEvent = phone3.waitRequest(10000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest().getMethod().equals(SipRequest.OPTIONS));
        String extraParam = ((SipUri)requestEvent.getRequest().getRequestURI()).getParameter("rc-id");
        assertNotNull(extraParam);
        logger.info("RequestEvent :"+requestEvent.getRequest().toString());

        ArrayList<Header> additionalHeader = new ArrayList<Header>();
        Header reason = sipStack3.getHeaderFactory().createReasonHeader("udp", 503, "Destination not available");
        additionalHeader.add(reason);
        ArrayList<Header> replaceHeaders = new ArrayList<Header>();
        Header toHeader = sipStack3.getHeaderFactory().createToHeader(sipStack3.getAddressFactory().createAddress(aliceContact3), null);
        replaceHeaders.add(toHeader);
        phone3.sendReply(requestEvent, 408, "Request Timeout", null, null, 3600, additionalHeader, replaceHeaders, null);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack3.dispose();
        phone3 = null;
        sipStack3 = null;

        Thread.sleep(1000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerUserAgentWith408ErrorResponse() throws ParseException, InterruptedException, InvalidArgumentException {
//        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = sipStack2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","bob", "1234");
        phone2.addUpdateCredential(c);
        assertTrue(phone2.register(uri, "bob", "1234", bobContact, 3600, 3600));
        Thread.sleep(2000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        phone2.listenRequestMessage();
        RequestEvent request = phone2.waitRequest(10000);
        assertEquals(request.getRequest().getMethod(), SipRequest.OPTIONS);

        ArrayList<Header> additionalHeader = new ArrayList<Header>();
        Header reason = sipStack2.getHeaderFactory().createReasonHeader("udp", 503, "Destination not available");
        additionalHeader.add(reason);

        phone2.sendReply(request, 408, "Request timeout", null, null, 3600);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack2.dispose();
        phone2 = null;
        sipStack2 = null;

        Thread.sleep(1000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    /**
     * registerUserAgentWithAtTheRateSignInLogin
     * we should be able to register and remove registration on non-response to options
     * @throws ParseException
     * @throws InterruptedException
     * @throws InvalidArgumentException
     */
    @Test
    public void registerUserAgentWithAtTheRateSignInLogin() throws ParseException, InterruptedException, InvalidArgumentException {
        SipURI uri = sipStack5.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","maria.test@telestax.com", "1234");
        phone5.addUpdateCredential(c);
        assertTrue(phone5.register(uri, "maria.test@telestax.com", "1234", mariaContact5, 3600, 3600));
        Thread.sleep(2000);

        //user should be registered successfully
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack5.dispose();
        phone5 = null;
        sipStack5 = null;

        Thread.sleep(100000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test
    public void registerMultipleUsersWithSameLoginUnderDifferentOrganizations() throws ParseException, InterruptedException, InvalidArgumentException {
        SipURI uri = sipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        Credential c = new Credential("127.0.0.1","alice", "1234");
        phone.addUpdateCredential(c);
        assertTrue(phone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        Thread.sleep(2000);

        //user should be registered successfully
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        //another alice in different organization
        uri = sipStack6.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        c = new Credential("127.0.0.1","alice", "1234");
        phone6.addUpdateCredential(c);
        sipStack6.getHeaderFactory().createToHeader(sipStack6.getAddressFactory().createAddress("<sip:alice@company.restcomm.com>"), null);
        assertTrue(phone6.register(uri, "alice", "1234", aliceContact6, 3600, 3600));
        Thread.sleep(2000);

        //user should be registered successfully
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        //Dispose phone. Restcomm will fail to send the OPTIONS message and should remove the registration
        sipStack.dispose();
        phone = null;
        sipStack = null;

        Thread.sleep(100000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Deployment(name = "UserAgentTest", managed = true, testable = false)
    public static WebArchive createWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.delete("/WEB-INF/classes/application.conf");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_UserAgentManagerTest.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_UserAgentTest", "data/hsql/restcomm.script");
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        return archive;
    }
}

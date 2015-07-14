package org.mobicents.servlet.restcomm.sms;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.sip.address.SipURI;
import javax.sip.header.Header;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.mobicents.servlet.restcomm.sms.Version;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@RunWith(Arquillian.class)
public final class SmsSessionTest {
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool;
    private SipStack receiver;
    private SipPhone phone;
    private String phoneContact = "sip:+17778889999@127.0.0.1:5091";

    private static SipStackTool tool2;
    private SipStack alice;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5092";
    
    public SmsSessionTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool = new SipStackTool("SmsSessionTest");
        tool2 = new SipStackTool("SmsSessionTest2");
    }

    @Before
    public void before() throws Exception {
        receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, phoneContact);
        
        alice = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        alicePhone = alice.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);
    }

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (receiver != null) {
            receiver.dispose();
        }
        
        if (alicePhone != null) {
            alicePhone.dispose();
        }
        if (alice != null) {
            alice.dispose();
        }
    }

    @Test
    public void testSendSmsRedirectReceiveSms() throws ParseException {
        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:+12223334450@127.0.0.1:5080";
        final String body = "Hello, waiting your response!";
        final SipCall call = phone.createSipCall();
        call.initiateOutgoingMessage(to, proxy, body);
        assertLastOperationSuccess(call);
        // Wait for a response sms.
        phone.setLoopback(true);
        phone.listenRequestMessage();
        assertTrue(call.waitForMessage(60 * 1000));
        call.sendMessageResponse(202, "Accepted", -1);
        final List<String> messages = call.getAllReceivedMessagesContent();
        assertTrue(messages.size() > 0);
        assertTrue(messages.get(0).equals("Hello World!"));
    }

    @Test
    public void testSendSmsRedirectReceiveSms2() throws ParseException {
        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:2001@127.0.0.1:5080";
        final String body = "Hello, waiting your response!";
        final SipCall call = phone.createSipCall();
        call.initiateOutgoingMessage(to, proxy, body);
        assertLastOperationSuccess(call);
        // Wait for a response sms.
        phone.setLoopback(true);
        phone.listenRequestMessage();
        assertTrue(call.waitForMessage(60 * 1000));
        call.sendMessageResponse(202, "Accepted", -1);
        final List<String> messages = call.getAllReceivedMessagesContent();
        assertTrue(messages.size() > 0);
        assertTrue(messages.get(0).equals("Hello World!"));
    }
    
    @Test
    public void testSendSmsRedirectReceiveSms3() throws ParseException {
        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:2001@127.0.0.1:5080";
        final String body = "Hello, waiting your response!";
        final SipCall call = phone.createSipCall();
        call.initiateOutgoingMessage(to, proxy, body);
        assertLastOperationSuccess(call);
        // Wait for a response sms.
        phone.setLoopback(true);
        phone.listenRequestMessage();
        assertTrue(call.waitForMessage(60 * 1000));
        call.sendMessageResponse(202, "Accepted", -1);
        final List<String> messages = call.getAllReceivedMessagesContent();
        assertTrue(messages.size() > 0);
        assertTrue(messages.get(0).equals("Hello World!"));
    }
    
    @Test
    public void testAliceEchoTest() throws ParseException {

        SipURI uri = alice.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);
        
        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
//        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:2002@127.0.0.1:5080";
        final String body = "Hello, waiting your response!";
        final SipCall call = phone.createSipCall();
        call.initiateOutgoingMessage(to, proxy, body);
        
        // Wait for a response sms.
        alicePhone.setLoopback(true);
        aliceCall.listenForMessage();
        assertTrue(aliceCall.waitForMessage(5000));
        assertTrue(aliceCall.sendMessageResponse(202, "Alice-Accepted", -1));
        String messageBody = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue(messageBody.equals("Hello World!"));
        aliceCall.initiateOutgoingMessage("sip:+17778889999@127.0.0.1:5091", null, "Its great to hear from you!");
        assertTrue(aliceCall.waitForAuthorisation(5000));
        
        call.listenForMessage();
        assertTrue(call.waitForMessage(6 * 1000));
        call.sendMessageResponse(202, "Accepted", -1);
        final List<String> messages = call.getAllReceivedMessagesContent();
        assertTrue(messages.size() > 0);
        assertTrue(messages.get(0).equals("Its great to hear from you!"));
    }
    
    private String smsRcml = "<Response><Sms to=\"alice\" from=\"restcomm\">Hello World!</Sms></Response>";
    @Test
    public void testSmsWithCustomHeaders() throws ParseException {
        stubFor(get(urlPathEqualTo("/rcml"))
                .withQueryParam("SipHeader_X-MyCustom-Header1", containing("Value1"))
                .withQueryParam("SipHeader_X-MyCustom-Header2", containing("Value2"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(smsRcml)));
        
        SipURI uri = alice.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential credential = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(credential);
        
        // Prepare Alice to receive call
        final SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();
        
//        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:2003@127.0.0.1:5080";
        final String body = "Hello, waiting your response!";
        final SipCall call = phone.createSipCall();
        ArrayList<Header> additionalHeaders = new ArrayList<Header>();
        additionalHeaders.add(phone.getParent().getHeaderFactory().createHeader("X-MyCustom-Header1", "Value1"));
        additionalHeaders.add(phone.getParent().getHeaderFactory().createHeader("X-MyCustom-Header2", "Value2"));
        call.initiateOutgoingMessage(phoneContact, to, proxy, additionalHeaders, null, body);
//        call.initiateOutgoingMessage(to, proxy, body);
        
        // Wait for a response sms.
        alicePhone.setLoopback(true);
        aliceCall.listenForMessage();
        assertTrue(aliceCall.waitForMessage(5000));
        assertTrue(aliceCall.sendMessageResponse(202, "Alice-Accepted", -1));
        String messageBody = new String(aliceCall.getLastReceivedMessageRequest().getRawContent());
        assertTrue(messageBody.equals("Hello World!"));
        aliceCall.initiateOutgoingMessage("sip:+17778889999@127.0.0.1:5091", null, "Its great to hear from you!");
        assertTrue(aliceCall.waitForAuthorisation(5000));
        
        call.listenForMessage();
        assertTrue(call.waitForMessage(6 * 1000));
        call.sendMessageResponse(202, "Accepted", -1);
        final List<String> messages = call.getAllReceivedMessagesContent();
        assertTrue(messages.size() > 0);
        assertTrue(messages.get(0).equals("Its great to hear from you!"));
    }
    
    @Deployment(name = "SmsSessionTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm_SmsTest.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_SmsTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("entry.xml");
        archive.addAsWebResource("sms.xml");
        archive.addAsWebResource("sms_to_alice.xml");
        return archive;
    }
}

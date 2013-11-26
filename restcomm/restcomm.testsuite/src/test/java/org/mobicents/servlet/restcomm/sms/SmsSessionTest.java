package org.mobicents.servlet.restcomm.sms;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.*;
import org.junit.runner.RunWith;
//import org.mobicents.servlet.restcomm.sms.Version;


import java.text.ParseException;
import java.util.List;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertTrue;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@RunWith(Arquillian.class)
public final class SmsSessionTest {
    private static final String version = org.mobicents.servlet.restcomm.Version.getInstance().getRestCommVersion();

    @ArquillianResource
    private Deployer deployer;

    private static SipStackTool tool;
    private SipStack receiver;
    private SipPhone phone;

    public SmsSessionTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool = new SipStackTool("SmsSessionTest");
    }

    @Before
    public void before() throws Exception {
        receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:+17778889999@127.0.0.1:5070");
    }

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (receiver != null) {
            receiver.dispose();
        }
        deployer.undeploy("SmsSessionTest");
    }

    @Ignore
    @Test
    public void testSendSmsRedirectReceiveSms() throws ParseException {
        deployer.deploy("SmsSessionTest");
        // Send restcomm an sms.
        final String proxy = phone.getStackAddress() + ":5080;lr/udp";
        final String to = "sip:+12223334444@127.0.0.1:5080";
        final String body = "Hello World!";
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

    @Deployment(name = "SmsSessionTest", managed = false, testable = false)
    public static WebArchive createWebArchive() {
        final WebArchive archive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        JavaArchive dependency = ShrinkWrapMaven.resolver().resolve("commons-configuration:commons-configuration:jar:1.7")
                .offline().withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("jain:jain-mgcp-ri:jar:1.0").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("org.mobicents.media.client:mgcp-driver:jar:3.0.0.Final").offline()
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.mgcp:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.http:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.interpreter:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.sms.api:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.sms:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
        archive.addAsWebResource("entry.xml");
        archive.addAsWebResource("sms.xml");
        return archive;
    }
}

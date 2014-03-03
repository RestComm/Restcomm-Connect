package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
 */
@RunWith(Arquillian.class)
public class LiveCallModificationTest {

    private final static Logger logger = Logger.getLogger(CreateCallsTest.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getInstance().getRestCommVersion();
    
    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("LiveCallModification1");
        tool2 = new SipStackTool("LiveCallModification2");
        tool3 = new SipStackTool("LiveCallModification3");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        georgeSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, georgeContact);

        aliceSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);
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

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }
    }

    @Test
    // Terminate a call in-progress using the Live Call Modification API. Non-regression test for issue:
    // https://bitbucket.org/telestax/telscale-restcomm/issue/139
    public void terminateInProgressCall() throws Exception {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = bobContact;
        String rcmlUrl = "http://127.0.0.1:8080/restcomm.application-7.1.4-TelScale-SNAPSHOT/dial-number-entry.xml";

        JsonObject callResult = RestcommCallsTool.getInstance().createCall(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, rcmlUrl);
        assertNotNull(callResult);
        String callSid = callResult.get("sid").getAsString();

        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob", 3600));
        assertTrue(bobCall
                .sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, receivedBody, "application", "sdp", null, null));

        // Restcomm now should execute RCML that will create a call to +131313 (george's phone)

        assertTrue(georgeCall.waitForIncomingCall(5000));
        receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));

        Thread.sleep(3000);

        bobCall.listenForDisconnect();
        georgeCall.listenForDisconnect();

        callResult = RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken,
                callSid, "completed", null);

        assertTrue(georgeCall.disconnect());
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());

        georgeCall.dispose();
        bobCall.dispose();
    }

    @Test
    // Terminate a call that is ringing using the Live Call Modification API. Non-regression test for issue:
    // https://bitbucket.org/telestax/telscale-restcomm/issue/139
    public void terminateRingingCall() throws Exception {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = bobContact;
        String rcmlUrl = "http://127.0.0.1:8080/restcomm.application-7.1.4-TelScale-SNAPSHOT/dial-number-entry.xml";

        JsonObject callResult = RestcommCallsTool.getInstance().createCall(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, rcmlUrl);
        assertNotNull(callResult);
        String callSid = callResult.get("sid").getAsString();

        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob1", 3600));

        Thread.sleep(1000);

        bobCall.listenForDisconnect();

        callResult = RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken,
                callSid, "canceled", null);

        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());

        georgeCall.dispose();
        bobCall.dispose();
    }

    @Test
    // Redirect a call to a different URL using the Live Call Modification API. Non-regression test for issue:
    // https://bitbucket.org/telestax/telscale-restcomm/issue/139
    // TODO: This test is expected to fail because of issue https://bitbucket.org/telestax/telscale-restcomm/issue/192
    public void redirectCall() throws Exception {

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.listenForIncomingCall();

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Register Alice Restcomm client
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = bobContact;
        String rcmlUrl = "http://127.0.0.1:8080/restcomm.application-7.1.4-TelScale-SNAPSHOT/dial-number-entry.xml";

        JsonObject callResult = RestcommCallsTool.getInstance().createCall(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, rcmlUrl);
        assertNotNull(callResult);
        String callSid = callResult.get("sid").getAsString();

        assertTrue(bobCall.waitForIncomingCall(5000));
        String receivedBody = new String(bobCall.getLastReceivedRequest().getRawContent());
        assertTrue(bobCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Bob", 3600));
        assertTrue(bobCall
                .sendIncomingCallResponse(Response.OK, "OK-Bob", 3600, receivedBody, "application", "sdp", null, null));

        assertTrue(bobCall.waitForAck(5000));
        
        // Restcomm now should execute RCML that will create a call to +131313 (george's phone)

        assertTrue(georgeCall.waitForIncomingCall(5000));
        receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp",
                null, null));

        assertTrue(georgeCall.waitForAck(5000));
        
        Thread.sleep(10000);
        System.out.println("\n ******************** \nAbout to redirect the call\n ********************\n");
        rcmlUrl = "http://127.0.0.1:8080/restcomm.application-7.1.4-TelScale-SNAPSHOT/dial-client-entry.xml";

        callResult = RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken,
                callSid, null, rcmlUrl);

        georgeCall.listenForDisconnect();
        assertTrue(georgeCall.waitForDisconnect(5000));
        assertTrue(georgeCall.respondToDisconnect());
        
        // Restcomm now should execute the new RCML and create a call to Alice Restcomm client
        // TODO: This test is expected to fail because of issue https://bitbucket.org/telestax/telscale-restcomm/issue/192
        assertTrue(aliceCall.waitForIncomingCall(5000));
        receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));

        Thread.sleep(3000);

        aliceCall.listenForDisconnect();
        bobCall.listenForDisconnect();

        assertTrue(aliceCall.disconnect());
        assertTrue(aliceCall.waitForAck(5000));

        assertTrue(bobCall.waitForDisconnect(5000));
        assertTrue(bobCall.respondToDisconnect());

    }

    @Deployment(name = "LiveCallModificationTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
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
        dependency = ShrinkWrapMaven.resolver().resolve("joda-time:joda-time:jar:2.0").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.iSpeech:iSpeech:jar:1.0.1").offline().withoutTransitivity()
                .asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.asr:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.fax:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.tts.acapela:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.tts.api:jar:" + version)
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
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.telephony.api:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        dependency = ShrinkWrapMaven.resolver().resolve("com.telestax.servlet:restcomm.telephony:jar:" + version)
                .withoutTransitivity().asSingle(JavaArchive.class);
        archive.addAsLibrary(dependency);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-number-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("hello-play.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}

package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonObject;

/**
 * LiveCallModificationMuteUnMuteTest
 * @author mariafarooq
 *
 */
@RunWith(Arquillian.class)
public class LiveCallModificationMuteUnMuteTest {

    private final static Logger logger = Logger.getLogger(CreateCallsTest.class.getName());
    private static final String version = Version.getVersion();

    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;

    private SipStack mariaSipStack;
    private SipPhone mariaPhone;
    private String mariaContact = "sip:maria@127.0.0.1:5090";

    private SipStack buttSipStack;
    private SipPhone buttPhone;
    private String buttContact = "sip:+131313@127.0.0.1:5070";

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
        mariaSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContact);

        buttSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        buttPhone = buttSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, buttContact);

        aliceSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);
    }

    @After
    public void after() throws Exception {
        if (mariaPhone != null) {
            mariaPhone.dispose();
        }
        if (mariaSipStack != null) {
            mariaSipStack.dispose();
        }

        if (buttPhone != null) {
            buttPhone.dispose();
        }
        if (buttSipStack != null) {
            buttSipStack.dispose();
        }

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }
        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    /**
     * muteInProgressCall
     * @throws Exception
     */
    @Test
    public void muteInProgressCall() throws Exception {

        SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.listenForIncomingCall();

        SipCall buttCall = buttPhone.createSipCall();
        buttCall.listenForIncomingCall();

        String from = "+15126002188";
        String to = mariaContact;
        String rcmlUrl = "http://127.0.0.1:8080/restcomm/dial-number-entry.xml";

        JsonObject callResult = (JsonObject) RestcommCallsTool.getInstance().createCall(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, from, to, rcmlUrl);
        assertNotNull(callResult);
        String callSid = callResult.get("sid").getAsString();

        assertTrue(mariaCall.waitForIncomingCall(5000));
        String receivedBody = new String(mariaCall.getLastReceivedRequest().getRawContent());
        assertTrue(mariaCall.sendIncomingCallResponse(Response.RINGING, "Ringing-maria", 3600));
        assertTrue(mariaCall
                .sendIncomingCallResponse(Response.OK, "OK-maria", 3600, receivedBody, "application", "sdp", null, null));

        // Restcomm now should execute RCML that will create a call to +131313 (butt's phone)

        assertTrue(buttCall.waitForIncomingCall(5000));
        receivedBody = new String(buttCall.getLastReceivedRequest().getRawContent());
        assertTrue(buttCall.sendIncomingCallResponse(Response.RINGING, "Ringing-butt", 3600));
        assertTrue(buttCall.sendIncomingCallResponse(Response.OK, "OK-butt", 3600, receivedBody, "application", "sdp",
                null, null));

        Thread.sleep(1000);

        mariaCall.listenForDisconnect();
        buttCall.listenForDisconnect();

        //Mute call
        callResult = RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken,
                callSid, true);
        assertTrue(callResult != null);
        
        //Mute again
        callResult = RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(), adminAccountSid, adminAuthToken,
                callSid, true);

        //TODO: get call from api again an check if mute is true!!! add this functionality in call api
        buttCall.dispose();
        mariaCall.dispose();
    }

    /**
     * unMuteInProgressCall
     * @throws Exception
     */
    @Ignore@Test
    public void unMuteInProgressCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteUnMutedCall: we can mute only unmuted call
     * @throws Exception
     */
    @Ignore@Test
    public void muteMutedCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteUnMutedCall: we can unmute only muted call
     * @throws Exception
     */
    @Ignore@Test
    public void unMuteUnMutedCall() throws Exception {
    	fail("Not Implemented");
    }
    
    /**
     * muteCompletedCall: we can mute/unmute only in progress call
     * @throws Exception
     */
    @Ignore@Test
    public void muteCompletedCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteCompletedCall: we can mute/unmute only in progress call
     * @throws Exception
     */
    @Ignore@Test
    public void unMuteCompletedCall() throws Exception {
    	fail("Not Implemented");
    }
    
    @Deployment(name = "LiveCallModificationTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
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

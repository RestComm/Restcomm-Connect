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

package org.restcomm.connect.testsuite.extensions;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for Dial Action attribute. Reference: https://www.twilio.com/docs/api/twiml/dial#attributes-action The 'action'
 * attribute takes a URL as an argument. When the dialed call ends, Restcomm will make a GET or POST request to this URL
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class TrafficThrottlingExtensionTest {

    private final static Logger logger = Logger.getLogger(TrafficThrottlingExtensionTest.class.getName());

    private static final String version = Version.getVersion();
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

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;

    // Bob is a simple SIP Client. Will not register with Restcomm
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private String aliceContact = "sip:alice@127.0.0.1:5091";

    // Henrique is a simple SIP Client. Will not register with Restcomm
    private SipStack henriqueSipStack;
    private SipPhone henriquePhone;
    private String henriqueContact = "sip:henrique@127.0.0.1:5092";

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private String georgeContact = "sip:+131313@127.0.0.1:5070";

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("TrafficThrottlingExtensionTest1");
        tool2 = new SipStackTool("TrafficThrottlingExtensionTest2");
        tool3 = new SipStackTool("TrafficThrottlingExtensionTest3");
        tool4 = new SipStackTool("TrafficThrottlingExtensionTest4");
    }


    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

        henriqueSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        henriquePhone = henriqueSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, henriqueContact);

        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
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

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }

        if (henriqueSipStack != null) {
            henriqueSipStack.dispose();
        }
        if (henriquePhone != null) {
            henriquePhone.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }
        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    private String dialNumberRcml = "<Response><Dial><Number>+131313</Number></Dial></Response>";
    @Test
    public void testMakeThreeConcurrentCallsToRCMLRegisteredClientAlice() throws ParseException, InterruptedException, ConfigurationException {
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        PropertiesConfiguration conf = new PropertiesConfiguration("traffic_throttling_configuration.properties");
        int concurrentOutgoingCallsPerClient = conf.getInt("concurrent_outgoing_calls_per_client");

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential c = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(c);

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        SipCall aliceCall1 = alicePhone.createSipCall();
        aliceCall1.initiateOutgoingCall(aliceContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
        int response = aliceCall1.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall1.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall1.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall1.sendInviteOkAck());

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(5000));

        int liveCalls = MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertTrue(liveCalls==2);
        assertTrue(liveCallsArraySize==2);

        assertTrue((new PropertiesConfiguration("traffic_throttling_configuration.properties")).getInt("concurrent_outgoing_calls_per_client") == 2);

        SipCall aliceCall2 = alicePhone.createSipCall();
        aliceCall2.initiateOutgoingCall(aliceContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(aliceCall2);
        assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
        response = aliceCall2.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall2.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall2.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall2.sendInviteOkAck());

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(5000));

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==4);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==4);

        //The third call will be blocked

        SipCall aliceCall3 = alicePhone.createSipCall();
        aliceCall3.initiateOutgoingCall(aliceContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(aliceCall3);
//        assertTrue(aliceCall.waitForAuthorisation(5000));
        assertTrue(aliceCall3.waitOutgoingCallResponse(5 * 1000));
        response = aliceCall3.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall3.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall3.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall3.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall3.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall3.sendInviteOkAck());

        aliceCall3.listenForDisconnect();
        assertTrue(aliceCall3.waitForDisconnect(5000));
        assertTrue(aliceCall3.respondToDisconnect());
        SipRequest bye = aliceCall3.getLastReceivedRequest();


        Thread.sleep(1000);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==4);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==4);

        aliceCall1.disconnect();
        aliceCall2.disconnect();

        Thread.sleep(2000);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
    }

    @Test @Ignore
    public void testMakeThreeConcurrentCallsToRCMLRegisteredClientAliceChangePropertiesOnTheFly() throws ParseException, InterruptedException, ConfigurationException {
        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialNumberRcml)));

        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        Credential c = new Credential("127.0.0.1", "alice", "1234");
        alicePhone.addUpdateCredential(c);

        SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        // Create outgoing call with first phone
        SipCall aliceCall1 = alicePhone.createSipCall();
        aliceCall1.initiateOutgoingCall(aliceContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
        int response = aliceCall1.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall1.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall1.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall1.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall1.sendInviteOkAck());

        assertTrue(georgeCall.waitForIncomingCall(5000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.TRYING, "George-Trying", 3600));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "George-Ringing", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "George-OK", 3600, receivedBody, "application", "sdp",
                null, null));
        assertTrue(georgeCall.waitForAck(5000));

        int liveCalls = MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
        assertTrue(liveCalls==2);
        assertTrue(liveCallsArraySize==2);

        URL resource = this.getClass().getClassLoader().getResource("traffic_throttling_configuration.properties");
        PropertiesConfiguration conf = new PropertiesConfiguration("traffic_throttling_configuration.properties");
        int originalValue = conf.getInt("concurrent_outgoing_calls_per_client");
        logger.info("original concurrent_outgoing_calls_per_client: "+originalValue);
        assertTrue(originalValue == 2);
        conf.setProperty("concurrent_outgoing_calls_per_client", 1);
        conf.save(resource);
        conf.reload();
        assertTrue((new PropertiesConfiguration("traffic_throttling_configuration.properties")).getInt("concurrent_outgoing_calls_per_client") == 1);
        Thread.sleep(1000);

        SipCall aliceCall2 = alicePhone.createSipCall();
        aliceCall2.initiateOutgoingCall(aliceContact, "sip:1111@127.0.0.1:5080", null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(aliceCall2);
        assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
        response = aliceCall2.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, aliceCall2.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall2.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, aliceCall2.getLastReceivedResponse().getStatusCode());
        assertTrue(aliceCall2.sendInviteOkAck());

        aliceCall2.listenForDisconnect();
        assertTrue(aliceCall2.waitForDisconnect(5000));
        assertTrue(aliceCall2.respondToDisconnect());

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==2);

        aliceCall1.disconnect();

        Thread.sleep(1000);

        assertTrue(MonitoringServiceTool.getInstance().getLiveCalls(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        assertTrue(MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

        conf.setProperty("concurrent_outgoing_calls_per_client", originalValue);
        conf.save(resource);
        conf.reload();
        assertTrue((new PropertiesConfiguration("traffic_throttling_configuration.properties")).getInt("concurrent_outgoing_calls_per_client") == 2);

    }

    @Deployment(name = "TrafficThrottlingExtensionTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/conf/extensions.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_traffic_throttling.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("extensions.xml", "conf/extensions.xml");
        archive.addAsWebInfResource("restcomm.script_traffic_throttling", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}

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

package org.restcomm.connect.testsuite.sms.push;

import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.http.CreateClientsTool;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
@RunWith(Arquillian.class)
public class SmsPushNotificationServerTest {

    private final static Logger logger = Logger.getLogger(SmsPushNotificationServerTest.class.getName());

    private static final String CLIENT_PASSWORD = "qwerty1234RT";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);

    @ArquillianResource
    URL deploymentUrl;

    private static SipStackTool tool1;
    private static SipStackTool tool2;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private static String bobPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String bobContact = "sip:bob@127.0.0.1:" + bobPort;

    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private static String alicePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String aliceContact = "sip:alice@127.0.0.1:" + alicePort;

    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;
    private static String restcommContact = "127.0.0.1:" + restcommPort;

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("SmsPushNotificationServerTest1");
        tool2 = new SipStackTool("SmsPushNotificationServerTest2");
    }

    public static void reconfigurePorts() {
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort;
        }
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));
        }
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", bobPort, restcommContact);
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, bobContact);

        aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort, restcommContact);
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact);
    }

    @After
    @SuppressWarnings("Duplicates")
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (alicePhone != null) {
            alicePhone.dispose();
        }
        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }

        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    @Test
    public void testB2BUAMessage() throws ParseException, InterruptedException, IOException {
        stubFor(post(urlPathEqualTo("/api/notifications"))
                .withHeader("Content-Type", matching("application/json;.*"))
                .willReturn(aResponse()
                        .withStatus(200)));

        final SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);

        wireMockRule.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(com.github.tomakehurst.wiremock.http.Request request, com.github.tomakehurst.wiremock.http.Response response) {
                if (request.getAbsoluteUrl().contains("/api/notifications") && response.getStatus() == 200) {
                    assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
                }
            }
        });

        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForMessage();

        CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", CLIENT_PASSWORD, null);

        Credential c = new Credential("127.0.0.1", "bob", CLIENT_PASSWORD);
        bobPhone.addUpdateCredential(c);
        assertTrue(bobPhone.register(uri, "bob", CLIENT_PASSWORD, bobContact, 3600, 3600));

        SipCall bobCall = bobPhone.createSipCall();
        bobCall.initiateOutgoingMessage(bobContact, aliceContact, null, null, null, "Hello, Alice!");
        assertTrue(bobCall.waitForAuthorisation(5000));
        assertTrue(bobCall.waitOutgoingMessageResponse(5000));
        assertTrue(aliceCall.waitForMessage(5000));
        assertTrue(aliceCall.sendMessageResponse(Response.ACCEPTED, "Accepted", 3600, null));

        int response;
        do {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            response = bobCall.getLastReceivedResponse().getStatusCode();
        } while (response == Response.TRYING);
        assertTrue(response == Response.ACCEPTED);

        verify(postRequestedFor(urlEqualTo("/api/notifications")));
    }

    @SuppressWarnings("Duplicates")
    @Deployment(name = "SmsPushNotificationServerTest", testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + Version.getVersion()).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.delete("/WEB-INF/classes/application.conf");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_pushNotificationServer.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_pushNotificationServer", "data/hsql/restcomm.script");
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        logger.info("Packaged Test App");
        return archive;
    }
}

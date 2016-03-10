package org.mobicents.servlet.restcomm.smpp;

import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@RunWith(Arquillian.class)
public class SmppTestServerInboundMessageToRestcomm {

	private final static Logger logger = Logger.getLogger(SmppTestServerInboundMessageToRestcomm.class);
	private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    private static String to = "7777";
    private static String from = "9999";
    private static String msgBody = "Message from SMPP Server to Restcomm";
    private static String msgBodyResp = "Response from Restcomm to SMPP server";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

	@ArquillianResource
	private Deployer deployer;
	private MockSmppServer mockSmppServer;

	@Before
	public void before() throws Exception {
		mockSmppServer = new MockSmppServer();
		Thread.sleep(5000);
	}

	@After
	public void after() throws InterruptedException {
		mockSmppServer.stop();
		Thread.sleep(2000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
	}

    private String smsEchoRcml = "<Response><Sms to=\""+from+"\" from=\""+to+"\">"+msgBodyResp+"</Sms></Response>";
	@Test
	public void testSendMessageToRestcomm () throws SmppInvalidArgumentException, IOException, InterruptedException {

        stubFor(get(urlPathEqualTo("/smsApp"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(smsEchoRcml)));

		logger.info("Will wait for the SMPP link to be established");
		do {
			Thread.sleep(1000);
		} while (!mockSmppServer.isLinkEstablished());
		logger.info("SMPP link is now established");

		mockSmppServer.sendSmppMessageToRestcomm(msgBody,to,from);
        Thread.sleep(2000);
        assertTrue(mockSmppServer.isMessageSent());

	}

	@Deployment(name = "SmppTestServerInboundMessageToRestcomm", managed = true, testable = false)
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
		archive.addAsWebInfResource("restcomm-smpp.xml", "conf/restcomm.xml");
	    archive.addAsWebInfResource("restcomm.script-smpp", "data/hsql/restcomm.script");
		return archive;
	}
}

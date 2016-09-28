package org.mobicents.servlet.restcomm.smpp;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.sms.smpp.SmppInboundMessageEntity;

import javax.sip.address.SipURI;
import javax.sip.message.Request;
import java.io.IOException;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@RunWith(Arquillian.class)
public class SmppTests {

	private final static Logger logger = Logger.getLogger(SmppTests.class);
	private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    private static String to = "7777";
    private static String from = "9999";
    private static String msgBody = "Message from SMPP Server to Restcomm";
    private static String msgBodyResp = "Response from Restcomm to SMPP server";
    private static String msgBodyRespUCS2 = "Response from Restcomm to SMPP serverПППРРр";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

	@ArquillianResource
	private Deployer deployer;
	private static MockSmppServer mockSmppServer;

	private static SipStackTool tool2;
	private SipStack aliceSipStack;
	private SipPhone alicePhone;
	private String aliceContact = "sip:alice@127.0.0.1:5092";

	private static SipStackTool tool3;
	private SipStack bobSipStack;
	private SipPhone bobPhone;
	private String bobContact = "sip:bob@127.0.0.1:5093";

	private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
	private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

	@BeforeClass
	public static void prepare() throws SmppChannelException, InterruptedException {
		tool2 = new SipStackTool("SmppTest2");
		tool3 = new SipStackTool("SmppTest3");

		mockSmppServer = new MockSmppServer();
		logger.info("Will wait for the SMPP link to be established");
		do {
			Thread.sleep(1000);
		} while (!mockSmppServer.isLinkEstablished());
		logger.info("SMPP link is now established");
	}

	@Before
	public void before() throws Exception {

		aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
		alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

		bobSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5093", "127.0.0.1:5080");
		bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);

		mockSmppServer.cleanup();
		Thread.sleep(5000);
	}

	@AfterClass
	public static void cleanup() {
		mockSmppServer.stop();
	}

	@After
	public void after() throws InterruptedException {
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

		mockSmppServer.sendSmppMessageToRestcomm(msgBody,to,from,CharsetUtil.CHARSET_GSM);
        Thread.sleep(2000);
        assertTrue(mockSmppServer.isMessageSent());
		Thread.sleep(2000);
		assertTrue(mockSmppServer.isMessageReceived());
		SmppInboundMessageEntity inboundMessageEntity = mockSmppServer.getSmppInboundMessageEntity();
		assertNotNull(inboundMessageEntity);
		assertTrue(inboundMessageEntity.getSmppTo().equals(from));
		assertTrue(inboundMessageEntity.getSmppFrom().equals(to));
		assertTrue(inboundMessageEntity.getSmppContent().equals(msgBodyResp));
	}

    private String smsEchoRcmlUCS2 = "<Response><Sms to=\""+from+"\" from=\""+to+"\">"+msgBodyRespUCS2+"</Sms></Response>";
	@Test
	public void testSendMessageToRestcommUCS2 () throws SmppInvalidArgumentException, IOException, InterruptedException {

        stubFor(get(urlPathEqualTo("/smsApp"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(smsEchoRcmlUCS2)));

		mockSmppServer.sendSmppMessageToRestcomm(msgBody,to,from,CharsetUtil.CHARSET_UCS_2);
        Thread.sleep(2000);
        assertTrue(mockSmppServer.isMessageSent());
		Thread.sleep(8000);
		assertTrue(mockSmppServer.isMessageReceived());
		SmppInboundMessageEntity inboundMessageEntity = mockSmppServer.getSmppInboundMessageEntity();
		assertNotNull(inboundMessageEntity);
		assertTrue(inboundMessageEntity.getSmppTo().equals(from));
		assertTrue(inboundMessageEntity.getSmppFrom().equals(to));
		logger.info("msgBodyResp: " + msgBodyRespUCS2);
		logger.info("getSmppContent: " + inboundMessageEntity.getSmppContent());
		assertTrue(inboundMessageEntity.getSmppContent().equals(msgBodyRespUCS2));
	}

	@Test
	public void testClientSentToOtherClient () throws ParseException {

		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
		Credential aliceCred = new Credential("127.0.0.1","alice","1234");
		alicePhone.addUpdateCredential(aliceCred);

		assertTrue(bobPhone.register(uri,"bob","1234",bobContact, 3600, 3600));
		Credential bobCread = new Credential("127.0.0.1","bob","1234");
		bobPhone.addUpdateCredential(bobCread);

		SipCall bobCall = bobPhone.createSipCall();
		bobCall.listenForMessage();

		SipCall aliceCall = alicePhone.createSipCall();
		assertTrue(aliceCall.initiateOutgoingMessage("sip:bob@127.0.0.1:5080", null, "Test Message from Alice"));
		assertTrue(aliceCall.waitForAuthorisation(5000));
		assertTrue(aliceCall.waitOutgoingMessageResponse(5000));

		assertTrue(bobCall.waitForMessage(5000));
		Request msgReceived = bobCall.getLastReceivedMessageRequest();
		assertTrue(new String(msgReceived.getRawContent()).equals("Test Message from Alice"));
	}

	@Test
	public void testClientSentOutUsingSMPP () throws ParseException, InterruptedException {

		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
		Credential aliceCred = new Credential("127.0.0.1","alice","1234");
		alicePhone.addUpdateCredential(aliceCred);

		assertTrue(bobPhone.register(uri,"bob","1234",bobContact, 3600, 3600));
		Credential bobCread = new Credential("127.0.0.1","bob","1234");
		bobPhone.addUpdateCredential(bobCread);


		SipCall aliceCall = alicePhone.createSipCall();
		aliceCall.initiateOutgoingMessage("sip:9999@127.0.0.1:5080", null, "Test Message from Alice");
		aliceCall.waitForAuthorisation(8000);
		Thread.sleep(5000);
		assertTrue(mockSmppServer.isMessageReceived());
		SmppInboundMessageEntity inboundMessageEntity = mockSmppServer.getSmppInboundMessageEntity();
		assertNotNull(inboundMessageEntity);
		assertTrue(inboundMessageEntity.getSmppTo().equals("9999"));
		assertTrue(inboundMessageEntity.getSmppFrom().equals("alice"));
		assertTrue(inboundMessageEntity.getSmppContent().equals("Test Message from Alice"));
	}

	@Deployment(name = "SmppTests", managed = true, testable = false)
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

package org.restcomm.connect.testsuite.smpp;

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
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.sms.smpp.SmppInboundMessageEntity;

import javax.sip.address.SipURI;
import javax.sip.message.Request;
import java.io.IOException;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author maria.farooq (Maria Farooq)
 */
@RunWith(Arquillian.class)
public class SmppOrganizationTests {

	private final static Logger logger = Logger.getLogger(SmppOrganizationTests.class);
	private static final String version = Version.getVersion();

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
	private SipStack mariaSipStack;
	private SipPhone mariaPhone;
	private String mariaContact = "sip:maria@org2.restcomm.com";

	private static SipStackTool tool3;
	private SipStack shoaibSipStack;
	private SipPhone shoaibPhone;
	private String shoaibContact = "sip:shoaib@org2.restcomm.com";

	private static SipStackTool tool4;
	private SipStack mariaOrg3SipStack;
	private SipPhone mariaOrg3Phone;
	private String mariaOrg3Contact = "sip:maria@org3.restcomm.com";

	private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
	private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

	@BeforeClass
	public static void prepare() throws SmppChannelException, InterruptedException {
		tool2 = new SipStackTool("SmppTest2");
		tool3 = new SipStackTool("SmppTest3");
		tool4 = new SipStackTool("SmppTest4");

		mockSmppServer = new MockSmppServer();
		logger.info("Will wait for the SMPP link to be established");
		do {
			Thread.sleep(1000);
		} while (!mockSmppServer.isLinkEstablished());
		logger.info("SMPP link is now established");
	}

	@Before
	public void before() throws Exception {

		mariaSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
		mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContact);

		shoaibSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5093", "127.0.0.1:5080");
		shoaibPhone = shoaibSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, shoaibContact);

		mariaOrg3SipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
		mariaOrg3Phone = mariaOrg3SipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaOrg3Contact);

		mockSmppServer.cleanup();
		Thread.sleep(5000);
	}

	@AfterClass
	public static void cleanup() {
		mockSmppServer.stop();
	}

	@After
	public void after() throws InterruptedException {
		if (shoaibPhone != null) {
			shoaibPhone.dispose();
		}
		if (shoaibSipStack != null) {
			shoaibSipStack.dispose();
		}

		if (mariaPhone != null) {
			mariaPhone.dispose();
		}
		if (mariaSipStack != null) {
			mariaSipStack.dispose();
		}

		if (mariaOrg3Phone != null) {
			mariaOrg3Phone.dispose();
		}
		if (mariaOrg3SipStack != null) {
			mariaOrg3SipStack.dispose();
		}
		Thread.sleep(2000);
        wireMockRule.resetRequests();
        Thread.sleep(2000);
	}

	@Test
	public void testClientSentToOtherClientDifferentOrganization () throws ParseException {

		SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(mariaPhone.register(uri, "maria", "qwerty1234RT", "sip:maria@127.0.0.1:5092", 3600, 3600));
		Credential mariaCred = new Credential("org3.restcomm.com","maria","qwerty1234RT");
		mariaPhone.addUpdateCredential(mariaCred);

		assertTrue(mariaOrg3Phone.register(uri,"maria","1234","sip:maria@127.0.0.1:5094", 3600, 3600));
		Credential mariaOrg3Cread = new Credential("org3.restcomm.com","maria","1234");
		mariaOrg3Phone.addUpdateCredential(mariaOrg3Cread);

		SipCall mariaOrg3Call = mariaOrg3Phone.createSipCall();
		mariaOrg3Call.listenForMessage();

		SipCall mariaCall = mariaPhone.createSipCall();
		assertTrue(mariaCall.initiateOutgoingMessage(mariaOrg3Contact, null, "Test Message from maria"));
		assertTrue(mariaCall.waitForAuthorisation(5000));
		assertTrue(mariaCall.waitOutgoingMessageResponse(5000));

		assertTrue(mariaOrg3Call.waitForMessage(5000));
		Request msgReceived = mariaOrg3Call.getLastReceivedMessageRequest();
		assertTrue(new String(msgReceived.getRawContent()).equals("Test Message from maria"));
	}

	@Test
	public void testClientSentToOtherClientSameOrganization () throws ParseException {

		SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(mariaPhone.register(uri, "maria", "qwerty1234RT", "sip:maria@127.0.0.1:5092", 3600, 3600));
		Credential mariaCred = new Credential("org2.restcomm.com","maria","qwerty1234RT");
		mariaPhone.addUpdateCredential(mariaCred);

		assertTrue(shoaibPhone.register(uri,"shoaib","qwerty1234RT","sip:shoaib@127.0.0.1:5093", 3600, 3600));
		Credential shoaibCread = new Credential("org2.restcomm.com","shoaib","qwerty1234RT");
		shoaibPhone.addUpdateCredential(shoaibCread);

		SipCall shoaibCall = shoaibPhone.createSipCall();
		shoaibCall.listenForMessage();

		SipCall mariaCall = mariaPhone.createSipCall();
		assertTrue(mariaCall.initiateOutgoingMessage(shoaibContact, null, "Test Message from maria"));
		assertTrue(mariaCall.waitForAuthorisation(5000));
		assertTrue(mariaCall.waitOutgoingMessageResponse(5000));

		assertTrue(shoaibCall.waitForMessage(5000));
		Request msgReceived = shoaibCall.getLastReceivedMessageRequest();
		assertTrue(new String(msgReceived.getRawContent()).equals("Test Message from maria"));
	}

	@Test
	public void testClientSentOutUsingSMPP () throws ParseException, InterruptedException {

		SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(mariaPhone.register(uri, "maria", "qwerty1234RT", "sip:maria@127.0.0.1:5092", 3600, 3600));
		Credential mariaCred = new Credential("127.0.0.1","maria","qwerty1234RT");
		mariaPhone.addUpdateCredential(mariaCred);

		SipCall mariaCall = mariaPhone.createSipCall();
		mariaCall.initiateOutgoingMessage("sip:9999@127.0.0.1:5080", null, "Test Message from maria");
		assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(8000));
		Thread.sleep(5000);
		assertTrue(mockSmppServer.isMessageReceived());
		SmppInboundMessageEntity inboundMessageEntity = mockSmppServer.getSmppInboundMessageEntity();
		assertNotNull(inboundMessageEntity);
		assertTrue(inboundMessageEntity.getSmppTo().equals("9999"));
		assertTrue(inboundMessageEntity.getSmppFrom().equals("maria"));
		assertTrue(inboundMessageEntity.getSmppContent().equals("Test Message from maria"));
	}

	@Deployment(name = "SmppTests", managed = true, testable = false)
	public static WebArchive createWebArchive() {
		WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
		final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
				.resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
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

package org.mobicents.servlet.restcomm.telephony;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;

import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.cafesip.sipunit.Credential;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for clients with or without VoiceURL (Bitbucket issue 115). Clients without VoiceURL can dial anything.
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class ClientsDialTest {

	private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114,
		49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32,
		73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13,
		10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116,
		61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84,
		80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80,
		67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
	private static final String body = new String(bytes);

	@ArquillianResource
	private Deployer deployer;
	@ArquillianResource
	URL deploymentUrl;

	private static SipStackTool tool1;
	private static SipStackTool tool2;
	private static SipStackTool tool3;

	//Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
	private SipStack mariaSipStack;
	private SipPhone mariaPhone;
	private String mariaContact = "sip:maria@127.0.0.1:5092";
	private String mariaRestcommClientSid;

	//Dimitris is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
	private SipStack dimitriSipStack;
	private SipPhone dimitriPhone;
	private String dimitriContact = "sip:dimitri@127.0.0.1:5093";
	private String dimitriRestcommClientSid;

	//Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML of the VoiceURL will be executed.
	private SipStack aliceSipStack;
	private SipPhone alicePhone;
	private String aliceContact = "sip:alice@127.0.0.1:5091";

	@BeforeClass 
	public static void beforeClass() throws Exception {
		tool1 = new SipStackTool("ClientsDialTest1");
		tool2 = new SipStackTool("ClientsDialTest2");
		tool3 = new SipStackTool("ClientsDialTest3");
	}

	@Before 
	public void before() throws Exception {

		aliceSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1","5091","127.0.0.1:5080");
		alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContact);

		mariaSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
		mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContact);

		dimitriSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1","5093","127.0.0.1:5080");
		dimitriPhone = dimitriSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, dimitriContact);

		mariaRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "maria" , "1234", null);
		dimitriRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "dimitri" , "1234", null);

	}

	@After public void after() throws Exception {
		if(mariaPhone != null) {
			mariaPhone.dispose();
		}
		if(mariaSipStack != null) {
			mariaSipStack.dispose();
		}

		if(aliceSipStack != null) {
			aliceSipStack.dispose();
		}
		if(alicePhone != null) {
			alicePhone.dispose();
		}

		if(dimitriSipStack != null) {
			dimitriSipStack.dispose();
		}
		if(dimitriPhone != null) {
			dimitriPhone.dispose();
		}		
	}


	@Test
	public void testRegisterClients() throws ParseException, InterruptedException{

		assertNotNull(mariaRestcommClientSid);
		assertNotNull(dimitriRestcommClientSid);

		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null,"127.0.0.1:5080");

		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
		assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 3600, 3600));
		assertTrue(dimitriPhone.register(uri, "dimitri", "1234", dimitriContact, 3600, 3600));

		Thread.sleep(1000);

		assertTrue(alicePhone.unregister(aliceContact, 0));
		assertTrue(mariaPhone.unregister(mariaContact, 0));
		assertTrue(dimitriPhone.unregister(dimitriContact, 0));
	}

	@Test
	public void testClientsCallEachOther() throws ParseException, InterruptedException {

		assertNotNull(mariaRestcommClientSid);
		assertNotNull(dimitriRestcommClientSid);

		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null,"127.0.0.1:5080");

		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
		Thread.sleep(3000);
		assertTrue(mariaPhone.register(uri, "maria", "1234", mariaContact, 3600, 3600));
		Thread.sleep(3000);
		assertTrue(dimitriPhone.register(uri, "dimitri", "1234", dimitriContact, 3600, 3600));
		Thread.sleep(3000);

		Credential c = new Credential("127.0.0.1", "maria", "1234");
		mariaPhone.addUpdateCredential(c);

		Thread.sleep(1000);

		//Maria initiates a call to Dimitri
		final SipCall mariaCall = mariaPhone.createSipCall();
		mariaCall.initiateOutgoingCall(mariaContact, dimitriContact, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(mariaCall);
		assertTrue(mariaCall.waitForAuthorisation(3000));

		//Start a new thread for Dimitri to wait disconnect
		new Thread( new Runnable() {
			@Override
			public void run() {
				final SipCall dimitriCall = dimitriPhone.createSipCall();
				dimitriCall.listenForIncomingCall();

				assertTrue(dimitriCall.waitForIncomingCall(3000));
				assertTrue(dimitriCall.sendIncomingCallResponse(180, "Ringing", 1800));
				assertTrue(dimitriCall.sendIncomingCallResponse(200, "OK", 1800));

			}
		}).start();




		assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
		int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
		assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

		if(responseMaria == Response.TRYING) {
			assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
		mariaCall.sendInviteOkAck();
		assertTrue(!(mariaCall.getLastReceivedResponse().getStatusCode() >= 400));

		assertTrue(mariaCall.disconnect());

	}

	@Deployment(name="ClientsDialTest", managed=true, testable=false)
	public static WebArchive createWebArchiveNoGw() {
		String version = "6.1.2-TelScale-SNAPSHOT";
		final WebArchive archive = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.application:war:" + version)
				.withoutTransitivity().asSingle(WebArchive.class);
		JavaArchive dependency = ShrinkWrapMaven.resolver()
				.resolve("commons-configuration:commons-configuration:jar:1.7")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("jain:jain-mgcp-ri:jar:1.0")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("org.mobicents.media.client:mgcp-driver:jar:3.0.0.Final")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("joda-time:joda-time:jar:2.0")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.iSpeech:iSpeech:jar:1.0.1")
				.offline().withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.asr:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.fax:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.tts.acapela:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.tts.api:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.mgcp:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.http:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.interpreter:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.sms.api:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.sms:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.telephony.api:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		dependency = ShrinkWrapMaven.resolver()
				.resolve("com.telestax.servlet:restcomm.telephony:jar:" + version)
				.withoutTransitivity().asSingle(JavaArchive.class);
		archive.addAsLibrary(dependency);
		archive.delete("/WEB-INF/sip.xml");
		archive.delete("/WEB-INF/conf/restcomm.xml");
		archive.delete("/WEB-INF/data/hsql/restcomm.script");
		archive.addAsWebInfResource("sip.xml");
		archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
		archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
		archive.addAsWebResource("dial-conference-entry.xml");
		archive.addAsWebResource("dial-fork-entry.xml");
		archive.addAsWebResource("dial-uri-entry.xml");
		archive.addAsWebResource("dial-client-entry.xml");
		archive.addAsWebResource("dial-number-entry.xml");
		return archive;
	}

}

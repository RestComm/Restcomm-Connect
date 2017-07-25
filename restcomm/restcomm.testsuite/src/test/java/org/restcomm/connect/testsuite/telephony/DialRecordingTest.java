package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
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
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by gvagenas on 08/01/2017.
 */
@RunWith(Arquillian.class)
public class DialRecordingTest {

	private final static Logger logger = Logger.getLogger(DialRecordingTest.class.getName());

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

	private String dialRestcomm = "sip:1111@127.0.0.1:5080";

	private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
	private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

	@BeforeClass
	public static void beforeClass() throws Exception {
		tool1 = new SipStackTool("DialActionTest1");
		tool2 = new SipStackTool("DialActionTest2");
		tool3 = new SipStackTool("DialActionTest3");
		tool4 = new SipStackTool("DialActionTest4");
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

	private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\" record=\"true\"><Client>alice</Client></Dial></Response>";

	@Test
	public synchronized void testDialClientAlice_BobDisconnects() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(dialClientRcml)));

		// Phone2 register as alice
		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

		// Prepare second phone to receive call
		SipCall aliceCall = alicePhone.createSipCall();
		aliceCall.listenForIncomingCall();

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
		assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
		String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
		assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
				null));
		assertTrue(aliceCall.waitForAck(50 * 1000));

		Thread.sleep(3000);

		// hangup.
		aliceCall.listenForDisconnect();
		bobCall.disconnect();

		assertTrue(aliceCall.waitForDisconnect(30 * 1000));
		assertTrue(aliceCall.respondToDisconnect());

		Thread.sleep(500);
		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertEquals(3.0, duration, 1.0);

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);
	}

	@Test
	public synchronized void testDialClientAliceGerRecordindNoFile() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(dialClientRcml)));

		// Phone2 register as alice
		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

		// Prepare second phone to receive call
		SipCall aliceCall = alicePhone.createSipCall();
		aliceCall.listenForIncomingCall();

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
		assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
		String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
		assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
				null));
		assertTrue(aliceCall.waitForAck(50 * 1000));

		Thread.sleep(3000);

		// hangup.
		aliceCall.listenForDisconnect();
		bobCall.disconnect();

		assertTrue(aliceCall.waitForDisconnect(30 * 1000));
		assertTrue(aliceCall.respondToDisconnect());

		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertTrue(duration==3.0);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(6000)
				.setConnectionRequestTimeout(6000)
				.setSocketTimeout(6000)
				.setCookieSpec(CookieSpecs.STANDARD).build();

		CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
		String recordingUri = recording.get(0).getAsJsonObject().get("file_uri").getAsString();
		HttpRequest request = new HttpGet(recordingUri);
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = client.execute((HttpUriRequest) request);
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertTrue(httpResponse != null);
		int code = httpResponse.getStatusLine().getStatusCode();
		assertEquals(404, code);

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);

	}

	@Test
	public synchronized void testDialClientAlice_AliceDisconnects() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(dialClientRcml)));

		// Phone2 register as alice
		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

		// Prepare second phone to receive call
		SipCall aliceCall = alicePhone.createSipCall();
		aliceCall.listenForIncomingCall();

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
		assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
		String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
		assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
				null));
		assertTrue(aliceCall.waitForAck(50 * 1000));

		Thread.sleep(3000);

		// hangup.
		bobCall.listenForDisconnect();
		aliceCall.disconnect();

		assertTrue(bobCall.waitForDisconnect(30 * 1000));
		assertTrue(bobCall.respondToDisconnect());

		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertTrue(duration==3.0);

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);
	}

	final String recordCall = "<Response><Record timeout=\"15\" maxLength=\"60\"/></Response>";
	@Test
	public synchronized void testRecordCall() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(recordCall)));

		// Phone2 register as alice
		SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
		assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

		// Prepare second phone to receive call
		SipCall aliceCall = alicePhone.createSipCall();
		aliceCall.listenForIncomingCall();

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(1,liveCalls);
		assertEquals(1, liveCallsArraySize);

		Thread.sleep(3000);

		bobCall.disconnect();

		Thread.sleep(3000);

		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertEquals(3.0, duration,1);

		metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);
	}

	final String recordCallWithAction = "<Response><Record timeout=\"15\" maxLength=\"60\" action=\"http://127.0.0.1:8090/record-action\"/></Response>";
	final String hangupRcml = "<Response><Hangup/></Response>";
	@Test
	public synchronized void testRecordCallWithAction() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(recordCallWithAction)));

		stubFor(post(urlPathEqualTo("/record-action"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(hangupRcml)));

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		DateTime start = DateTime.now();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(1,liveCalls);
		assertEquals(1, liveCallsArraySize);

		Thread.sleep(3000);

		bobCall.disconnect();
		DateTime end = DateTime.now();

		Thread.sleep(500);

		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double recordedDuration = (end.getMillis() - start.getMillis())/1000;
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertEquals(recordedDuration, duration,0);

		logger.info("\n\n &&&&&& About to check liveCalls &&&&&& \n");

		metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);
	}

	@Test
	public synchronized void testRecordCallWithActionWithMaxRecordingReached() throws InterruptedException, ParseException {
		stubFor(get(urlPathEqualTo("/1111"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(recordCallWithAction)));

		stubFor(post(urlPathEqualTo("/record-action"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/xml")
						.withBody(hangupRcml)));

		// Create outgoing call with first phone
		final SipCall bobCall = bobPhone.createSipCall();
		bobCall.initiateOutgoingCall(bobContact, dialRestcomm, null, body, "application", "sdp", null, null);
		assertLastOperationSuccess(bobCall);
		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		final int response = bobCall.getLastReceivedResponse().getStatusCode();
		assertTrue(response == Response.TRYING || response == Response.RINGING);

		if (response == Response.TRYING) {
			assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
			assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
		}

		assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
		assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

		bobCall.sendInviteOkAck();
		DateTime start = DateTime.now();
		assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));
		String callSid = bobCall.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim();

		JsonObject metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		int liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		int liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(1,liveCalls);
		assertEquals(1, liveCallsArraySize);

		bobCall.listenForDisconnect();
		assertTrue(bobCall.waitForDisconnect(70000));
		assertTrue(bobCall.respondToDisconnect());
		DateTime end = DateTime.now();

		Thread.sleep(500);

		//Check recording
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double recordedDuration = (end.getMillis() - start.getMillis())/1000;
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertEquals(recordedDuration, duration,0);

		logger.info("\n\n &&&&&& About to check liveCalls &&&&&& \n");

		metrics = MonitoringServiceTool.getInstance().getMetrics(deploymentUrl.toString(),adminAccountSid, adminAuthToken);
		assertNotNull(metrics);
		liveCalls = metrics.getAsJsonObject("Metrics").get("LiveCalls").getAsInt();
		logger.info("LiveCalls: "+liveCalls);
		liveCallsArraySize = metrics.getAsJsonArray("LiveCallDetails").size();
		logger.info("LiveCallsArraySize: "+liveCallsArraySize);
		assertEquals(0,liveCalls);
		assertEquals(0, liveCallsArraySize);
	}

	@Test
	public void testGetRecordingWithOldS3Url() {
		String callSid = "CA2d3f6354e75e46b3ac76f534129ff511";
		JsonArray recording = RestcommCallsTool.getInstance().getCallRecordings(deploymentUrl.toString(),adminAccountSid,adminAuthToken,callSid);
		assertNotNull(recording);
		assertEquals(1, recording.size());
		double duration = recording.get(0).getAsJsonObject().get("duration").getAsDouble();
		assertTrue(duration==3.0);
		//Since for this test the S3Accesstoll is not enabled, the file_uri will still point to the old S3 URL.
		//Check test org.restcomm.connect.testsuite.telephony.DialRecordingS3UploadTest_NoneSecurity.testGetRecordingWithOldS3Url()
		assertTrue(recording.get(0).getAsJsonObject().get("file_uri").getAsString().startsWith("https://s3.amazonaws.com"));
	}

	@Deployment(name = "DialRecordingTest", managed = true, testable = false)
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
		archive.delete("/WEB-INF/classes/application.conf");
		archive.addAsWebInfResource("sip.xml");
		archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
		archive.addAsWebInfResource("restcomm.script_DialRecording", "data/hsql/restcomm.script");
		archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
		logger.info("Packaged Test App");
		return archive;
	}

}

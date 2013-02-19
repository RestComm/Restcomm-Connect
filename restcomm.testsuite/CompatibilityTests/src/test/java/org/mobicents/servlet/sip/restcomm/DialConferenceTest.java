package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.arquillian.mediaserver.api.EmbeddedMediaserver;
import org.mobicents.arquillian.mediaserver.api.MgcpEventListener;
import org.mobicents.arquillian.mediaserver.api.MgcpUnitRequest;
import org.mobicents.arquillian.mediaserver.api.annotations.Mediaserver;
import org.mobicents.commtesting.MgcpUnit;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class DialConferenceTest extends AbstractTest {

	@ArquillianResource
	URL deploymentUrl;
	String endpoint;

	@Mediaserver(IVR=20,CONF=20,RELAY=20)
	private EmbeddedMediaserver mediaserver;
	private MgcpUnit mgcpUnit;
	private MgcpEventListener mgcpEventListener;
	private static String appName = "dialConftest-app.xml";

	private SipStack firstStack;
	private SipCall sipCallFirst;
	private SipPhone sipPhoneFirst;

	private SipStack secondStack;
	private SipCall sipCallSecond;
	private SipPhone sipPhoneSecond;

	private SipStack thirdStack;
	private SipCall sipCallThird;
	private SipPhone sipPhoneThird;



	private final int TIMEOUT = 20000;
	private final int smallTIMEOUT = 1000;

	private static SipStackTool sipStackToolFirst;
	private static SipStackTool sipStackToolSecond;
	private static SipStackTool sipStackToolThird;
	private TwilioRestClient client;
	private Account account;
	private IncomingPhoneNumber incomingPhoneNumber;
	private String uri = "sip:+14321@127.0.0.1:5070";
	private String appURL;

	@BeforeClass
	public static void beforeClass(){
		sipStackToolFirst = new SipStackTool("DialConfVerbTestFirst");
		sipStackToolSecond = new SipStackTool("DialConfVerbTestSecond");
		sipStackToolThird = new SipStackTool("DialConfVerbTestThird");
	}
	
	@AfterClass
	public static void afterClass(){
		sipStackToolFirst = null;
		sipStackToolSecond = null;
		sipStackToolThird = null;
		appName = null;
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the Sender sipCall
		firstStack = sipStackToolFirst.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5080", "127.0.0.1:5070");
		sipPhoneFirst = firstStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:first@here.com");
		sipCallFirst = sipPhoneFirst.createSipCall();

		//Create the Receiver sipCall and start listening for messages
		secondStack = sipStackToolSecond.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5070");
		sipPhoneSecond = secondStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:second@here.com");
		sipCallSecond = sipPhoneSecond.createSipCall();
		sipCallSecond.listenForIncomingCall();

		//Create the Receiver sipCall and start listening for messages
		thirdStack = sipStackToolThird.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5070");
		sipPhoneThird = thirdStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:third@here.com");
		sipCallThird = sipPhoneThird.createSipCall();
		sipCallThird.listenForIncomingCall();

		endpoint = super.getEndpoint(deploymentUrl.toString());
		if(client==null)
			client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		if(account==null)
			account = client.getAccount();
		
		if(incomingPhoneNumber==null){
			appURL = endpoint+"/demo/"+appName;
			incomingPhoneNumber = super.createPhoneNumber(appURL, account);
		}
			
		mgcpUnit = new MgcpUnit();
		mgcpEventListener = mgcpUnit.getMgcpEventListener();
		mediaserver.registerListener(mgcpEventListener);

	}

	@After
	public void tearDown() throws Exception
	{
		if(sipCallFirst != null)	sipCallFirst.disposeNoBye();
		if(sipPhoneFirst != null) sipPhoneFirst.dispose();
		if(firstStack != null) firstStack.dispose();

		if(sipCallSecond != null)	sipCallSecond.disposeNoBye();
		if(sipPhoneSecond != null) sipPhoneSecond.dispose();
		if(secondStack != null) secondStack.dispose();
		
		if(sipCallThird != null) sipCallThird.disposeNoBye();
		if(sipPhoneThird != null) sipPhoneThird.dispose();
		if(thirdStack != null) thirdStack.dispose();

		incomingPhoneNumber.delete();
		incomingPhoneNumber = null;
		
		mgcpEventListener = null;
		mgcpUnit = null;
	}

	@Deployment(testable=false)
	public static WebArchive createWebArchive(){
		return AbstractTest.createWebArchive("compattests-restcomm.xml", appName);
	}

	@Test //Issue 153: http://code.google.com/p/restcomm/issues/detail?id=153
	public void testConferenceShutdown(){
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals(appURL));

		int iterations = 1;

		while(iterations>0) {

			//SDP for the INVITE
			byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
			String body = new String(bodyByte);

			//First participant calling in the conference
			sipCallFirst.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
			assertTrue(sipCallFirst.waitForAnswer(TIMEOUT));
			assertEquals(Response.OK, sipCallFirst.getLastReceivedResponse().getStatusCode());
			assertTrue(sipCallFirst.sendInviteOkAck());


			//Second participant calling in the conference
			sipCallSecond.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
			assertTrue(sipCallSecond.waitForAnswer(TIMEOUT));
			assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			assertTrue(sipCallSecond.sendInviteOkAck());

			//Disconnect and try again
			try {
				Thread.sleep(TIMEOUT);
				assertTrue(sipCallSecond.disconnect());
				Thread.sleep(smallTIMEOUT);
				assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			} catch (InterruptedException e3) {}

			//Second participant calling in the conference
			sipCallSecond.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
			assertTrue(sipCallSecond.waitForAnswer(TIMEOUT));
			assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			assertTrue(sipCallSecond.sendInviteOkAck());
			
			//Disconnect and try again
			try {
				Thread.sleep(TIMEOUT);
				assertTrue(sipCallSecond.disconnect());
				Thread.sleep(smallTIMEOUT);
				assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			} catch (InterruptedException e3) {}

			//Second participant calling in the conference
			sipCallSecond.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
			assertTrue(sipCallSecond.waitForAnswer(TIMEOUT));
			assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			assertTrue(sipCallSecond.sendInviteOkAck());

			//Disconnect and try again
			try {
				Thread.sleep(TIMEOUT);
				assertTrue(sipCallSecond.disconnect());
				Thread.sleep(smallTIMEOUT);
				assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			} catch (InterruptedException e3) {}

			//Second participant calling in the conference
			sipCallSecond.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
			assertTrue(sipCallSecond.waitForAnswer(TIMEOUT));
			assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
			assertTrue(sipCallSecond.sendInviteOkAck());

			//Wait for announcements to finish.
			try {
				Thread.sleep(TIMEOUT);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			assertTrue(sipCallFirst.disconnect());
			try {
				Thread.sleep(smallTIMEOUT);
			} catch (InterruptedException e) {}
			assertEquals(Response.OK, sipCallFirst.getLastReceivedResponse().getStatusCode());
			
			assertTrue(sipCallSecond.disconnect());
			try {
				Thread.sleep(smallTIMEOUT);
			} catch (InterruptedException e) {}
			assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());

			assertTrue(mgcpEventListener.verifyAll());
			mgcpEventListener.clearAll();

			//Wait and then check for deadlocks
			try {
				Thread.sleep(TIMEOUT);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			//Deadlock detection
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			long[] monitorDeadlockedThreads = bean.findMonitorDeadlockedThreads();
			long[] deadlockedThreads = bean.findDeadlockedThreads();
			assertTrue(monitorDeadlockedThreads == null);
			assertTrue(deadlockedThreads == null);

			iterations--;
		}		
	}


	@Test //Issue 153: http://code.google.com/p/restcomm/issues/detail?id=153
	public void testConferenceShutdownThreeParticipants(){
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals(appURL));

		//SDP for the INVITE
		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);

		//First participant calling in the conference
		sipCallFirst.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
		assertTrue(sipCallFirst.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCallFirst.getLastReceivedResponse().getStatusCode());
		assertTrue(sipCallFirst.sendInviteOkAck());

		try {
			Thread.sleep(smallTIMEOUT);
		} catch (InterruptedException e2) {}

		//Second participant calling in the conference
		sipCallSecond.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
		assertTrue(sipCallSecond.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());
		assertTrue(sipCallSecond.sendInviteOkAck());

		try {
			Thread.sleep(smallTIMEOUT);
		} catch (InterruptedException e2) {}

		//Third participant calling in the conference
		sipCallThird.initiateOutgoingCall(null, uri, null, body, "application", "sdp", null, null);
		assertTrue(sipCallThird.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCallThird.getLastReceivedResponse().getStatusCode());
		assertTrue(sipCallThird.sendInviteOkAck());

		//Wait for announcements to finish.
		try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		Collection<MgcpUnitRequest> mgcpUnitRequests = mgcpEventListener.getPlayAnnoRequestsReceived();
		for (Iterator<MgcpUnitRequest> iterator = mgcpUnitRequests.iterator(); iterator.hasNext();) {
			MgcpUnitRequest mgcpUnitRequest = (MgcpUnitRequest) iterator.next();
			assertTrue(mgcpEventListener.checkForSuccessfulResponse(mgcpUnitRequest.getTxId()));
		}

		assertTrue(sipCallFirst.disconnect());
		try {
			Thread.sleep(smallTIMEOUT);
		} catch (InterruptedException e) {}
		assertEquals(Response.OK, sipCallFirst.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCallSecond.disconnect());
		try {
			Thread.sleep(smallTIMEOUT);
		} catch (InterruptedException e) {}
		assertEquals(Response.OK, sipCallSecond.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCallThird.disconnect());
		try {
			Thread.sleep(smallTIMEOUT);
		} catch (InterruptedException e) {}
		assertEquals(Response.OK, sipCallThird.getLastReceivedResponse().getStatusCode());

		assertTrue(mgcpEventListener.verifyAll());
		mgcpEventListener.clearAll();

		//Wait and then check for deadlocks
		try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		//Deadlock detection
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		long[] monitorDeadlockedThreads = bean.findMonitorDeadlockedThreads();
		long[] deadlockedThreads = bean.findDeadlockedThreads();
		assertTrue(monitorDeadlockedThreads == null);
		assertTrue(deadlockedThreads == null);
	}

}

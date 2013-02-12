package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sip.address.SipURI;
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
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Client;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class DialClientVerbTest extends AbstractTest {
	
	@ArquillianResource
	URL deploymentUrl;
	String endpoint;
	
	@Mediaserver(IVR=10,CONF=10,RELAY=10)
	private EmbeddedMediaserver mediaserver;
	private MgcpUnit mgcpUnit;
	private MgcpEventListener mgcpEventListener;
	private static String appName = "dialtestClient-app.xml";
	
	private SipStack sender;

	private SipCall sipCallSender;
	private SipPhone sipPhoneSender;

	private SipStack receiver;

	private SipCall sipCallReceiver;
	private SipPhone sipPhoneReceiver;

	
	private final int TIMEOUT = 10000;	

	private static SipStackTool sipStackToolSender;
	private static SipStackTool sipStackToolReceiver;
	private static TwilioRestClient twilioRestclient;
	private static Client client;
	private static Account account;
	private static IncomingPhoneNumber incomingPhoneNumber;
	private String uri = "sip:+14321@127.0.0.1:5070";
	private static String appURL;
	
	@BeforeClass
	public static void beforeClass(){
		sipStackToolSender = new SipStackTool("DialVerbTestSender");
		sipStackToolReceiver = new SipStackTool("DialVerbTestReceiver");
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the Sender sipCall
		sender = sipStackToolSender.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5080", "127.0.0.1:5070");
		sipPhoneSender = sender.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:6001@here.com");
		sipCallSender = sipPhoneSender.createSipCall();

		//Create the Receiver sipCall and start listening for messages
		receiver = sipStackToolReceiver.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5070");
		sipPhoneReceiver = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:georgeClient@127.0.0.1:5060");
		sipCallReceiver = sipPhoneReceiver.createSipCall();
		sipCallReceiver.listenForIncomingCall();
		

		endpoint = super.getEndpoint(deploymentUrl.toString());
		if(twilioRestclient==null)
			twilioRestclient = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		if(account==null)
			account = twilioRestclient.getAccount();
		
		if(incomingPhoneNumber==null){
			appURL = endpoint+"/demo/"+appName;
			incomingPhoneNumber = super.createPhoneNumber(appURL, account);
		}
			
		if(client==null)
			createClient();
		
		mgcpUnit = new MgcpUnit();
		mgcpEventListener = mgcpUnit.getMgcpEventListener();
		mediaserver.registerListener(mgcpEventListener);
		
	}

	@After
	public void tearDown() throws Exception
	{
		if(sipCallSender != null)	sipCallSender.disposeNoBye();
		if(sipPhoneSender != null) sipPhoneSender.dispose();
		if(sender != null) sender.dispose();
		
		if(sipCallReceiver != null)	sipCallReceiver.disposeNoBye();
		if(sipPhoneReceiver != null) sipPhoneReceiver.dispose();
		if(receiver != null) receiver.dispose();
		
		incomingPhoneNumber.delete();
		incomingPhoneNumber = null;
		mgcpEventListener = null;
		mgcpUnit = null;
	}

	@Deployment(testable=false)
	public static WebArchive createWebArchive(){
		return AbstractTest.createWebArchive("compattests-restcomm.xml", appName);
	}

	public void createClient() throws TwilioRestException{
		ClientFactory clientFactory = account.getClientFactory();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Login","rest");
		parameters.put("Password","comm");
		parameters.put("FriendlyName", "georgeClient");
		parameters.put("VoiceUrl", endpoint+"/demo/hello-world.xml");
		parameters.put("VoiceMethod", "POST");
		parameters.put("VoiceFallbackUrl", endpoint+"/demo/hello-world.xml");
		parameters.put("VoiceFallbackMethod", "POST");
		parameters.put("StatusCallback", endpoint+"/demo/hello-world.xml");
		parameters.put("StatusCallbackMethod", "POST");
		parameters.put("VoiceCallerIdLookup", "false");
		
		client = clientFactory.create(parameters);
	}
	
	@Test 
	public void dialClientTest() throws ParseException, TwilioRestException, ClassNotFoundException{	
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals(appURL));

		SipURI registerUri = sipCallReceiver.getAddressFactory().createSipURI("georgeClient", "127.0.0.1:5060");
		assertTrue(sipPhoneReceiver.register(registerUri,"rest", "comm", "sip:georgeClient@127.0.0.1:5060", 3000, 3000));
		
		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		sipCallSender.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		assertTrue(sipCallSender.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCallSender.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCallSender.sendInviteOkAck());
		assertTrue(sipCallSender.listenForDisconnect());
		
		assertTrue(sipCallReceiver.waitForIncomingCall(TIMEOUT));
		String receivedBody = new String(sipCallReceiver.getLastReceivedRequest().getRawContent());
		assertTrue(sipCallReceiver.sendIncomingCallResponse(Response.OK, "OK", TIMEOUT, receivedBody, "application", "sdp", null, null));
		assertTrue(sipCallReceiver.waitForAck(TIMEOUT));

		try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertTrue(sipCallReceiver.disconnect());
		assertTrue(sipCallSender.waitForDisconnect(TIMEOUT));
		assertTrue(sipCallSender.respondToDisconnect());
		assertTrue(Response.OK == sipCallReceiver.getLastReceivedResponse().getStatusCode());
		
		Collection<MgcpUnitRequest> mgcpUnitRequests = mgcpEventListener.getPlayAnnoRequestsReceived();
		assertTrue(mgcpUnitRequests.size()==1);
		for (Iterator<MgcpUnitRequest> iterator = mgcpUnitRequests.iterator(); iterator.hasNext();) {
			MgcpUnitRequest mgcpUnitRequest = (MgcpUnitRequest) iterator.next();
			assertTrue(mgcpEventListener.checkForSuccessfulResponse(mgcpUnitRequest.getTxId()));	
		}
		
		assertTrue(sipCallReceiver.getLastReceivedResponse().getStatusCode()==200);
		assertTrue(!(sipCallSender.getLastReceivedResponse().getStatusCode()>400));
	}
	
}

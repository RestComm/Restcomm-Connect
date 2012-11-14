package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mobicents.api.annotations.GetDeployableContainer;
import org.jboss.arquillian.container.mss.extension.ContainerManagerTool;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.factory.IncomingPhoneNumberFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Client;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 * Needs MMS to be be running before test execution
 * 
 */

public class MediaTest extends AbstractEndpointTest {

	private Logger logger = Logger.getLogger(MediaTest.class);
	public String endpoint = "http://127.0.0.1:8888/restcomm";

	@ArquillianResource
	private Deployer deployer;

	private SipStack receiver;

	private SipCall sipCall;
	private SipPhone sipPhone;

	private final int TIMEOUT = 10000;	

	@GetDeployableContainer
	private ContainerManagerTool containerManager = null;

	private static SipStackTool sipStackTool;
	private TwilioRestClient twilioClient;
	private IncomingPhoneNumber incomingPhoneNumber;
	private String uri = "sip:14321@127.0.0.1:5070";
	
	@BeforeClass
	public static void beforeClass(){
		sipStackTool = new SipStackTool("MediaTest");
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the sipCall and start listening for messages
		receiver = sipStackTool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5080", "127.0.0.1:5070");
		sipPhone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:RestcommUser@there.com");
		sipCall = sipPhone.createSipCall();
		sipCall.listenForIncomingCall();
	}

	@After
	public void tearDown() throws Exception
	{

		if(sipCall != null)	sipCall.disposeNoBye();
		if(sipPhone != null) sipPhone.dispose();
		if(receiver != null) receiver.dispose();
//		if(incomingPhoneNumber != null) incomingPhoneNumber = null;
//		if(twilioClient != null) twilioClient = null;
		
	}

	private void createPhoneNumber() throws TwilioRestException{
			twilioClient = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
			final Account account = twilioClient.getAccount();
			final IncomingPhoneNumberFactory factory = account.getIncomingPhoneNumberFactory();
			
			final Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("PhoneNumber", "+14321");
			parameters.put("VoiceUrl", "http://restcomm-demo.appspot.com/app/voice/restcomm2.xml");
//			parameters.put("VoiceUrl", endpoint+"/demo/hello-world.xml");
			parameters.put("VoiceMethod", "POST");
			parameters.put("VoiceFallbackUrl", endpoint+"/demo/hello-world.xml");
			parameters.put("VoiceFallbackMethod", "POST");
			parameters.put("StatusCallback", endpoint+"/demo/hello-world.xml");
			parameters.put("StatusCallbackMethod", "POST");
			parameters.put("VoiceCallerIdLookup", "false");
			// parameters.put("VoiceApplicationSid", "");
			parameters.put("SmsUrl", endpoint+"/demo/hello-world.xml");
			parameters.put("SmsMethod", "POST");
			parameters.put("SmsFallbackUrl", endpoint+"/demo/hello-world.xml");
			parameters.put("SmsFallbackMethod", "POST");
			incomingPhoneNumber = factory.create(parameters);
			
	}

	//Issue: http://code.google.com/p/restcomm/issues/detail?id=87
	@Test
	public void sendByeAfterAck() throws ParseException, TwilioRestException{	
		createPhoneNumber();
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://restcomm-demo.appspot.com/app/voice/restcomm2.xml"));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		assertTrue(sipCall.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCall.sendInviteOkAck());
		assertTrue(sipCall.disconnect());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
	}

	@Test @Ignore
	public void sendCancel() throws ParseException, TwilioRestException{	
		createPhoneNumber();
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://restcomm-demo.appspot.com/app/voice/restcomm2.xml"));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		SipTransaction cancelTransaction = sipCall.sendCancel();
		assertNotNull(cancelTransaction);
		assertTrue(sipCall.waitForCancelResponse(cancelTransaction, TIMEOUT));

		assertTrue(sipCall.waitOutgoingCallResponse());
		assertEquals(Response.REQUEST_TERMINATED, sipCall.getLastReceivedResponse().getStatusCode());
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
	}
	
	@Test
	public void sendByeAfterAckMany() throws ParseException, TwilioRestException{	
		createPhoneNumber();
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://restcomm-demo.appspot.com/app/voice/restcomm2.xml"));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		
		int iter = 16;
		
		while (iter != 0){
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		assertTrue(sipCall.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCall.sendInviteOkAck());
		assertTrue(sipCall.disconnect());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		iter--;
		}
	}
	
	@Test
	public void sendBye() throws ParseException, TwilioRestException{	
		createPhoneNumber();
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://restcomm-demo.appspot.com/app/voice/restcomm2.xml"));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		assertTrue(sipCall.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCall.sendInviteOkAck());
	
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
		
		assertTrue(sipCall.disconnect());
		
	}
	
	@Test
	public void testManyCalls() throws ParseException, TwilioRestException{	
		createPhoneNumber();
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://restcomm-demo.appspot.com/app/voice/restcomm2.xml"));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		int iter = 32;
		
		while (iter != 0){
		logger.info("Iteration: "+iter);
		
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);
		
		assertTrue(sipCall.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCall.sendInviteOkAck());
	
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
		
		assertTrue(sipCall.disconnect());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		iter--;
		}
	}
	
}

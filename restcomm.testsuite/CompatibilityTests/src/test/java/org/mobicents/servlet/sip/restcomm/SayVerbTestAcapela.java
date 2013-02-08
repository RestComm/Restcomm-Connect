package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import com.twilio.sdk.resource.factory.IncomingPhoneNumberFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

/**
 * Tests the SAY verb and the Acapela TTS SpeechSynthesizer service. 
 * In order to run, you need to specify the username/password details at the  src/test/resources/compattests-restcomm_Acapela.xml
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

@RunWith(Arquillian.class)
public class SayVerbTestAcapela extends AbstractTest {
	
	@ArquillianResource
	URL deploymentUrl;
	String endpoint;
	
	@Mediaserver(CONF=2,IVR=2,RELAY=2)
	private EmbeddedMediaserver mediaserver;
	private MgcpUnit mgcpUnit;
	private MgcpEventListener mgcpEventListener;
	private static String appName = "saytest-app.xml";
	
	private SipStack receiver;

	private SipCall sipCall;
	private SipPhone sipPhone;

	private final int TIMEOUT = 10000;	

	private static SipStackTool sipStackTool;
	private static TwilioRestClient client;
	private static Account account;
	private static IncomingPhoneNumber incomingPhoneNumber;
	private String uri = "sip:+14321@127.0.0.1:5070";
	private static String appURL;
	
	@BeforeClass
	public static void beforeClass(){
		sipStackTool = new SipStackTool("SayVerbTest");
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the sipCall and start listening for messages
		receiver = sipStackTool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5080", "127.0.0.1:5070");
		sipPhone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:RestcommUser@there.com");
		sipCall = sipPhone.createSipCall();
		sipCall.listenForIncomingCall();

		endpoint = super.getEndpoint(deploymentUrl.toString());
		if(client==null)
			client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		if(account==null)
			account = client.getAccount();

		if(incomingPhoneNumber==null)
			createPhoneNumber();
		
		mgcpUnit = new MgcpUnit();
		mgcpEventListener = mgcpUnit.getMgcpEventListener();
		mediaserver.registerListener(mgcpEventListener);
		
	}

	@After
	public void tearDown()
	{
		if(sipCall != null)	sipCall.disposeNoBye();
		if(sipPhone != null) sipPhone.dispose();
		if(receiver != null) receiver.dispose();

		mgcpEventListener = null;
		mgcpUnit = null;
	}

	@Deployment(testable=false)
	public static WebArchive createWebArchive(){
		return AbstractTest.createWebArchive("compattests-restcomm_Acapela.xml", appName);
	}

	private void createPhoneNumber() throws TwilioRestException{
		appURL = endpoint+"/demo/"+appName;
		IncomingPhoneNumberFactory factory = account.getIncomingPhoneNumberFactory();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("PhoneNumber", "+14321");
		parameters.put("VoiceUrl", appURL);
		parameters.put("VoiceMethod", "POST");
		parameters.put("VoiceFallbackUrl", appURL);
		parameters.put("VoiceFallbackMethod", "POST");
		parameters.put("StatusCallback", appURL);
		parameters.put("StatusCallbackMethod", "POST");
		parameters.put("VoiceCallerIdLookup", "false");
		parameters.put("SmsUrl", appURL);
		parameters.put("SmsMethod", "POST");
		parameters.put("SmsFallbackUrl", appURL);
		parameters.put("SmsFallbackMethod", "POST");
		incomingPhoneNumber = factory.create(parameters);

	}

	@Test
	public void testSayVerbAcapela() throws ParseException, TwilioRestException, ClassNotFoundException{	
		assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
		assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+14321"));
		assertTrue(incomingPhoneNumber.getVoiceUrl().equals(appURL));

		byte[] bodyByte = new byte[]{118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10};
		String body = new String(bodyByte);
		sipCall.initiateOutgoingCall("sip:RestcommUser@there.com", uri, null, body, "application", "sdp", null, null);

		assertTrue(sipCall.waitForAnswer(TIMEOUT));
		assertEquals(Response.OK, sipCall.getLastReceivedResponse().getStatusCode());

		assertTrue(sipCall.sendInviteOkAck());

		assertTrue(sipCall.waitForDisconnect(TIMEOUT));
		assertTrue(sipCall.respondToDisconnect());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Collection<MgcpUnitRequest> mgcpUnitRequests = mgcpEventListener.getPlayAnnoRequestsReceived();
		assertTrue(mgcpUnitRequests.size()>0);
		for (Iterator iterator = mgcpUnitRequests.iterator(); iterator.hasNext();) {
			MgcpUnitRequest mgcpUnitRequest = (MgcpUnitRequest) iterator.next();
			assertTrue(mgcpEventListener.checkForSuccessfulResponse(mgcpUnitRequest.getTxId()));
			
		}
		assertTrue(!(sipCall.getLastReceivedResponse().getStatusCode()>400));
	}
	
}

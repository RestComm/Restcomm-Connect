package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.arquillian.mediaserver.api.annotations.Mediaserver;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Call;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@RunWith(Arquillian.class)
@Mediaserver(IVR=1,CONF=1,RELAY=1)
public class OutgoingCallTest extends AbstractTest {

	@ArquillianResource
	URL deploymentUrl;
	String endpoint;

	private SipStack sipStackA;
	private SipCall sipCallA;
	private SipPhone sipPhoneA;
	private static SipStackTool sipStackToolA;

	private static TwilioRestClient client;
	private static Account account;

	@BeforeClass
	public static void beforeClass(){
		sipStackToolA = new SipStackTool("OutgoingCallTestA");
	}

	@Before
	public void setUp() throws Exception
	{
		//Create the sipCall and start listening for messages
		sipStackA = sipStackToolA.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5070");
		sipPhoneA = sipStackA.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5070, "sip:+14321@127.0.0.1:5060");
		sipCallA = sipPhoneA.createSipCall();
		sipCallA.listenForIncomingCall();

		endpoint = super.getEndpoint(deploymentUrl.toString());
		if(client==null)
			client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		if(account==null)
			account = client.getAccount();
	}

	@Deployment(testable=false)
	public static WebArchive createWebArchive(){
		return AbstractTest.createWebArchive("mediatest-restcomm.xml");
	}

	@Test
	public void testOutgoing() throws TwilioRestException, ParseException, InterruptedException{
		CallFactory callFactory = account.getCallFactory();

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("From", "+18765");
		parameters.put("To", "4321");
		parameters.put("Url", "http://restcomm-demo.appspot.com/app/voice/restcomm3.xml");
		//		parameters.put("Url", "http://127.0.0.1:8888/restcomm/demo/hello-world.xml");
		parameters.put("VoiceMethod", "POST");

		Call outgoingCall = callFactory.create(parameters);

		assertNotNull(outgoingCall);

		assertTrue(sipCallA.waitForIncomingCall(5000));

		assertTrue(sipCallA.sendIncomingCallResponse(Response.RINGING, "Ringing", 0));
		assertTrue(sipCallA.sendIncomingCallResponse(Response.OK, "OK", 0));

		assertTrue(sipCallA.waitForDisconnect(60000));
	}

}

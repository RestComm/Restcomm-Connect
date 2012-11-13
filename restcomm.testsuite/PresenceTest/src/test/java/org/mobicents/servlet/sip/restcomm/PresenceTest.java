/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.sip.address.SipURI;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Client;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class PresenceTest extends AbstractTest {

	@ArquillianResource
	URL deploymentUrl;
	String endpoint;

	private static TwilioRestClient client;
	private static Client sipClient;
	private static Account account;

	private SipStack receiver;

	private SipCall sipCall;
	private SipPhone sipPhone;

	private final int TIMEOUT = 10000;	

	private static SipStackTool sipStackTool;

	@BeforeClass
	public static void beforeClass(){
		if (sipStackTool==null)
			sipStackTool = new SipStackTool("PresenceTest");
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

		if(sipClient==null)
			sipClient=createClient();
	}

	@After
	public void tearDown() throws TwilioRestException
	{
		if(sipCall != null)	sipCall.disposeNoBye();
		if(sipPhone != null) sipPhone.dispose();
		if(receiver != null) receiver.dispose();
	}

	private Client createClient() throws TwilioRestException{
		ClientFactory clientFactory = account.getClientFactory();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Login","rest");
		parameters.put("Password","comm");
		parameters.put("FriendlyName", "Restcomm User");
		parameters.put("VoiceUrl", endpoint+"/demo/hello-world.xml");
		parameters.put("VoiceMethod", "POST");
		parameters.put("VoiceFallbackUrl", endpoint+"/demo/hello-world.xml");
		parameters.put("VoiceFallbackMethod", "POST");
		parameters.put("StatusCallback", endpoint+"/demo/hello-world.xml");
		parameters.put("StatusCallbackMethod", "POST");
		parameters.put("VoiceCallerIdLookup", "false");

		//Create client
		return clientFactory.create(parameters);
	}

	@Test
	public void registerClient() throws ParseException, TwilioRestException{	
		//Register
		SipURI requestURI = receiver.getAddressFactory().createSipURI(null,"127.0.0.1:5070");
		assertTrue(sipPhone.register(requestURI, "rest", "comm", "sip:127.0.0.1:5080", TIMEOUT, TIMEOUT));
		assertTrue(sipPhone.unregister("sip:127.0.0.1:5080", TIMEOUT));
	}

	//Issue: http://code.google.com/p/restcomm/issues/detail?id=84
	@Test
	public void registerClientWithTransport() throws ParseException, TwilioRestException, LifecycleException{
		//Register
		SipURI requestURI = receiver.getAddressFactory().createSipURI(null,"127.0.0.1:5070");
		assertTrue(sipPhone.register(requestURI, "rest", "comm", "sip:127.0.0.1:5080;transport=udp", TIMEOUT, TIMEOUT));
		assertTrue(sipPhone.unregister("sip:127.0.0.1:5080;transport=udp", TIMEOUT));
	}


}

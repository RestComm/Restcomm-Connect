/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.sip.address.SipURI;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mobicents.api.annotations.GetDeployableContainer;
import org.jboss.arquillian.container.mss.extension.ContainerManagerTool;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Client;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
public class PresenceTest extends AbstractEndpointTest {

	private Logger logger = Logger.getLogger(PresenceTest.class);
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

	@BeforeClass
	public static void beforeClass(){
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
	}

	@After
	public void tearDown() throws Exception
	{

		if(sipCall != null)	sipCall.disposeNoBye();
		if(sipPhone != null) sipPhone.dispose();
		if(receiver != null) receiver.dispose();
		logger.info("About to un-deploy the application");
		deployer.undeploy(super.testArchive);
	}

	private void createClient() throws TwilioRestException{
		final TwilioRestClient twilioClient = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
				"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		final Account account = twilioClient.getAccount();
		final ClientFactory clientFactory = account.getClientFactory();
		final Map<String, String> parameters = new HashMap<String, String>();
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
		Client client = clientFactory.create(parameters);
	}
	
	@Test
	public void registerClient() throws ParseException, TwilioRestException{
		logger.info("About to deploy the application");
		deployer.deploy(testArchive);
		
		//Create Client
		createClient();
		
		//Register
		SipURI requestURI = receiver.getAddressFactory().createSipURI(null,"127.0.0.1:5070");
		assertTrue(sipPhone.register(requestURI, "rest", "comm", "sip:127.0.0.1:5080", TIMEOUT, TIMEOUT));
		
	}
	

}

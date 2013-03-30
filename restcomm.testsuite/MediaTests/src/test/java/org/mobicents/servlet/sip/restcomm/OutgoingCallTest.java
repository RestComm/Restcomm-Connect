package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
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
@Mediaserver(IVR=5,CONF=5,RELAY=5)
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
	public void testOutgoing() throws TwilioRestException, ParseException, InterruptedException, UnsupportedEncodingException, IOException{
		
		CallFactory callFactory = account.getCallFactory();
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("From", "8765");
		parameters.put("To", "4321");
		parameters.put("Url", endpoint+"/demo/hello-world.xml");
		parameters.put("VoiceMethod", "POST");

		Call outgoingCall = callFactory.create(parameters);

		assertNotNull(outgoingCall);

		assertTrue(sipCallA.waitForIncomingCall(5000));
		
		byte[] bodyByte = sipCallA.getLastReceivedRequest().getRawContent();
		String body = new String(bodyByte);
		ContentTypeHeader contentHeader = sipCallA.getHeaderFactory().createContentTypeHeader("application", "sdp");
		ArrayList<Header> additionalHeaders = new ArrayList<Header>();
		additionalHeaders.add(contentHeader);

		assertTrue(sipCallA.sendIncomingCallResponse(Response.TRYING, "TRYING", -1));
		assertTrue(sipCallA.sendIncomingCallResponse(Response.RINGING, "Ringing", -1));
		assertTrue(sipCallA.sendIncomingCallResponse(Response.OK, "Ok", -1, additionalHeaders, null, body));
		assertTrue(sipCallA.waitForAck(5000));

		assertTrue(sipCallA.waitForDisconnect(600000));
		assertTrue(sipCallA.respondToDisconnect());
	}

}

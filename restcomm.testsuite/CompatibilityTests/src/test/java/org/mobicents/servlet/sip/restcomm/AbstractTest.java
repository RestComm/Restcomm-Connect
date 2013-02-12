package org.mobicents.servlet.sip.restcomm;

import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;

import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.IncomingPhoneNumberFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

public class AbstractTest {
	private static final String restcommVersion = "1.0.0.CR1-SNAPSHOT";

	public static WebArchive createWebArchive(){
		WebArchive archive = ShrinkWrapMaven.resolver()
				.resolve("org.mobicents.servlet.sip:restcomm.core:war:"+restcommVersion)
				.withoutTransitivity().asSingle(WebArchive.class);
		return archive;
	}

	public static WebArchive createWebArchive(String restcommConfig){
		WebArchive archive = createWebArchive();

		archive.delete("/WEB-INF/conf/restcomm.xml");
		archive.addAsWebInfResource(restcommConfig, "conf/restcomm.xml");

		return archive;		
	}

	public static WebArchive createWebArchive(String restcommConfig, String restcommApp){
		WebArchive archive = createWebArchive(restcommConfig);

		archive.addAsWebResource(restcommApp, "demo/"+restcommApp);

		return archive;
	}

	public String getEndpoint(String deploymentUrl) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		return deploymentUrl;
	}

	@After
	public void tearDown() throws Exception{
		Thread.sleep(1000);
	}

	public IncomingPhoneNumber createPhoneNumber(String appURL, Account account) throws TwilioRestException{
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
		IncomingPhoneNumber phoneNumber = factory.create(parameters);
		phoneNumber.setRequestAccountSid(account.getSid());
		return phoneNumber;
	}
	
}

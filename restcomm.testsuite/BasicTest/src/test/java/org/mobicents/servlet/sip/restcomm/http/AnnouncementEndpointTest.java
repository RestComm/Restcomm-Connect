package org.mobicents.servlet.sip.restcomm.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.sip.restcomm.AbstractTest;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.AnnouncementFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Announcement;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@RunWith(Arquillian.class)
public class AnnouncementEndpointTest extends AbstractTest {
	private Logger logger = Logger.getLogger(AnnouncementEndpointTest.class);

	@ArquillianResource
	URL deploymentUrl;
	String endpoint;

	private TwilioRestClient client;
	private Account account;

	@Before
	public void setUp(){
		endpoint = super.getEndpoint(deploymentUrl.toString());
		if(client==null)
			client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
					"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		if(account==null)
			account = client.getAccount();
	}

	//TODO Needs TTS to be running
	@Test @Ignore 
	public void createAnnouncement() throws TwilioRestException{
		AnnouncementFactory announcementFactory = account.getAnnouncementFactory();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Gender", "man");
		parameters.put("Language", "en");
		parameters.put("Text", "Hello World! This is an announcement");
		Announcement announcement = announcementFactory.create(parameters);

		assertNotNull(announcement);
		assertNotNull(announcement.getSid());
		assertEquals("ACae6e420f425248d6a26948c17a9e2acf", announcement.getAccountSid());
		assertNotNull(announcement.getUri());

		logger.info("URI: "+announcement.getUri());
		logger.info("Gender: "+announcement.getGender());
		logger.info("Language: "+announcement.getLanguage());
		logger.info("Text: "+announcement.getText());		
	}

	//TODO Needs TTS to be running
	@Test @Ignore  
	public void createAnnouncementJustText() throws TwilioRestException{
		AnnouncementFactory announcementFactory = account.getAnnouncementFactory();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Text", "Hello World! This is an announcement");
		Announcement announcement = announcementFactory.create(parameters);

		assertNotNull(announcement);
		assertNotNull(announcement.getSid());
		assertEquals("ACae6e420f425248d6a26948c17a9e2acf", announcement.getAccountSid());
		assertNotNull(announcement.getUri());
		assertEquals("en", announcement.getLanguage());
		assertEquals("man", announcement.getGender());

		logger.info("URI: "+announcement.getUri());
		logger.info("Gender: "+announcement.getGender());
		logger.info("Language: "+announcement.getLanguage());
		logger.info("Text: "+announcement.getText());		
	}

}

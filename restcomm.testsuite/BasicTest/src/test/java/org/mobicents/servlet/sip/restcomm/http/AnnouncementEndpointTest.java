package org.mobicents.servlet.sip.restcomm.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.AnnouncementFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Announcement;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class AnnouncementEndpointTest extends AbstractEndpointTest {
	private Logger logger = Logger.getLogger(AnnouncementEndpointTest.class);
	
	public String endpoint = "http://127.0.0.1:8888/restcomm";
	
	public AnnouncementEndpointTest() {
		super();
	}

	@Test
	public void createAnnouncement() throws TwilioRestException{
		final TwilioRestClient twilioClient = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
				"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		final Account account = twilioClient.getAccount();
		final AnnouncementFactory announcementFactory = account.getAnnouncementFactory();
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Gender", "man");
		parameters.put("Language", "en");
		parameters.put("Text", "Hello World! This is an announcement");
		final Announcement announcement = announcementFactory.create(parameters);
		
		assertNotNull(announcement);
		assertNotNull(announcement.getSid());
		assertEquals("ACae6e420f425248d6a26948c17a9e2acf", announcement.getAccountSid());
		assertNotNull(announcement.getUri());
		
		logger.info("URI: "+announcement.getUri());
		logger.info("Gender: "+announcement.getGender());
		logger.info("Language: "+announcement.getLanguage());
		logger.info("Text: "+announcement.getText());		
	}

	@Test
	public void createAnnouncementJustText() throws TwilioRestException{
		final TwilioRestClient twilioClient = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
				"77f8c12cc7b8f8423e5c38b035249166", endpoint);
		final Account account = twilioClient.getAccount();
		final AnnouncementFactory announcementFactory = account.getAnnouncementFactory();
		final Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("Text", "Hello World! This is an announcement");
		final Announcement announcement = announcementFactory.create(parameters);
		
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

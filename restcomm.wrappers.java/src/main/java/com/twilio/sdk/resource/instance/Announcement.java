package com.twilio.sdk.resource.instance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.InstanceResource;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class Announcement extends InstanceResource {

	/** The Constant SID_PROPERTY. */
	private static final String SID_PROPERTY = "sid";
	
	public Announcement(TwilioRestClient client) {
		super(client);
	}
	
	public Announcement(TwilioRestClient client, String sid) {
		super(client);
        if (sid == null) { 
            throw new IllegalStateException("The Sid for an Sms can not be null");
        }
		this.setProperty(SID_PROPERTY, sid);
	}
	
	public Announcement(TwilioRestClient client, Map<String, Object> properties) {
		super(client, properties);
	}

	@Override
	protected String getResourceLocation() {
		return "/" + TwilioRestClient.DEFAULT_VERSION + "/Accounts/"
				+ this.getRequestAccountSid() + "/Announcements/" + this.getSid() + ".json";
	}

	public String getSid() {
		return this.getProperty(SID_PROPERTY);
	}
	
	public Date getDateCreated() {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z");
		try {
			return format.parse(this.getProperty("date_created"));
		} catch (ParseException e) {
			return null;
		}
	}
	
	public String getAccountSid() {
		return this.getProperty("account_sid");
	}
	
	public String getGender() {
		return this.getProperty("gender");
	}
	
	public String getLanguage() {
		return this.getProperty("language");
	}
	
	public String getText() {
		return this.getProperty("text");
	}
	
	public String getUri() {
		return this.getProperty("uri");
	}
}

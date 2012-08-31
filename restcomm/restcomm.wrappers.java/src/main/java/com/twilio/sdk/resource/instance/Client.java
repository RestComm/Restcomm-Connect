/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.twilio.sdk.resource.instance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;
import com.twilio.sdk.resource.InstanceResource;

/**
 * The Class Client.
 * 
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class Client extends InstanceResource {
	/** The Constant SID_PROPERTY. */
	private static final String SID_PROPERTY = "sid";

	public Client(final TwilioRestClient client) {
		super(client);
	}
	
	public Client(final TwilioRestClient client, final String sid) {
		super(client);
		if(sid == null) { 
            throw new IllegalArgumentException("The Sid for a Client can not be null");
        }
		this.setProperty(SID_PROPERTY, sid);
	}

	public Client(final TwilioRestClient client, final Map<String, Object> properties) {
		super(client, properties);
	}
	
	/**
	 * Deprovision this Client. This will remove it from your
	 * account.
	 * 
	 * @throws TwilioRestException
	 *             if there is an error in the request
	 * @return true, if successful
	 * 
	 */
	public boolean delete() throws TwilioRestException {
		TwilioRestResponse response = this.getClient().safeRequest(
				this.getResourceLocation(), "DELETE", null);
		
		return !response.isError();
	}
	
	/**
	 * Gets the account sid.
	 * 
	 * @return the account sid
	 */
	public String getAccountSid() {
		return this.getProperty("account_sid");
	}
	
	/**
	 * Gets the api version.
	 * 
	 * @return the api version
	 */
	public String getApiVersion() {
		return this.getProperty("api_version");
	}
	
	/**
	 * Gets the date created.
	 * 
	 * @return the date created
	 */
	public Date getDateCreated() {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z");
		try {
			return format.parse(this.getProperty("date_created"));
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Gets the date updated.
	 * 
	 * @return the date updated
	 */
	public Date getDateUpdated() {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z");
		try {
			return format.parse(this.getProperty("date_updated"));
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Gets the friendly name.
	 * 
	 * @return the friendly name
	 */
	public String getFriendlyName() {
		return this.getProperty("friendly_name");
	}
	
	public String getLogin() {
		return this.getProperty("login");
	}
	
	public String getPassword() {
		return this.getProperty("password");
	}
	
	@Override protected String getResourceLocation() {
		return "/" + TwilioRestClient.DEFAULT_VERSION + "/Accounts/"
				+ this.getRequestAccountSid() + "/Clients/"
				+ this.getSid() + ".json";
	}
	
	/**
	 * Gets the sid.
	 * 
	 * @return the sid
	 */
	public String getSid() {
		return this.getProperty(SID_PROPERTY);
	}
	
	public String getStatus() {
		return this.getProperty("status");
	}
	
	/**
	 * Gets the voice application sid.
	 * 
	 * @return the voice application sid
	 */
	public String getVoiceApplicationSid() {
		return this.getProperty("voice_application_sid");
	}

	/**
	 * Gets the voice fallback method.
	 * 
	 * @return the voice fallback method
	 */
	public String getVoiceFallbackMethod() {
		return this.getProperty("voice_fallback_method");
	}
	
	/**
	 * Gets the voice fallback url.
	 * 
	 * @return the voice fallback url
	 */
	public String getVoiceFallbackUrl() {
		return this.getProperty("voice_fallback_url");
	}

	/**
	 * Gets the voice method.
	 * 
	 * @return the voice method
	 */
	public String getVoiceMethod() {
		return this.getProperty("voice_method");
	}
	
	/**
	 * Gets the voice url.
	 * 
	 * @return the voice url
	 */
	public String getVoiceUrl() {
		return this.getProperty("voice_url");
	}
}

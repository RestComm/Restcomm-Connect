package com.twilio.sdk.resource.list;

import java.util.Map;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;
import com.twilio.sdk.resource.ListResource;
import com.twilio.sdk.resource.factory.AnnouncementFactory;
import com.twilio.sdk.resource.instance.Announcement;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class AnnouncementList extends ListResource<Announcement> implements AnnouncementFactory {

	public AnnouncementList(TwilioRestClient client) {
		super(client);
	}
	
	public AnnouncementList(TwilioRestClient client, Map<String, String> filters) {
		super(client, filters);
	}
	
	@Override
	protected Announcement makeNew(TwilioRestClient client,
			Map<String, Object> params) {
		return new Announcement(client, params);
	}

	@Override
	protected String getListKey() {
		return "announcements";
	}

	@Override
	protected String getResourceLocation() {
		return "/" + TwilioRestClient.DEFAULT_VERSION + "/Accounts/"
				+ this.getRequestAccountSid() + "/Announcements.json";
	}

	@Override
	public Announcement create(Map<String, String> params)
			throws TwilioRestException {
		TwilioRestResponse response = this.getClient().safeRequest(
				this.getResourceLocation(), "POST", params);
		return makeNew(this.getClient(), response.toMap());
	}

}

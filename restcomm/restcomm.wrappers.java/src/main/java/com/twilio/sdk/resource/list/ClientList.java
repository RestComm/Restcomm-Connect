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
package com.twilio.sdk.resource.list;

import java.util.Map;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;
import com.twilio.sdk.resource.ListResource;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.instance.Client;

/**
 * The Class ClientList.
 * 
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class ClientList extends ListResource<Client> implements ClientFactory {
	public ClientList(final TwilioRestClient client) {
		super(client);
	}
	
	public ClientList(final TwilioRestClient client, final Map<String, String> filters) {
		super(client, filters);
	}

	@Override public Client create(final Map<String, String> params) throws TwilioRestException {
		TwilioRestResponse response = this.getClient().safeRequest(
				this.getResourceLocation(), "POST", params);
		return makeNew(this.getClient(), response.toMap());
	}

	@Override protected String getListKey() {
		return "clients";
	}

	@Override protected String getResourceLocation() {
		return "/" + TwilioRestClient.DEFAULT_VERSION + "/Accounts/" 
				+ this.getRequestAccountSid() + "/Clients.json";
	}
	
	@Override protected Client makeNew(final TwilioRestClient client, final Map<String, Object> params) {
		return new Client(client, params);
	}
}

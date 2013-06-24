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
package org.mobicents.servlet.sip.restcomm.http;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.sip.restcomm.AbstractTest;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.ClientFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Client;
import com.twilio.sdk.resource.list.ClientList;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@RunWith(Arquillian.class)
public class ClientsEndpointTest extends AbstractTest {

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

	@Test
	public void checkClientTest() {
		ClientList clients = account.getClients();
		assertTrue(clients.getTotal() == 0);
	}

	@Test
	public void createClientTest() throws TwilioRestException{
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
		Client client = clientFactory.create(parameters);

		assertTrue(client.getFriendlyName().equalsIgnoreCase("Restcomm User"));
		assertTrue(client.getLogin().equalsIgnoreCase("rest"));
		assertTrue(client.getPassword().equalsIgnoreCase("comm"));

	}
}

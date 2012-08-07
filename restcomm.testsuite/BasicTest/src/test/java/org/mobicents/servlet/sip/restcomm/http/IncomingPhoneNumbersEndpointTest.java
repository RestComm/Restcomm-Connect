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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Test;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.IncomingPhoneNumberFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.IncomingPhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
// @RunWith(Arquillian.class)
public class IncomingPhoneNumbersEndpointTest extends AbstractEndpointTest {
  public IncomingPhoneNumbersEndpointTest() {
    super();
  }
  
  @Test public void createIncomingPhoneNumber() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166", "http://192.168.1.106:8080/restcomm");
	// Create incoming phone number.
    final Account account = client.getAccount();
    final IncomingPhoneNumberFactory factory = account.getIncomingPhoneNumberFactory();
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("PhoneNumber", "+12223334444");
    parameters.put("VoiceUrl", "http://192.168.1.106:8080/restcomm/demo/hello-world.xml");
    parameters.put("VoiceMethod", "POST");
    parameters.put("VoiceFallbackUrl", "http://192.168.1.106:8080/restcomm/demo/hello-world.xml");
    parameters.put("VoiceFallbackMethod", "POST");
    parameters.put("StatusCallback", "http://192.168.1.106:8080/restcomm/demo/hello-world.xml");
    parameters.put("StatusCallbackMethod", "POST");
    parameters.put("VoiceCallerIdLookup", "false");
    // parameters.put("VoiceApplicationSid", "");
    parameters.put("SmsUrl", "http://192.168.1.106:8080/restcomm/demo/hello-world.xml");
    parameters.put("SmsMethod", "POST");
    parameters.put("SmsFallbackUrl", "http://192.168.1.106:8080/restcomm/demo/hello-world.xml");
    parameters.put("SmsFallbackMethod", "POST");
    // parameters.put("SmsApplicationSid", "");
    final IncomingPhoneNumber incomingPhoneNumber = factory.create(parameters);
    assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
    assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+12223334444"));
    assertTrue(incomingPhoneNumber.getVoiceUrl().equals("http://192.168.1.106:8080/restcomm/demo/hello-world.xml"));
  }
  
  @Test public void deleteIncomingPhoneNumber() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166", "http://192.168.1.106:8080/restcomm");
    // Create incoming phone number.
    final Account account = client.getAccount();
    final IncomingPhoneNumberFactory factory = account.getIncomingPhoneNumberFactory();
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("PhoneNumber", "+12223334444");
    final IncomingPhoneNumber incomingPhoneNumber = factory.create(parameters);
    assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
    assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+12223334444"));
    // Delete the new incoming phone number.
    incomingPhoneNumber.setRequestAccountSid("ACae6e420f425248d6a26948c17a9e2acf");
    incomingPhoneNumber.delete();
  }
  
  @Test public void getIncomingPhoneNumber() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166", "http://192.168.1.106:8080/restcomm");
    // Get incoming phone number.
    final Account account = client.getAccount();
    final IncomingPhoneNumber ipn = account.getIncomingPhoneNumber("PNc96bd38c7fd7413c99cab286bb73df5b");
    assertTrue("ACae6e420f425248d6a26948c17a9e2acf".equals(ipn.getAccountSid()));
    assertTrue("(305) 587-2294".equals(ipn.getFriendlyName()));
    assertTrue("+13055872294".equals(ipn.getPhoneNumber()));
    assertTrue("http://192.168.1.106:8080/restcomm/demo/hello-world.xml".equals(ipn.getVoiceUrl()));
    assertTrue("POST".equals(ipn.getVoiceMethod()));
    assertTrue("2012-04-24".equals(ipn.getApiVersion()));
  }
  
  @Test public void updateIncomingPhoneNumber() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166", "http://192.168.1.106:8080/restcomm");
    // Create incoming phone number.
    final Account account = client.getAccount();
    final IncomingPhoneNumberFactory factory = account.getIncomingPhoneNumberFactory();
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("PhoneNumber", "+12223334444");
    final IncomingPhoneNumber incomingPhoneNumber = factory.create(parameters);
    assertTrue(incomingPhoneNumber.getAccountSid().equals("ACae6e420f425248d6a26948c17a9e2acf"));
    assertTrue(incomingPhoneNumber.getPhoneNumber().equals("+12223334444"));
    // Update incoming phone number.
    
  }
}

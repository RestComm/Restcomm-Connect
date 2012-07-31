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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.instance.Account;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class AccountsEndpointTest extends AbstractEndpointTest {
  public AccountsEndpointTest() {
    super();
  }
  
  @Test public void createSubAccount() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166");
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("EmailAddress", "user@company.com");
    parameters.put("Password", "1234");
    final Account account = client.getAccounts().create(parameters);
    System.out.println("Created a new sub-account with SID " + account.getSid());
    assertTrue("user@company.com".equals(account.getFriendlyName()));
    assertTrue("81dc9bdb52d04dc20036dbd8313ed055".equals(account.getAuthToken()));
  }

  @Test public void getAccount() {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166");
    final Account account = client.getAccount();
    assertTrue("ACae6e420f425248d6a26948c17a9e2acf".equals(account.getSid()));
    assertTrue("Default Administrator Account".equals(account.getFriendlyName()));
    assertTrue("active".equals(account.getStatus()));
    assertTrue("77f8c12cc7b8f8423e5c38b035249166".equals(account.getAuthToken()));
  }
  
  @Test public void updateAccount() throws TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166");
    final Account account = client.getAccount();
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("FriendlyName", "Administrator Account");
    parameters.put("Password", "1234");
    parameters.put("Status", "closed");
    account.update(parameters);
    assertTrue("Administrator Account".equals(account.getFriendlyName()));
    assertTrue("81dc9bdb52d04dc20036dbd8313ed055".equals(account.getAuthToken()));
    assertTrue("closed".equals(account.getStatus()));
  }
}

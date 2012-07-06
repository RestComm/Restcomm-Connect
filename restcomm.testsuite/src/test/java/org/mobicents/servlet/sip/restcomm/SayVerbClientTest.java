package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Call;

import org.junit.Test;

public final class SayVerbClientTest {
  public SayVerbClientTest() {
    super();
  }
  
  @Test public void test() throws InterruptedException, TwilioRestException {
    final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166");
    final Account account = client.getAccount();
    final CallFactory factory = account.getCallFactory();
    final Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("To", "+13055872294");
    parameters.put("From", "(305) 587-2294");
    parameters.put("Url", "http://192.168.1.106:8080/restcomm/tests/dial-say-verb-test");
    final Call call = factory.create(parameters);
    final String status = call.getStatus();
    assertTrue("in-progress".equals(status));
  }
}

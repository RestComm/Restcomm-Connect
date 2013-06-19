/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.sip.restcomm.sms.EsendexService;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
public class EssendexSMSTest {

	private String account = "EXxxxxxx";
	private String user = "gvagenas@xxxxxxx";
	private String password = "NDXxxxx";
	private String to = "003xxxxxxxxxxxx";
	
	@Before
	public void prepareConfiguration(){
	}
	
	@Test
	public void sendSMSTest(){
		EsendexService service = new EsendexService(user, password, account);
		service.sendMessage(to, "Test SMS message");
		HttpResponse response = service.getResponse();
		assertNotNull(response);
		assertTrue(response.getStatusLine().getStatusCode()==200);
	}
	
	
}

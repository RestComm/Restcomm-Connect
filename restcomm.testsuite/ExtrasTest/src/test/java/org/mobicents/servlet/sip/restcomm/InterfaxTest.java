/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.sip.restcomm.fax.InterfaxHttpClient;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
public class InterfaxTest {

	private String user = "gvagenas";
	private String password = "xxxx";
	private String to = "003xxx";
	private URI content ;

	@Before
	public void prepareContent(){
		File file = new File("faxPdf.pdf");
		content = file.toURI();
	}

	@Test
	public void sendFax(){
		InterfaxHttpClient client = new InterfaxHttpClient(user, password, to, content);
		HttpResponse response = client.sendfax();
		assertNotNull(response);
		assertTrue(response.getStatusLine().getStatusCode()==201);
	}

}

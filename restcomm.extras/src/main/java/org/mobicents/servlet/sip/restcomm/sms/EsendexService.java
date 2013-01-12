/**
 * 
 */
package org.mobicents.servlet.sip.restcomm.sms;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.CertificateException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
public class EsendexService {
	private static final Logger logger = Logger.getLogger(EsendexService.class);

	private String user;
	private String password;
	private String account;

	private HttpContext localContext;
	private DefaultHttpClient httpClient;

	private HttpResponse response;


	public EsendexService(String user, String password, String account) {
		this.user = user;
		this.password = password;
		this.account = account;

		localContext = new BasicHttpContext();
		httpClient = new DefaultHttpClient();
		TrustStrategy easyStrategy = new TrustStrategy() {
			@Override
			public boolean isTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
							throws CertificateException {
				return true;
			}
		};

		SSLSocketFactory socketFactory = null;
		try {
			socketFactory = new SSLSocketFactory(easyStrategy);
		}  catch (Exception exception){
			logger.error(exception);
		}
		Scheme sch = new Scheme("https", 443, socketFactory);
		httpClient.getConnectionManager().getSchemeRegistry().register(sch);

	}


	public void sendMessage(String to, String body){
		try {
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.user, this.password);
			HttpPost post = new HttpPost("https://api.esendex.com/v1.0/messagedispatcher");

			StringEntity stringEntity = new StringEntity(createXML(to, body, account));
			stringEntity.setContentType("application/xml");

			post.setEntity(stringEntity);
			post.addHeader(new BasicScheme().authenticate(creds, post, localContext));

			logger.info("executing request " + post.getRequestLine());
			response = httpClient.execute(post, localContext);
			HttpEntity resEntity = response.getEntity();

			logger.info("Response Status: "+response.getStatusLine());
			if (resEntity != null) {
				logger.info("Response content length: " + resEntity.getContentLength());
			}
			EntityUtils.consume(resEntity);

		} catch (Exception exception){
			logger.error(exception);
		}
		finally {
			try { httpClient.getConnectionManager().shutdown(); } catch (Exception ignore) {}
		}
	}

	private static String createXML(String toString, String bodyString, String account) throws ParserConfigurationException, TransformerException, IOException{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("messages");
		doc.appendChild(rootElement);

		// set attribute to staff element
		Element accountRef = doc.createElement("accountreference");
		accountRef.appendChild(doc.createTextNode(account));
		rootElement.appendChild(accountRef);

		Element message = doc.createElement("message");
		rootElement.appendChild(message);

		Element to = doc.createElement("to");
		to.appendChild(doc.createTextNode(toString));
		message.appendChild(to);

		Element body = doc.createElement("body");
		body.appendChild(doc.createTextNode(bodyString));
		message.appendChild(body);

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);

		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);

		transformer.transform(source, result);

		return writer.toString();
	}


	public HttpResponse getResponse() {
		return response;
	}


	public void setResponse(HttpResponse response) {
		this.response = response;
	}

}

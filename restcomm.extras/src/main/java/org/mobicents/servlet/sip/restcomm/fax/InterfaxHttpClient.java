/**
 * 
 */
package org.mobicents.servlet.sip.restcomm.fax;

import java.io.File;
import java.net.URI;
import java.net.URLConnection;
import java.security.cert.CertificateException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
public class InterfaxHttpClient {
	private static final Logger logger = Logger.getLogger(InterfaxHttpClient.class);

	private String user;
	private String password;
	private String to;
	private URI content;

	private String url = "https://rest.interfax.net/outbound/faxes?faxNumber=";

	private HttpContext localContext;
	private DefaultHttpClient httpclient;
	private HttpResponse response;

	public InterfaxHttpClient(String user, String password, String to, URI content) {

		this.user = user;
		this.password = password;
		this.to = to;
		this.content = content;

		localContext = new BasicHttpContext();
		httpclient = new DefaultHttpClient();
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
		} catch (Exception exception) {
			logger.error(exception);
		}
		Scheme sch = new Scheme("https", 443, socketFactory);

		httpclient.getConnectionManager().getSchemeRegistry().register(sch);

	}


	public HttpResponse sendfax() {
		try{
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.user, this.password);

			HttpPost post = new HttpPost(url+to);

			File file = new File(content);
			FileEntity fileEntity = new FileEntity(file, URLConnection.guessContentTypeFromName(file.getName()));

			post.setEntity(fileEntity);
			post.addHeader(new BasicScheme().authenticate(creds, post, localContext));

			logger.info("executing request " + post.getRequestLine());
			response = httpclient.execute(post, localContext);
			HttpEntity resEntity = response.getEntity();

			logger.info("Response status: "+response.getStatusLine());
			if (resEntity != null) {
				logger.info("Response content length: " + resEntity.getContentLength());
			}

			Header[] headers = response.getHeaders(HttpHeaders.LOCATION);
			String url = null;
			for (int i = 0; i < headers.length; i++) {
				url = headers[i].getValue();
			}
			logger.info("Job location: "+url);
			EntityUtils.consume(resEntity);



		} catch (Exception exception) {
			logger.error(exception);
		} finally {
			try { httpclient.getConnectionManager().shutdown(); } catch (Exception ignore) {}
		}
		return response;
	}


}

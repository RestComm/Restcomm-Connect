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
package org.mobicents.servlet.restcomm.fax;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import java.io.File;
import java.net.URI;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.configuration.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
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

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class InterfaxService extends UntypedActor {
  private static final String url = "https://rest.interfax.net/outbound/faxes?faxNumber=";
  
  private final TrustStrategy strategy;
  private final String user;
  private final String password;
  
  public InterfaxService(final Configuration configuration) {
    super();
    user = configuration.getString("user");
    password = configuration.getString("password");
    strategy = new TrustStrategy() {
      @Override public boolean isTrusted(final X509Certificate[] chain,
          final String authType) throws CertificateException {
	    return true;
	  }
    };
  }

  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(FaxRequest.class.equals(klass)) {
      try {
        sender.tell(new FaxResponse(send(message)), self);
      } catch(final Exception exception) {
        sender.tell(new FaxResponse(exception), self);
      }
    }
  }
  
  private URI send(final Object message) throws Exception {
    final FaxRequest request = (FaxRequest)message;
    final String to = request.to();
    final File file = request.file();
    // Prepare the request.
    final DefaultHttpClient client = new DefaultHttpClient();
    final HttpContext context = new BasicHttpContext();
    final SSLSocketFactory sockets = new SSLSocketFactory(strategy);
    final Scheme scheme = new Scheme("https", 443, sockets);
    client.getConnectionManager().getSchemeRegistry().register(scheme);
    final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
    final HttpPost post = new HttpPost(url + to);
    final String mime = URLConnection.guessContentTypeFromName(file.getName());
    final FileEntity entity = new FileEntity(file, mime);
    post.addHeader(new BasicScheme().authenticate(credentials, post, context));
    post.setEntity(entity);
    // Handle the response.
    final HttpResponse response = client.execute(post, context);
    final StatusLine status = response.getStatusLine();
    final int code = status.getStatusCode();
    if(HttpStatus.SC_CREATED == code) {
      EntityUtils.consume(response.getEntity());
      final Header[] headers = response.getHeaders(HttpHeaders.LOCATION);
      final Header location = headers[0];
      final String resource = location.getValue();
      return URI.create(resource);
    } else {
      final StringBuilder buffer = new StringBuilder();
      buffer.append(code).append(" ").append(status.getReasonPhrase());
      throw new FaxServiceException(buffer.toString());
    }
  }
}

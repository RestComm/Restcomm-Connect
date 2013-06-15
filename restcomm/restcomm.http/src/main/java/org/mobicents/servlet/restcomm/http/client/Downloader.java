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
package org.mobicents.servlet.restcomm.http.client;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Downloader extends UntypedActor {
  public Downloader() {
    super();
  }
  
  public HttpResponseDescriptor fetch(final HttpRequestDescriptor descriptor)
      throws ClientProtocolException, IllegalArgumentException, UnsupportedEncodingException,
      IOException, URISyntaxException {
    int code = -1;
    HttpRequest request = null;
    HttpResponse response = null;
    HttpRequestDescriptor temp = descriptor;
    do {
      final DefaultHttpClient client = new DefaultHttpClient();
      client.getParams().setParameter(ClientPNames.COOKIE_POLICY,
          CookiePolicy.BROWSER_COMPATIBILITY);
      request = request(temp);
      response = client.execute((HttpUriRequest)request);
      code = response.getStatusLine().getStatusCode();
      if(isRedirect(code)) {
        final Header header = response.getFirstHeader(HttpHeaders.LOCATION);
        if(header != null) {
          final String location = header.getValue();
          final URI uri = URI.create(location);
          temp = new HttpRequestDescriptor(uri, temp.getMethod(),
              temp.getParameters());
          continue;
        } else {
          break;
        }
      }
    } while(isRedirect(code));
    return response(request, response);
  }
  
  private boolean isRedirect(final int code) {
    return HttpStatus.SC_MOVED_PERMANENTLY == code ||
        HttpStatus.SC_MOVED_TEMPORARILY == code ||
        HttpStatus.SC_SEE_OTHER == code ||
        HttpStatus.SC_TEMPORARY_REDIRECT == code;
  }
  
  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(HttpRequestDescriptor.class.equals(klass)) {
      final HttpRequestDescriptor request = (HttpRequestDescriptor)message;
      DownloaderResponse response = null;
      try {
        response = new DownloaderResponse(fetch(request));
      } catch(final Exception exception) {
        response = new DownloaderResponse(exception);
      }
      sender.tell(response, self);
    }
  }
  
  public HttpUriRequest request(final HttpRequestDescriptor descriptor)
      throws IllegalArgumentException, URISyntaxException, UnsupportedEncodingException {
    final URI uri = descriptor.getUri();
    final String method = descriptor.getMethod();
	if("GET".equalsIgnoreCase(method)) {
	  final String query = descriptor.getParametersAsString();
	  URI result = null;
	  if(query != null && !query.isEmpty()) {
	    result = URIUtils.createURI(uri.getScheme(), uri.getHost(),
	  	    uri.getPort(), uri.getPath(), query, null);
	  } else {
	    result = uri;
	  }
	  return new HttpGet(result);
	} else if("POST".equalsIgnoreCase(method)) {
	  final List<NameValuePair> parameters = descriptor.getParameters();
	  final HttpPost post = new HttpPost(uri);
	  post.setEntity(new UrlEncodedFormEntity(parameters));
	  return post;
	} else {
	  throw new IllegalArgumentException(method + " is not a supported downloader method."); 
	}
  }
  
  private HttpResponseDescriptor response(final HttpRequest request, final HttpResponse response)
      throws IOException {
    final HttpResponseDescriptor.Builder builder = HttpResponseDescriptor.builder();
    final URI uri = URI.create(request.getRequestLine().getUri());
    builder.setURI(uri);
    builder.setStatusCode(response.getStatusLine().getStatusCode());
    builder.setStatusDescription(response.getStatusLine().getReasonPhrase());
    builder.setHeaders(response.getAllHeaders());
    final HttpEntity entity = response.getEntity();
    if(entity != null) {
      final Header contentEncoding = entity.getContentEncoding();
      if(contentEncoding != null) {
        builder.setContentEncoding(contentEncoding.getValue());
      }
      final Header contentType = entity.getContentType();
      if(contentType != null) {
        builder.setContentType(contentType.getValue());
      }
      builder.setContent(entity.getContent());
      builder.setContentLength(entity.getContentLength());
      builder.setIsChunked(entity.isChunked());
    }
    return builder.build();
  }
}

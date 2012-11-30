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
package org.mobicents.servlet.sip.restcomm.interpreter.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class HttpRequestExecutor {
  public HttpRequestExecutor() {
    super();
  }
  
  public HttpResponseDescriptor execute(final HttpRequestDescriptor request) throws ClientProtocolException,
      IllegalArgumentException, UnsupportedEncodingException, IOException, URISyntaxException {
    int statusCode = -1;
    HttpResponse response = null;
    HttpRequestDescriptor descriptor = request;
    do {
      response = new DefaultHttpClient().execute(descriptor.getHttpRequest());
      statusCode = response.getStatusLine().getStatusCode();
      if(isRedirect(statusCode)) {
        final Header locationHeader = response.getFirstHeader("Location");
        if(locationHeader != null) {
          final String redirectLocation = locationHeader.getValue();
          final URI uri = URI.create(redirectLocation);
          descriptor = new HttpRequestDescriptor(uri, descriptor.getMethod(), descriptor.getParameters());
          continue;
        } else {
          break;
        }
      }
    } while(isRedirect(statusCode));
    return response(descriptor, response);
  }
  
  private boolean isRedirect(final int code) {
    return HttpStatus.SC_MOVED_PERMANENTLY == code || HttpStatus.SC_MOVED_TEMPORARILY == code ||
	    HttpStatus.SC_SEE_OTHER == code || HttpStatus.SC_TEMPORARY_REDIRECT == code;
  }
  
  private HttpResponseDescriptor response(final HttpRequestDescriptor descriptor, final HttpResponse response)
      throws IllegalStateException, IOException {
    final HttpResponseDescriptor.Builder builder = HttpResponseDescriptor.builder();
    builder.setRequestDescriptor(descriptor);
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
    }
    return builder.build();
  }
}

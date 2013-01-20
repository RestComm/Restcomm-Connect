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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class HttpRequestDescriptor {
  private final URI uri;
  private final String method;
  private final List<NameValuePair> parameters;
  
  public HttpRequestDescriptor(final URI uri, final String method,
      final List<NameValuePair> parameters) throws UnsupportedEncodingException,
      URISyntaxException {
    super();
    this.uri = URIUtils.createURI(uri.getScheme(), uri.getHost(), uri.getPort(),
        uri.getPath(), null, null);
    this.method = method;
    final String query = uri.getQuery();
    if(query != null) {
      parameters.addAll(URLEncodedUtils.parse(uri, "UTF-8"));
    }
    this.parameters = parameters;
  }
  
  public HttpUriRequest getHttpRequest() throws IllegalArgumentException, URISyntaxException, UnsupportedEncodingException {
    if("GET".equalsIgnoreCase(method)) {
      final String query = URLEncodedUtils.format(parameters, "UTF-8");
      final URI uri = URIUtils.createURI(this.uri.getScheme(), this.uri.getHost(), this.uri.getPort(),
          this.uri.getPath(), query, null);
      return new HttpGet(uri);
    } else if("POST".equalsIgnoreCase(method)) {
      final HttpPost post = new HttpPost(uri);
      post.setEntity(new UrlEncodedFormEntity(parameters));
      return post;
    } else {
      throw new IllegalArgumentException(method + " is not a supported HTTP method."); 
    }
  }
  
  public String getMethod() {
    return method;
  }
  
  public List<NameValuePair> getParameters() {
    return parameters;
  }
  
  public URI getUri() {
    return uri;
  }
}

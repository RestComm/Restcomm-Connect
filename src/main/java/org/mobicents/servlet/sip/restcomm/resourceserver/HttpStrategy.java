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
package org.mobicents.servlet.sip.restcomm.resourceserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import static org.mobicents.servlet.sip.restcomm.http.RequestMethod.*;

public final class HttpStrategy implements SchemeStrategy {
  public static final String SCHEME = "http";
  
  public HttpStrategy() {
    super();
  }
  
  public InputStream getInputStream(final URI uri, final Map<String, Object> attributes,
    final byte[] message, final String method) throws ResourceFetchException {
	HttpUriRequest request = null;
	if(method.equals(GET)) {
	  URI requestUri = null;
	  try {
	    requestUri = addAttributes(uri, attributes);
	  } catch(final URISyntaxException exception) {
	    throw new ResourceFetchException(exception);
	  }
	  request = new HttpGet(requestUri);
	} else if(method.equals(POST)) {
	  final HttpPost post = new HttpPost(uri);
	  if(message != null) {
	    post.setEntity(new ByteArrayEntity(message));
	  }
	  request = post;
	} else {
	  throw new ResourceFetchException(method + " is not a supported method.");
	}
	HttpResponse response = null;
	try {
	  final HttpClient client = new DefaultHttpClient();
      response = client.execute(request);
      final StatusLine status = response.getStatusLine();
      if(status.getStatusCode() == HttpStatus.SC_OK) {
        return response.getEntity().getContent();
      } else {
    	throw new ResourceFetchException(status.getStatusCode() + " " + status.getReasonPhrase());  
      }      
	} catch(final ClientProtocolException exception) {
      throw new ResourceFetchException(exception);
	} catch(final IOException exception) {
      throw new ResourceFetchException(exception);
	}
  }
  
  private URI addAttributes(final URI uri, final Map<String, Object> attributes)
    throws URISyntaxException {
    final List<NameValuePair> pairs = new ArrayList<NameValuePair>();
    final Set<String> keys = attributes.keySet();
    for(final String key : keys) {
      final Object value = attributes.get(key);
      pairs.add(new BasicNameValuePair(key, value.toString()));
    }
    final String host = uri.getHost();
    final int port = uri.getPort();
    final String path = uri.getPath();
    final String query = URLEncodedUtils.format(pairs, "UTF-8");
    final String fragment = uri.getFragment();
    return URIUtils.createURI(getScheme(), host, port, path, query, fragment);
  }

  public String getScheme() {
    return SCHEME;
  }
}

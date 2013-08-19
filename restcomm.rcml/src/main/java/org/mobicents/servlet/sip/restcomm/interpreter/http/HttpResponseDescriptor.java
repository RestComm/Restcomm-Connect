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
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.mobicents.servlet.sip.restcomm.util.HttpUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class HttpResponseDescriptor {
  private final HttpRequestDescriptor requestDescriptor;
  private final int statusCode;
  private final String statusDescription;
  private final HttpClient client;
  private final HttpEntity entity;
  private final long contentLength;
  private final String contentEncoding;
  private final String contentType;
  private final boolean isChunked;
  private final Header[] headers;
  
  private HttpResponseDescriptor(final HttpRequestDescriptor requestDescriptor, final int statusCode,
      final String statusDescription, final HttpClient client, final HttpEntity entity,
      final long contentLength, final String contentEncoding, final String contentType,
      final boolean isChunked, final Header[] headers) {
    super();
    this.requestDescriptor = requestDescriptor;
    this.statusCode = statusCode;
    this.statusDescription = statusDescription;
    this.client = client;
    this.entity = entity;
    this.contentLength = contentLength;
    this.contentEncoding = contentEncoding;
    this.contentType = contentType;
    this.isChunked = isChunked;
    this.headers = headers;
  }
  
  public HttpRequestDescriptor getRequestDescriptor() {
	return requestDescriptor;
  }

  public int getStatusCode() {
	return statusCode;
  }

  public String getStatusDescription() {
	return statusDescription;
  }

  public InputStream getContent() throws IllegalStateException, IOException {
	return entity.getContent();
  }
  
  public long getContentLength() {
    return contentLength;
  }

  public String getContentEncoding() {
	return contentEncoding;
  }

  public String getContentType() {
	return contentType;
  }
  
  public HttpClient getClient() {
    return client;
  }
  
  public HttpEntity getEntity() {
    return entity;
  }
  
  public boolean isChunked() {
    return isChunked;
  }

  public Header[] getHeaders() {
	return headers;
  }
  
  public String getHeadersAsString() {
    return HttpUtils.toString(headers);
  }

  public static Builder builder() {
    return new Builder();
  }
  
  public static final class Builder {
    private HttpRequestDescriptor requestDescriptor;
    private int statusCode;
    private String statusDescription;
    private HttpClient client;
    private HttpEntity entity;
    private long contentLength;
    private String contentEncoding;
    private String contentType;
    private boolean isChunked;
    private Header[] headers;
    
    private Builder() {
      super();
    }
    
    public HttpResponseDescriptor build() {
      return new HttpResponseDescriptor(requestDescriptor, statusCode, statusDescription, client,
          entity, contentLength, contentEncoding, contentType, isChunked, headers);
    }
    
    public void setStatusCode(final int statusCode) {
      this.statusCode = statusCode;
    }
    
    public void setStatusDescription(final String statusDescription) {
      this.statusDescription = statusDescription;
    }
    
    public void setClient(final HttpClient client) {
      this.client = client;
    }
    
    public void setEntity(final HttpEntity entity) {
      this.entity = entity;
    }
    
    public void setContentLength(final long contentLength) {
      this.contentLength = contentLength;
    }
    
    public void setContentEncoding(final String contentEncoding) {
      this.contentEncoding = contentEncoding;
    }
    
    public void setContentType(final String contentType) {
      this.contentType = contentType;
    }
    
    public void setIsChuncked(final boolean isChunked) {
      this.isChunked = isChunked;
    }
    
    public void setHeaders(final Header[] headers) {
      this.headers = headers;
    }
    
    public void setRequestDescriptor(final HttpRequestDescriptor requestDescriptor) {
      this.requestDescriptor = requestDescriptor;
    }
  }
}

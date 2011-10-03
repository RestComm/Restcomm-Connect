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
package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

public final class Application {
  private String endpoint;
  private String name;
  private String requestMethod;
  private URI uri;
  
  public Application() {
    super();
  }
  
  public String getEndPoint() {
    return endpoint;
  }
  
  public String getName() {
    return name;
  }
  
  public String getRequestMethod() {
    return requestMethod;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public void setEndPoint(final String endpoint) {
    this.endpoint = endpoint;
  }
  
  public void setName(final String name) {
    this.name = name;
  }
  
  public void setRequestMethod(final String requestMethod) {
    this.requestMethod = requestMethod;
  }
  
  public void setUri(final URI uri) {
    this.uri = uri;
  }
  
  @Override public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("Name: ").append(name).append("\n");
    buffer.append("Request Method: ").append(requestMethod).append("\n");
    buffer.append("SIP Endpoint: ").append(endpoint).append("\n");
    buffer.append("URI: ").append(uri);
    return buffer.toString();
  }
}

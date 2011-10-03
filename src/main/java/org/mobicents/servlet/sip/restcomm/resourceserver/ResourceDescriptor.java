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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class ResourceDescriptor {
  private Map<String, Object> attributes;
  private byte[] message;
  private String method;
  private final URI uri;
  
  public ResourceDescriptor(final URI uri) {
    super();
    this.uri = uri;
    this.attributes = new HashMap<String, Object>();
  }
  
  public Map<String, Object> getAttributes() {
    return attributes;
  }
  
  public byte[] getMessage() {
    return message;
  }
  
  public String getMethod() {
    return method;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = attributes;
  }
  
  public void setMessage(final byte[] message) {
    this.message = message;
  }
  
  public void setMethod(final String method) {
    this.method = method;
  }
}

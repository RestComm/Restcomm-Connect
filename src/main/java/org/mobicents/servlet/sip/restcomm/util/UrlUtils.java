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
package org.mobicents.servlet.sip.restcomm.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

public final class UrlUtils {
  private UrlUtils() {
    super();
  }
  
  public static String toQueryString(final Map<String, Object> map) {
    // Stringify the map entries.
    final StringBuilder buffer = new StringBuilder();
    final Set<String> keys = map.keySet();
    for(final String key : keys) {
      final Object value = map.get(key);
      try {
    	final String encodedValue = URLEncoder.encode(value.toString(), "UTF-8");
        buffer.append(key).append("=").append(encodedValue).append("&");
      } catch(final UnsupportedEncodingException ignored) {
        // Will never happen.
  	  }
    }
    String message = buffer.toString();
    // Remove the & delimiter at the end of the string.
    message = message.substring(0, message.length() - 1);
    return message;
  }
}

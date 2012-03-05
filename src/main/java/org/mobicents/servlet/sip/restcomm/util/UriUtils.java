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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class UriUtils {
  private UriUtils() {
    super();
  }
  
  public static Map<String, String> parseEntity(final HttpEntity entity)
      throws IllegalStateException, IOException {
	final int length = (int)entity.getContentLength();
	final byte[] data = new byte[length];
	entity.getContent().read(data, 0, length);
    final String input = new String(data);
    final String[] tokens = input.split("&");
    final Map<String, String> map = new HashMap<String, String>();
    for(final String token : tokens) {
      final String[] parts = token.split("=");
      if(parts.length == 1) {
        map.put(parts[0], null);
      } else if(parts.length == 2) {
        map.put(parts[0], URLDecoder.decode(parts[1], "UTF-8"));
      }
    }
    return map;
  }
}

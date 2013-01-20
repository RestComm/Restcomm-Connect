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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class HttpUtils {
  private HttpUtils() {
    super();
  }
  
  public static Map<String, String> toMap(final HttpEntity entity)
      throws IllegalStateException, IOException {
    final List<NameValuePair> parameters = URLEncodedUtils.parse(entity);
    final Map<String, String> map = new HashMap<String, String>();
    for(final NameValuePair parameter : parameters) {
      map.put(parameter.getName(), parameter.getValue());
    }
    return map;
  }
  
  public static String toString(final Header[] headers) {
    final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    for(final Header header : headers) {
      parameters.add(new BasicNameValuePair(header.getName(), header.getValue()));
    }
    return URLEncodedUtils.format(parameters, "UTF-8");
  }
}

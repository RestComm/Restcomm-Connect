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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrLookup;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class ConfigurationStringLookup extends StrLookup {
  private final Map<String, String> dictionary;
  
  public ConfigurationStringLookup() {
    super();
    dictionary = new HashMap<String, String>();
  }
  
  public void addProperty(final String name, final String value) {
    dictionary.put(name, value);
  }
  
  @Override public String lookup(final String key) {
	final String result = dictionary.get(key);
    if(result != null) {
      return result;
    } else {
      return key;
    }
  }
}

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
package org.mobicents.servlet.sip.restcomm.xml;

import java.util.HashSet;
import java.util.Set;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class BooleanAttribute extends AbstractAttribute {
  protected static final Set<String> VALUES;
  static {
    VALUES = new HashSet<String>();
    VALUES.add(Boolean.toString(Boolean.TRUE));
    VALUES.add(Boolean.toString(Boolean.FALSE));
  }
  
  public BooleanAttribute() {
    super();
  }
  
  public boolean getBooleanValue() {
    return Boolean.parseBoolean(value);
  }

  @Override public abstract String getName();

  @Override public boolean isSupportedValue(final String value) {
    return VALUES.contains(value);
  }
  
  public void setBooleanValue(final boolean newValue) {
    setValue(Boolean.toString(newValue));
  }
}

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

import java.util.regex.Pattern;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class PhoneNumberAttribute extends AbstractAttribute {
  private static final Pattern E164 = Pattern.compile("\\+?\\d{10,15}");
  private static final Pattern US_1 = Pattern.compile("\\d{3}\\-\\d{3}\\-\\d{4}");
  private static final Pattern US_2 = Pattern.compile("\\(\\d{3}\\)\\d{3}\\-\\d{4}");
  
  public PhoneNumberAttribute() {
    super();
  }
  
  @Override public abstract String getName();

  @Override public boolean isSupportedValue(final String value) {
    return E164.matcher(value).matches() || US_1.matcher(value).matches() || US_2.matcher(value).matches();
  }
}

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
package org.mobicents.servlet.sip.restcomm.xml.rcml;

import java.util.regex.Pattern;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.xml.AbstractAttribute;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class CallerId extends AbstractAttribute {
  public static final String NAME = "callerId";
  private static final Pattern  CLIENT = Pattern.compile("client:[_a-z]+");
  private static final Pattern E164 = Pattern.compile("\\+?\\d{10,15}");
  private static final Pattern US = Pattern.compile("\\d\\-\\d{3}\\-\\d{3}\\-\\d{4}");
  
  
  public CallerId() {
    super();
  }
  
  @Override public String getName() {
    return NAME;
  }

  @Override public boolean isSupportedValue(final String value) {
    return CLIENT.matcher(value).matches() || E164.matcher(value).matches() || US.matcher(value).matches();
  }
}

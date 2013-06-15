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
package org.mobicents.servlet.restcomm.util;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class TimeUtils {
  public static final long SECOND_IN_MILLIS = 1000;
  public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
  public static final long HOURS_IN_MILLIES = MINUTE_IN_MILLIS * 60;
  
  private TimeUtils() {
    super();
  }
  
  public static int millisToMinutes(final long milliseconds) {
    final long minute = 60 * 1000;
    final long remainder = milliseconds % minute;
    if(remainder != 0) {
      final long delta = minute - remainder;
      return (int)((milliseconds + delta) / minute);
    } else {
      return (int)(milliseconds / minute);
    }
  }
}

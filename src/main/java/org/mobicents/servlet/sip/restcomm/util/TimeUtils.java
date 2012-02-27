package org.mobicents.servlet.sip.restcomm.util;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

@ThreadSafe public final class TimeUtils {
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

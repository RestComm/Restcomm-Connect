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

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RangeCounter {
  private final long initialValue;
  private volatile long count;
  private final long limit;
  
  public RangeCounter(final long limit) {
	if(limit <= 0) {
	  throw new IllegalArgumentException("The counter limit can not be less than or equal to 0");
	}
	this.initialValue = 0;
	this.count = 0;
    this.limit = limit;
  }
  
  public RangeCounter(final long initialValue, final long limit) {
    if(limit <= initialValue) {
      throw new IllegalArgumentException("The counter limit can not be less than or equal to initial value");
    }
    this.initialValue = initialValue;
    this.count = initialValue;
    this.limit = limit;
  }
  
  public long get() {
    return count;
  }
  
  public synchronized long getAndIncrement() {
    final long result = count;
    increment();
    return result;
  }
  
  public synchronized void increment() {
    count += 1;
    if(count == limit) {
      count = initialValue;
    }
  }
}

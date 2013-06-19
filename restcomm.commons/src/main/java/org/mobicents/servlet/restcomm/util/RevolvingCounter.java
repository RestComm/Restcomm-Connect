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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RevolvingCounter {
  private final long start;
  private final long limit;
  private final AtomicLong count;
  private final Lock lock;
  
  public RevolvingCounter(final long limit) {
    this(0, limit);
  }
  
  public RevolvingCounter(final long start, final long limit) {
    super();
    this.start = start;
    this.limit = limit;
    this.count = new AtomicLong();
    this.count.set(start);
    this.lock = new ReentrantLock();
  }
  
  public long get() {
    long result = count.getAndIncrement();
    if(result >= limit) {
      while(!lock.tryLock()) { /* Spin */ }
      if(count.get() >= limit) {
        result = start;
        count.set(start + 1);
        lock.unlock();
      } else {
        lock.unlock();
        result = get();
      }
    }
    return result;
  }
}

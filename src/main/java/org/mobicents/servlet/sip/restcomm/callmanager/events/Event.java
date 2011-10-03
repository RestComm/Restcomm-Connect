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
package org.mobicents.servlet.sip.restcomm.callmanager.events;

public class Event<T extends EventType> {
  private final Object source;
  private final long timestamp;
  private final T type;
  
  public Event(final Object source, final T type) {
    this.source = source;
    this.timestamp = System.nanoTime();
    this.type = type;
  }
  
  public Object getSource() {
    return source;
  }
  
  public long getTimestamp() {
    return timestamp;
  }
  
  public T getType() {
    return type;
  }
}

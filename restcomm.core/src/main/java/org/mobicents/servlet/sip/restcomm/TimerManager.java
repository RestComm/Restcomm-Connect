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

import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.TimerListener;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class TimerManager implements TimerListener {
  private final Map<String, TimerListener> listeners;
  
  public TimerManager() {
    super();
    listeners = new ConcurrentHashMap<String, TimerListener>();
    ServiceLocator.getInstance().set(TimerManager.class, this);
  }
  
  public void register(final String key, final TimerListener listener) throws TooManyListenersException {
    if(!listeners.containsKey(key)) {
      listeners.put(key, listener);
    } else {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("There is already a listener registered for a key with the name ").append(key);
      throw new TooManyListenersException(buffer.toString());
    }
  }
  
  public boolean isRegistered(final String key) {
    return listeners.containsKey(key);
  }

  @Override public void timeout(final ServletTimer timer) {
    final String key = (String)timer.getInfo();
    final TimerListener listener = listeners.get(key);
    if(listener != null) {
      listener.timeout(timer);
    }
  }
  
  public void unregister(final String key) {
    if(listeners.containsKey(key)) { listeners.remove(key); }
  }
}

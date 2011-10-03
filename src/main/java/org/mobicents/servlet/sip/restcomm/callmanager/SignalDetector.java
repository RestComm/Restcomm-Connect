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
package org.mobicents.servlet.sip.restcomm.callmanager;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalDetectorEvent;
import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public abstract class SignalDetector extends FSM {
  // Signal detector states.
  public static final State IDLE = new State("idle");
  public static final State DETECTING = new State("detecting");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(DETECTING);
    IDLE.addTransition(FAILED);
    DETECTING.addTransition(IDLE);
    DETECTING.addTransition(FAILED);
  }
  
  private int numberOfDigits;
  private final List<EventListener<SignalDetectorEvent>> listeners;
  
  public SignalDetector() {
    super(IDLE);
    addState(IDLE);
    addState(DETECTING);
    addState(FAILED);
    this.listeners = new ArrayList<EventListener<SignalDetectorEvent>>();
  }
  
  public synchronized void addListener(final EventListener<SignalDetectorEvent> listener) {
    listeners.add(listener);
  }
  
  public synchronized void removeListener(final EventListener<SignalDetectorEvent> listener) {
    listeners.remove(listener);
  }
  
  protected synchronized void fire(final SignalDetectorEvent event) {
    for(final EventListener<SignalDetectorEvent> listener : listeners) {
      listener.onEvent(event);
    }
  }
  
  public int getNumberOfDigits() {
    return numberOfDigits;
  }
  
  public void setNumberOfDigits(final int numberOfDigits) {
    this.numberOfDigits = numberOfDigits;
  }
  
  public abstract void detect();
}

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

import org.mobicents.servlet.sip.restcomm.callmanager.events.CallEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public abstract class Call extends FSM {
  // Call Directions.
  public static final String INBOUND = "inbound";
  public static final String OUTBOUND_DIAL = "outbound-dial";
  // Call states.
  public static final State IDLE = new State("idle");
  public static final State QUEUED = new State("queued");
  public static final State RINGING = new State("ringing");
  public static final State IN_PROGRESS = new State("in-progress");
  public static final State COMPLETED = new State("completed");
  public static final State BUSY = new State("busy");
  public static final State FAILED = new State("failed");
  public static final State NO_ANSWER = new State("no-answer");
  public static final State CANCELLED = new State("cancelled");
  static {
    IDLE.addTransition(RINGING);
    IDLE.addTransition(QUEUED);
    RINGING.addTransition(IN_PROGRESS);
    RINGING.addTransition(FAILED);
    RINGING.addTransition(CANCELLED);
    IN_PROGRESS.addTransition(COMPLETED);
    IN_PROGRESS.addTransition(FAILED);
  }
  
  protected List<EventListener<CallEvent>> listeners;
  
  public Call() {
    super(IDLE);
    addState(IDLE);
    addState(QUEUED);
    addState(RINGING);
    addState(IN_PROGRESS);
    addState(COMPLETED);
    addState(BUSY);
    addState(FAILED);
    addState(NO_ANSWER);
    addState(CANCELLED);
    this.listeners = new ArrayList<EventListener<CallEvent>>();
  }
  
  public synchronized void addListener(final EventListener<CallEvent> listener) {
    listeners.add(listener);
  }
  
  public synchronized void removeListener(final EventListener<CallEvent> listener) {
    listeners.remove(listener);
  }
  
  protected synchronized void fire(final CallEvent event) {
	for(final EventListener<CallEvent> listener : listeners) {
	  listener.onEvent(event);
	}
  }
  
  public abstract void answer() throws CallException;
  public abstract void bridge(Call call) throws CallException;
  public abstract void connect() throws CallException;
  public abstract void dial() throws CallException;
  public abstract CallManager getCallManager();
  public abstract String getDirection();
  public abstract String getId();
  public abstract String getOriginator();
  public abstract Player getPlayer();
  public abstract String getRecipient();
  public abstract Recorder getRecorder();
  public abstract SignalDetector getSignalDetector();
  public abstract SpeechSynthesizer getSpeechSynthesizer();
  public abstract String getStatus();
  public abstract void hangup();
  public abstract void join(Conference conference);
  public abstract void reject();
  public abstract void unjoin(Conference conference);
}

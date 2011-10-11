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

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class Jsr309DtmfDetector extends FSM implements DtmfDetector, MediaEventListener<SignalDetectorEvent> {
  //Signal detector states.
  private static final State IDLE = new State("idle");
  private static final State DETECTING = new State("detecting");
  private static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(DETECTING);
    IDLE.addTransition(FAILED);
    DETECTING.addTransition(IDLE);
    DETECTING.addTransition(FAILED);
  }

  private final SignalDetector detector;
  private String digit;
  
  public Jsr309DtmfDetector(final SignalDetector detector) {
    super(IDLE);
    addState(IDLE);
    addState(DETECTING);
    addState(FAILED);
    this.detector = detector;
  }

  @Override public synchronized String detect() throws MediaException {
    assertState(IDLE);
    digit = null;
    detector.addListener(this);
    try {
      detector.receiveSignals(1, null, null, null);
	  setState(DETECTING);
	  wait();
	  return digit;
	} catch(final Exception exception) {
	  setState(FAILED);
	  throw new MediaException(exception);
	}
  }

  public void onEvent(final SignalDetectorEvent event) {
    detector.removeListener(this);
    if(event.isSuccessful()) {
      if(event.getEventType() == SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED) {
    	digit = event.getSignalString();
        notify();
      }
    }
  }
}

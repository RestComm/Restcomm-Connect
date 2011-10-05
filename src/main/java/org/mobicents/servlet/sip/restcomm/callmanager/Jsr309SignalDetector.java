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
import javax.media.mscontrol.MsControlException;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalDetectorEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalDetectorEventType;

public final class Jsr309SignalDetector extends SignalDetector implements MediaEventListener<javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent> {
  private static final Logger logger = Logger.getLogger(Jsr309SignalDetector.class);
  
  private final javax.media.mscontrol.mediagroup.signals.SignalDetector detector;
  
  public Jsr309SignalDetector(final javax.media.mscontrol.mediagroup.signals.SignalDetector detector) {
    super();
    this.detector = detector;
  }

  @Override public void detect() {
    assertState(IDLE);
    detector.addListener(this);
    try {
      detector.receiveSignals(getNumberOfDigits(), null, null, null);
	  setState(DETECTING);
	} catch(final MsControlException exception) {
	  setState(FAILED);
	  logger.error(exception);
	  fire(new SignalDetectorEvent(this, SignalDetectorEventType.FAILED));
	}
  }

  public void onEvent(final javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent event) {
    detector.removeListener(this);
    logger.info(event.getEventType());
    if(event.isSuccessful()) {
      if(event.getEventType() == javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED) {
        final SignalDetectorEvent done = new SignalDetectorEvent(this, SignalDetectorEventType.DONE_DETECTING);
        done.setDigits(event.getSignalString());
        fire(done);
      }
    }
  }
}

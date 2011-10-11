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

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.mediagroup.Recorder;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class Jsr309MediaRecorder extends FSM implements MediaRecorder {
  //Recorder states.
  public static final State IDLE = new State("idle");
  public static final State RECORDING = new State("recording");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(RECORDING);
    IDLE.addTransition(FAILED);
    RECORDING.addTransition(IDLE);
    RECORDING.addTransition(FAILED);
  }

  private final Recorder recorder;
  
  public Jsr309MediaRecorder(final Recorder recorder) {
    super(IDLE);
    addState(IDLE);
    addState(RECORDING);
    addState(FAILED);
    this.recorder = recorder;
  }

  @Override public void record(final URI destination) throws MediaException {
    assertState(IDLE);
    try {
	  recorder.record(destination, null, null);
	  setState(RECORDING);
	} catch(final MsControlException exception) {
	  setState(FAILED);
	  throw new MediaException(exception);
	}
  }

  @Override public void stop() {
	assertState(RECORDING);
    recorder.stop();
    setState(IDLE);
  }
}

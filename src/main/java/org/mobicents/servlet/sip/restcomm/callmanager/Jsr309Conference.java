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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.resource.enums.ParameterEnum;

public final class Jsr309Conference implements Conference {
  private static final int MAX_PARTICIPANTS = 40;
  private final String name;
  private final MediaSession session;
  private volatile MediaMixer mixer;
  private final Map<String, Call> calls;
  
  public Jsr309Conference(final String name, final MediaSession session) {
    super();
    this.name = name;
    this.session = session;
    this.calls = new HashMap<String, Call>();
  }
  
  public synchronized void addCall(final Call call) throws ConferenceException {
    try {
	  call.join(this);
      calls.put(call.getId(), call);
    } catch(final CallException exception) {
      throw new ConferenceException(exception);
    }
  }
  
  public MediaMixer getMixer() throws ConferenceException {
	if(mixer != null) {
	  return mixer;
	} else {
	  synchronized(this) {
		if(mixer == null) {
	      try {
	        final Parameters parameters = session.createParameters();
            parameters.put(ParameterEnum.MAX_PORTS, MAX_PARTICIPANTS);
            mixer = session.createMediaMixer(MediaMixer.AUDIO, parameters);
	      } catch(final MsControlException exception) {
	        throw new ConferenceException(exception);
	      }
		}
	  }
	  return mixer;
	}
  }

  @Override public String getName() {
    return name;
  }
  
  public synchronized void removeCall(final Call call) throws ConferenceException {
    try {
      call.leave(this);
      calls.remove(call.getId());
    } catch(final CallException exception) {
      throw new ConferenceException(exception);
    }
  }
  
  public synchronized void shutdown() {
	final Collection<Call> attendees = calls.values();
	for(final Call attendee : attendees) {
	  try {
	    attendee.leave(this);
	  } catch(final CallException ignored) { }
	}
    mixer.release();
    session.release();
  }
}

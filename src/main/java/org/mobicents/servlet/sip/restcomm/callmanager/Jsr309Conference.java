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

import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.resource.enums.ParameterEnum;


public final class Jsr309Conference implements Conference {
  private static final int MAX_PARTICIPANTS = 40;
  private final Map<String, Call> calls;
  private final MediaSession media;
  private final MediaMixer mixer;
  
  public Jsr309Conference(final MsControlFactory factory) throws MsControlException {
    super();
    calls = new HashMap<String, Call>();
    media = factory.createMediaSession();
    final Parameters parameters = media.createParameters();
    parameters.put(ParameterEnum.MAX_PORTS, MAX_PARTICIPANTS);
    mixer = media.createMediaMixer(MediaMixer.AUDIO, parameters);
  }
  
  public synchronized void addCall(final Call call) {
	final String id = call.getId();
    if(!calls.containsKey(id)) {
      calls.put(id, call);
    }
  }
  
  public MediaMixer getMixer() {
    return mixer;
  }
  
  public synchronized void removeCall(final Call call) {
	final String id = call.getId();
    if(calls.containsKey(id)) {
      calls.remove(id);
    }
  }
}

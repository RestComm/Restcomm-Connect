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
import java.util.Set;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;

import org.apache.log4j.Logger;

public final class Jsr309ConferenceCenter implements ConferenceCenter {
  private static final Logger logger = Logger.getLogger(Jsr309ConferenceCenter.class);
  private final Map<String, Conference> conferences;
  private final MsControlFactory factory;
  
  public Jsr309ConferenceCenter(final MsControlFactory factory) {
    super();
    this.conferences = new HashMap<String, Conference>();
    this.factory = factory;
  }
  
  public synchronized Conference getConference(final String name) throws ConferenceException {
    if(conferences.containsKey(name)) {
      return conferences.get(name);
    } else {
      try {
    	final MediaSession session = factory.createMediaSession();
        final Conference conference = new Jsr309Conference(name, session);
        conferences.put(name, conference);
        return conference;
      } catch(final Exception exception) {
        logger.error(exception);
        throw new ConferenceException(exception);
      }
    }
  }
  
  public synchronized Set<String> getConferenceNames() {
    return conferences.keySet();
  }
  
  public synchronized void removeConference(final String name) {
    if(conferences.containsKey(name)) {
      final Jsr309Conference conference = (Jsr309Conference)conferences.remove(name);
      conference.shutdown();
    }
  }
  
  public synchronized void shutdown() {
    final Set<String> names = getConferenceNames();
    for(final String name : names) {
      removeConference(name);
    }
  }
}

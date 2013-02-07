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
package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class MgcpConferenceCenter implements ConferenceCenter {
  private final MgcpServerManager serverManager;
  private final Map<String, MgcpConference> conferences;
  
  public MgcpConferenceCenter(final MgcpServerManager serverManager) {
    super();
    this.serverManager = serverManager;
    this.conferences = new ConcurrentHashMap<String, MgcpConference>();
  }
  
  @Override public synchronized Conference getConference(final String name) {
    MgcpConference conference = conferences.get(name);
    if(conference == null) {
      final MgcpServer server = serverManager.getMediaServer();
      conference = new MgcpConference(name, server);
      conference.start();
      conferences.put(name, conference);
    }
    return conference;
  }

  @Override public synchronized Set<String> getConferenceNames() {
    return conferences.keySet();
  }

  @Override public synchronized void removeConference(final String name) throws InterruptedException {
    if(conferences.containsKey(name)) {
      final MgcpConference conference = conferences.remove(name);
      conference.shutdown();
    }
  }
}

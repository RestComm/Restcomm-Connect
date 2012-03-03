package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;

@ThreadSafe public class MgcpConferenceCenter implements ConferenceCenter {
  private final MgcpServerManager serverManager;
  private final Map<String, MgcpConference> conferences;
  
  public MgcpConferenceCenter(final MgcpServerManager serverManager) {
    super();
    this.serverManager = serverManager;
    this.conferences = new HashMap<String, MgcpConference>();
  }
  
  @Override public Conference getConference(final String name) {
    MgcpConference conference = conferences.get(name);
    if(conference == null) {
      synchronized(this) {
        if(conference == null) {
          final MgcpServer server = serverManager.getMediaServer();
          conference = new MgcpConference(name, server);
          conferences.put(name, conference);
        }
      }
    }
    return conference;
  }

  @Override public Set<String> getConferenceNames() {
    return conferences.keySet();
  }

  @Override public synchronized void removeConference(final String name) {
    if(conferences.containsKey(name)) {
      conferences.remove(name);
    }
  }
}

package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

public final class MgcpConference implements Conference {
  private final String name;
  private final MgcpSession session;
  private final MgcpConferenceEndpoint endpoint;
  private final Map<Sid, MgcpCall> calls;

  public MgcpConference(final String name, final MgcpServer server) {
    super();
    this.name = name;
    this.session = server.createMediaSession();
    this.endpoint = session.getConferenceEndpoint();
    this.calls = new HashMap<Sid, MgcpCall>();
  }
  
  public synchronized void addCall(final Call call) throws InterruptedException {
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.join(this);
    calls.put(call.getSid(), mgcpCall);
  }
  
  public MgcpConferenceEndpoint getConferenceEndpoint() {
    return endpoint;
  }
	
  @Override public String getName() {
    return name;
  }
  
  public synchronized void removeCall(final Call call) throws InterruptedException {
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.leave(this);
    calls.remove(mgcpCall.getSid());
  }
}

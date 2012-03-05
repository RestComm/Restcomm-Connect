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

import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
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

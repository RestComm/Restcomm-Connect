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

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpConference extends FiniteStateMachine implements Conference, LifeCycle, MgcpConnectionObserver {
  private static final State INIT = new State(Status.INIT.toString());
  private static final State IN_PROGRESS = new State(Status.IN_PROGRESS.toString());
  private static final State COMPLETE = new State(Status.COMPLETED.toString());
  
  private final String name;
  private final Map<Sid, MgcpCall> calls;
  
  private final MgcpSession session;
  private MgcpConferenceEndpoint conference;
  private MgcpIvrEndpoint ivr;
  private MgcpConnection ivrOutboundConnection;
  private MgcpConnection ivrInboundConnection;

  public MgcpConference(final String name, final MgcpServer server) {
    super(INIT);
    addState(INIT);
    addState(IN_PROGRESS);
    addState(COMPLETE);
    this.name = name;
    this.calls = new HashMap<Sid, MgcpCall>();
    this.session = server.createMediaSession();
  }
  
  public synchronized void addCall(final Call call) throws InterruptedException {
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.join(this);
    calls.put(call.getSid(), mgcpCall);
  }
  
  public MgcpConferenceEndpoint getConferenceEndpoint() {
    return conference;
  }
	
  @Override public String getName() {
    return name;
  }
  
  public synchronized void removeCall(final Call call) throws InterruptedException {
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.leave(this);
    calls.remove(mgcpCall.getSid());
  }

  @Override public synchronized void halfOpen(final MgcpConnection connection) {
    if(connection == ivrOutboundConnection) {
      conference = session.getConferenceEndpoint();
      ivrInboundConnection = session.createConnection(conference, connection.getLocalDescriptor());
      ivrInboundConnection.addObserver(this);
      ivrInboundConnection.connect(ConnectionMode.Confrnce);
    }
  }

  @Override public synchronized void open(final MgcpConnection connection) {
    if(connection == ivrInboundConnection) {
      
    } else if(connection == ivrOutboundConnection) {
      
    }
  }

  @Override public synchronized void disconnected(final MgcpConnection connection) {
    if(connection == ivrOutboundConnection) {
      ivrOutboundConnection.removeObserver(this);
      ivrOutboundConnection = null;
      ivrInboundConnection.disconnect();
    } else if(connection == ivrInboundConnection) {
      ivrInboundConnection.removeObserver(this);
      ivrInboundConnection = null;
      ivr = null;
      notify();
    }
  }

  @Override public synchronized void failed(final MgcpConnection connection) {
    // Nothing to do.
  }

  @Override public synchronized void modified(final MgcpConnection connection) {
    // Nothing to do.
  }

  @Override public synchronized void start() throws RuntimeException {
    ivr = session.getIvrEndpoint();
    ivrOutboundConnection = session.createConnection(ivr);
    ivrOutboundConnection.addObserver(this);
    ivrOutboundConnection.connect(ConnectionMode.SendRecv);
    try {
      wait();
    } catch(final InterruptedException ignored) { return; }
  }

  @Override public synchronized void shutdown() {
    try {
      ivrOutboundConnection.disconnect();
      wait();
    } catch(final InterruptedException ignored) { return; }
  }
}

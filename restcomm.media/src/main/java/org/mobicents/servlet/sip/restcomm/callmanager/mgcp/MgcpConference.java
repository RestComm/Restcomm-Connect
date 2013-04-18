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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceException;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpConference extends FiniteStateMachine implements Conference, LifeCycle,
    MgcpConnectionObserver, MgcpIvrEndpointObserver {
  private static final Logger logger = Logger.getLogger(MgcpConference.class);
  private static final State INIT = new State(Status.INIT.toString());
  private static final State IN_PROGRESS = new State(Status.IN_PROGRESS.toString());
  private static final State COMPLETE = new State(Status.COMPLETED.toString());
  private static final State FAILED = new State(Status.FAILED.toString());
  static {
    INIT.addTransition(IN_PROGRESS);
    INIT.addTransition(FAILED);
    IN_PROGRESS.addTransition(COMPLETE);
    IN_PROGRESS.addTransition(FAILED);
  }
  
  private final Sid sid;
  private final String name;
  private final Map<Sid, MgcpCall> calls;
  private final List<ConferenceObserver> observers;
  
  private final MgcpServer server;
  private final MgcpSession session;
  private MgcpConferenceEndpoint conference;
  private MgcpIvrEndpoint ivrEndpoint;
  private MgcpConnection ivrOutboundConnection;
  private MgcpConnection ivrInboundConnection;

  public MgcpConference(final String name, final MgcpServer server) {
    super(INIT);
    addState(INIT);
    addState(IN_PROGRESS);
    addState(COMPLETE);
    addState(FAILED);
    
    this.sid = Sid.generate(Sid.Type.CONFERENCE);
    this.name = name;
    this.calls = new ConcurrentHashMap<Sid, MgcpCall>();
    this.observers = new CopyOnWriteArrayList<ConferenceObserver>();
    this.server = server;
    this.session = server.createMediaSession();
  }
  
  @Override public void addParticipant(final Call call) throws ConferenceException {
    assertState(IN_PROGRESS);
    final MgcpCall mgcpCall = (MgcpCall)call;
    if(!calls.containsKey(call.getSid())) {
      try {
        mgcpCall.join(this);
      } catch(final CallException exception) {
        throw new ConferenceException(exception);
      }
      calls.put(call.getSid(), mgcpCall);
    }
  }
  
  @Override public void addObserver(final ConferenceObserver observer) {
    observers.add(observer);
  }
  
  private void cleanup() {
    for(final MgcpCall call : calls.values()) {
      final MgcpCall mgcpCall = (MgcpCall)call;
      mgcpCall.leave(this);
    }
    calls.clear();
  }
  
  private void fireStatusChanged() {
    for(final ConferenceObserver observer : observers) {
      observer.onStatusChanged(this);
    }
  }
  
  public MgcpConferenceEndpoint getConferenceEndpoint() {
    assertState(IN_PROGRESS);
    return conference;
  }
	
  @Override public String getName() {
    return name;
  }
  
  @Override public int getNumberOfParticipants() {
    return calls.size();
  }

  @Override public Collection<Call> getParticipants() {
	final List<Call> result = new ArrayList<Call>();
	result.addAll(calls.values());
    return result;
  }
  
  @Override public Sid getSid() {
	return sid;
  }
  
  @Override public void play(final URI audio) {
    play(audio, 1);
  }
  
  @Override public void play(final URI audio, final int iterations) {
    assertState(IN_PROGRESS);
    final List<URI> uri = new ArrayList<URI>();
    uri.add(audio);
    synchronized(this) {
      try {
        ivrEndpoint.play(uri, iterations);
        wait();
      } catch(final InterruptedException ignored) {
        stop();
      }
    }
  }
  
  @Override public void stop() {
    assertState(IN_PROGRESS);
    ivrEndpoint.stop();
    try {
      block(2);
    } catch(final InterruptedException ignored) { }
  }
  
  private void block(int numberOfRequests) throws InterruptedException {
	  synchronized(this) {
	    wait(server.getResponseTimeout() * numberOfRequests);
	  }
  }

  @Override public Status getStatus() {
    return Status.getValueOf(getState().getName());
  }
  
  @Override public void removeParticipant(final Call call) {
    assertState(IN_PROGRESS);
    if(calls.containsKey(call.getSid())) {
      final MgcpCall mgcpCall = (MgcpCall)call;
      mgcpCall.leave(this);
      calls.remove(mgcpCall.getSid());
    }
  }
  
  @Override public void removeObserver(final ConferenceObserver observer) {
    observers.remove(observer);
  }

  @Override public void halfOpen(final MgcpConnection connection) {
    if(connection == ivrOutboundConnection) {
      conference = session.getConferenceEndpoint();
      ivrInboundConnection = session.createConnection(conference, connection.getLocalDescriptor());
      ivrInboundConnection.addObserver(this);
      ivrInboundConnection.connect(ConnectionMode.Confrnce);
    }
  }

  @Override public void open(final MgcpConnection connection) {
    if(connection == ivrInboundConnection) {
      ivrOutboundConnection.modify(connection.getLocalDescriptor());
    } else if(connection == ivrOutboundConnection) {
      setState(IN_PROGRESS);
      fireStatusChanged();
      synchronized(this) {
        notify();
      }
    }
  }

  @Override public void disconnected(final MgcpConnection connection) {
    // Nothing to do.
  }

  @Override public void failed(final MgcpConnection connection) {
    setState(FAILED);
    fireStatusChanged();
    synchronized(this) {
      notify();
    }
  }

  @Override public void modified(final MgcpConnection connection) {
    // Nothing to do.
  }

  @Override public void start() throws RuntimeException {
    assertState(INIT);
    ivrEndpoint = session.getIvrEndpoint();
    ivrEndpoint.addObserver(this);
    ivrOutboundConnection = session.createConnection(ivrEndpoint);
    ivrOutboundConnection.addObserver(this);
    ivrOutboundConnection.connect(ConnectionMode.SendRecv);
    block(3, INIT);
    assertState(IN_PROGRESS);
  }
  
  private void block(final int numberOfRequests, final State errorState) {
    try {
      // ResponseTimeout * NumberOfRequests
      synchronized(this) {
        wait(server.getResponseTimeout() * numberOfRequests);
      }
      if(errorState.equals(getState())) {
    	final StringBuilder buffer = new StringBuilder();
    	buffer.append("The server @ ").append(server.getDomainName()).append(" failed to create a conference. ")
    		.append("One or all of our requests failed to receive a response in time.");
        logger.warn(buffer.toString());
        server.destroyMediaSession(session);
        setState(FAILED);
        fireStatusChanged();
      }
    } catch(final InterruptedException ignored) { return; }
  }

  @Override public void shutdown() throws InterruptedException {
    assertState(IN_PROGRESS);
    stop();
    cleanup();
    server.destroyMediaSession(session);
    setState(COMPLETE);
    fireStatusChanged();
  }

  @Override public void operationCompleted(final MgcpIvrEndpoint endpoint) {
    synchronized(this) {
      notify();
    }
  }

  @Override public void operationFailed(final MgcpIvrEndpoint endpoint) {
    setState(FAILED);
    fireStatusChanged();
  }
}

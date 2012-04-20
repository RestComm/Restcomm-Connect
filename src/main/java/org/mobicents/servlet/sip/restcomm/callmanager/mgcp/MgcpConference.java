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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceObserver;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpConference extends FiniteStateMachine implements Conference, LifeCycle,
    MgcpConnectionObserver, MgcpIvrEndpointObserver {
  private static final State INIT = new State(Status.INIT.toString());
  private static final State IN_PROGRESS = new State(Status.IN_PROGRESS.toString());
  private static final State COMPLETE = new State(Status.COMPLETED.toString());
  private static final State FAILED = new State(Status.FAILED.toString());
  
  private final String name;
  private final Map<Sid, MgcpCall> calls;
  private final List<ConferenceObserver> observers;
  
  private final MgcpSession session;
  private MgcpConferenceEndpoint conference;
  private MgcpIvrEndpoint ivr;
  private MgcpConnection ivrOutboundConnection;
  private MgcpConnection ivrInboundConnection;
  
  private final List<URI> alertAudioFile;
  private List<URI> musicAudioFiles;
  private boolean backgroundMusic;

  public MgcpConference(final String name, final MgcpServer server) {
    super(INIT);
    addState(INIT);
    addState(IN_PROGRESS);
    addState(COMPLETE);
    addState(FAILED);
    this.name = name;
    this.calls = new HashMap<Sid, MgcpCall>();
    this.observers = new ArrayList<ConferenceObserver>();
    this.session = server.createMediaSession();
    this.alertAudioFile = new ArrayList<URI>();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    final URI uri = URI.create("file://" + configuration.getString("alert-audio-file"));
    alertAudioFile.add(uri);
  }
  
  public synchronized void addCall(final Call call) {
    assertState(IN_PROGRESS);
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.join(this);
    calls.put(call.getSid(), mgcpCall);
  }
  
  @Override public synchronized void addObserver(final ConferenceObserver observer) {
    observers.add(observer);
  }
  
  public synchronized void alert() {
    assertState(IN_PROGRESS);
    ivr.play(alertAudioFile, 1);
    try { wait(); }
    catch(final InterruptedException ignored) {
      ivr.stop();
    }
  }
  
  public void fireStatusChanged() {
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
  
  @Override synchronized public int getNumberOfParticipants() {
    return calls.size();
  }

  @Override synchronized public Collection<Call> getParticipants() {
	final List<Call> result = new ArrayList<Call>();
	result.addAll(calls.values());
    return result;
  }

  @Override synchronized public void playBackgroundMusic() {
    assertState(IN_PROGRESS);
    if(musicAudioFiles != null && !musicAudioFiles.isEmpty()) {
      backgroundMusic = true;
      ivr.play(musicAudioFiles, 1);
    } else {
      backgroundMusic = false;
    }
  }
  
  @Override synchronized public void setBackgroundMusic(final List<URI> musicAudioFiles) {
    this.musicAudioFiles = musicAudioFiles;
  }
  
  @Override synchronized public void stopBackgroundMusic() {
    assertState(IN_PROGRESS);
    if(backgroundMusic) {
      backgroundMusic = false;
      ivr.stop();
    }
  }
  
  @Override public Status getStatus() {
    return Status.valueOf(getState().getName());
  }
  
  @Override public synchronized void removeCall(final Call call) {
    assertState(IN_PROGRESS);
    final MgcpCall mgcpCall = (MgcpCall)call;
    mgcpCall.leave(this);
    calls.remove(mgcpCall.getSid());
  }
  
  @Override synchronized public void removeObserver(final ConferenceObserver observer) {
    observers.remove(observer);
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
      ivrOutboundConnection.modify(connection.getLocalDescriptor());
    } else if(connection == ivrOutboundConnection) {
      notify();
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
    setState(FAILED);
    fireStatusChanged();
    notify();
  }

  @Override public synchronized void modified(final MgcpConnection connection) {
    // Nothing to do.
  }

  @Override public synchronized void start() throws RuntimeException {
    assertState(INIT);
    ivr = session.getIvrEndpoint();
    ivrOutboundConnection = session.createConnection(ivr);
    ivrOutboundConnection.addObserver(this);
    ivrOutboundConnection.connect(ConnectionMode.SendRecv);
    try {
      wait();
    } catch(final InterruptedException ignored) { return; }
    assertState(INIT);
    setState(IN_PROGRESS);
    fireStatusChanged();
  }

  @Override public synchronized void shutdown() {
    assertState(IN_PROGRESS);
    stopBackgroundMusic();
    for(final MgcpCall call : calls.values()) {
      removeCall(call);
    }
    ivrOutboundConnection.disconnect();
    try {
      wait();
    } catch(final InterruptedException ignored) { return; }
    session.release();
    setState(COMPLETE);
    fireStatusChanged();
  }

  @Override synchronized public void operationCompleted(final MgcpIvrEndpoint endpoint) {
    if(backgroundMusic) {
      playBackgroundMusic();
    }
  }

  @Override synchronized public void operationFailed(final MgcpIvrEndpoint endpoint) {
    setState(FAILED);
    fireStatusChanged();
  }
}

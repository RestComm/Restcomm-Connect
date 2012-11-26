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

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpConference extends FiniteStateMachine implements Conference, LifeCycle,
    MgcpConnectionObserver, MgcpIvrEndpointObserver {
  private static final List<URI> emptyAnnouncement = new ArrayList<URI>();
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
  
  private final String name;
  private final Map<Sid, MgcpCall> calls;
  private final List<ConferenceObserver> observers;
  
  private final MgcpServer server;
  private final MgcpSession session;
  private MgcpConferenceEndpoint conference;
  private MgcpIvrEndpoint ivrEndpoint;
  private MgcpConnection ivrOutboundConnection;
  private MgcpConnection ivrInboundConnection;

  private List<URI> musicAudioFiles;
  private boolean backgroundMusic;
  private boolean recordAudio;
  private boolean playingAudio;

  public MgcpConference(final String name, final MgcpServer server) {
    super(INIT);
    addState(INIT);
    addState(IN_PROGRESS);
    addState(COMPLETE);
    addState(FAILED);
    this.name = name;
    this.calls = new ConcurrentHashMap<Sid, MgcpCall>();
    this.observers = new CopyOnWriteArrayList<ConferenceObserver>();
    this.server = server;
    this.session = server.createMediaSession();
    musicAudioFiles = null;
    backgroundMusic = false;
    recordAudio = false;
  }
  
  @Override public void addParticipant(final Call call) {
    assertState(IN_PROGRESS);
    final MgcpCall mgcpCall = (MgcpCall)call;
    if(!calls.containsKey(call.getSid())) {
      mgcpCall.join(this);
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
  
  @Override public synchronized int getNumberOfParticipants() {
    return calls.size();
  }

  @Override public synchronized Collection<Call> getParticipants() {
	final List<Call> result = new ArrayList<Call>();
	result.addAll(calls.values());
    return result;
  }
  
  @Override public synchronized void play(final URI audio) {
    assertState(IN_PROGRESS);
    if(audio.getPath().isEmpty()) return;
    if(!playingAudio) {
      playingAudio = true;
      final List<URI> uri = new ArrayList<URI>();
      uri.add(audio);
      ivrEndpoint.play(uri, 1);
      try { wait(); }
      catch(final InterruptedException ignored) { ivrEndpoint.stop(); }
    }
  }

  @Override public synchronized void playBackgroundMusic() {
    assertState(IN_PROGRESS);
    if(musicAudioFiles != null && !musicAudioFiles.isEmpty() &&
        !backgroundMusic) {
      backgroundMusic = true;
      ivrEndpoint.play(musicAudioFiles, 1);
    }
  }
  
  @Override public synchronized void recordAudio(final URI destination, final long length) {
    assertState(IN_PROGRESS);
    if(!recordAudio) {
      ivrEndpoint.playRecord(emptyAnnouncement, destination, TimeUtils.MINUTE_IN_MILLIS * 30, length, null);
      recordAudio = true;
    }
  }
  
  @Override public synchronized void setBackgroundMusic(final List<URI> musicAudioFiles) {
    this.musicAudioFiles = musicAudioFiles;
  }
  
  @Override public synchronized void stopBackgroundMusic() {
    assertState(IN_PROGRESS);
    if(backgroundMusic) {
      backgroundMusic = false;
      ivrEndpoint.stop();
      try { wait(); }
      catch(final InterruptedException ignored) { }
    }
  }
  
  @Override public synchronized void stopRecordingAudio() {
    assertState(IN_PROGRESS);
    if(recordAudio) {
      recordAudio = false;
      ivrEndpoint.stop();
      try { wait(); }
      catch(final InterruptedException ignored) { }
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
    ivrEndpoint = session.getIvrEndpoint();
    ivrEndpoint.addObserver(this);
    ivrOutboundConnection = session.createConnection(ivrEndpoint);
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
    stopRecordingAudio();
    cleanup();
    server.destroyMediaSession(session);
    setState(COMPLETE);
    fireStatusChanged();
  }

  @Override public synchronized void operationCompleted(final MgcpIvrEndpoint endpoint) {
    if(backgroundMusic) {
      backgroundMusic = false;
      playBackgroundMusic();
    } else if(recordAudio) {
      recordAudio = false;
    } else if(playingAudio) {
      playingAudio = false;
      notify();
    } else {
      notify();
    }
  }

  @Override public synchronized void operationFailed(final MgcpIvrEndpoint endpoint) {
    setState(FAILED);
    fireStatusChanged();
  }
}

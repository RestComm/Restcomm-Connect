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

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MgcpCall extends FiniteStateMachine
    implements Call, MgcpConnectionObserver, MgcpIvrEndpointObserver {
  private static final Logger LOGGER = Logger.getLogger(MgcpCall.class);
  // Call states.
  private static final State IDLE = new State(Status.IDLE.toString());
  private static final State QUEUED = new State(Status.QUEUED.toString());
  private static final State RINGING = new State(Status.RINGING.toString());
  private static final State IN_PROGRESS = new State(Status.IN_PROGRESS.toString());
  private static final State COMPLETED = new State(Status.COMPLETED.toString());
  private static final State BUSY = new State(Status.BUSY.toString());
  private static final State FAILED = new State(Status.FAILED.toString());
  private static final State NO_ANSWER = new State(Status.NO_ANSWER.toString());
  private static final State CANCELLED = new State(Status.CANCELLED.toString());
  static {
    IDLE.addTransition(RINGING);
    IDLE.addTransition(QUEUED);
    QUEUED.addTransition(IN_PROGRESS);
    QUEUED.addTransition(FAILED);
    QUEUED.addTransition(CANCELLED);
    RINGING.addTransition(IN_PROGRESS);
    RINGING.addTransition(FAILED);
    RINGING.addTransition(CANCELLED);
    IN_PROGRESS.addTransition(COMPLETED);
    IN_PROGRESS.addTransition(FAILED);
  }
  
  private Sid sid;
  private Direction direction;
  private List<CallObserver> observers;
  
  private SipServletRequest initialInvite;
  
  private MgcpServer server;
  private MgcpSession session;
  private MgcpPacketRelayEndpoint relayEndpoint;
  private MgcpConferenceEndpoint cnfEndpoint;
  private MgcpIvrEndpoint ivrEndpoint;
  private MgcpConferenceEndpoint remoteEndpoint;
  private MgcpConnection userAgentConnection;
  private MgcpConnection relayOutboundConnection;
  private MgcpConnection relayInboundConnection;
  private MgcpConnection ivrOutboundConnection;
  private MgcpConnection ivrInboundConnection;
  private MgcpConference remoteConference;
  private MgcpConnection remoteOutboundConnection;
  private MgcpConnection remoteInboundConnection;
  
  private volatile String digits;
  private volatile boolean muted;
  
  public MgcpCall(final MgcpServer server) {
    super(IDLE);
    // Initialize the state machine.
    addState(IDLE);
    addState(QUEUED);
    addState(RINGING);
    addState(IN_PROGRESS);
    addState(COMPLETED);
    addState(BUSY);
    addState(FAILED);
    addState(NO_ANSWER);
    addState(CANCELLED);
    this.sid = Sid.generate(Sid.Type.CALL);
    this.server = server;
    this.session = server.createMediaSession();
    this.observers = new ArrayList<CallObserver>();
  }
  
  public MgcpCall(final SipServletRequest initialInvite, final MgcpServer server) {
    this(server);
    this.initialInvite = initialInvite;
    setState(QUEUED);
  }
  
  @Override public synchronized void addObserver(final CallObserver observer) {
    observers.add(observer);
  }
  
  public synchronized void alert(final SipServletRequest request) throws IOException {
    assertState(IDLE);
	direction = Direction.INBOUND;
	final SipServletResponse ringing = request.createResponse(SipServletResponse.SC_RINGING);
	try {
	  ringing.send();
	  initialInvite = request;
	  setState(RINGING);
	  fireStatusChanged();
	} catch(final IOException exception) {
	  cleanup();
	  setState(FAILED);
	  fireStatusChanged();
	  LOGGER.error(exception);
	  throw exception;
	}
  }
  
  @Override public synchronized void answer() throws CallException {
    assertState(RINGING);
    try {
      // Try to negotiate media with a packet relay end point.
      relayEndpoint = session.getPacketRelayEndpoint();
      final byte[] offer = initialInvite.getRawContent();
      final ConnectionDescriptor remoteDescriptor = new ConnectionDescriptor(new String(offer));
      userAgentConnection = session.createConnection(relayEndpoint, remoteDescriptor);
      userAgentConnection.addObserver(this);
      userAgentConnection.connect(ConnectionMode.SendRecv);
      wait();
      // Send the response back to the caller.
      final byte[] answer = userAgentConnection.getLocalDescriptor().toString().getBytes();
      final SipServletResponse ok = initialInvite.createResponse(SipServletResponse.SC_OK);
      ok.setContent(answer, "application/sdp");
      ok.send();
      wait();
    } catch(final Exception exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      fireStatusChanged();
      LOGGER.error(exception);
      throw new CallException(exception);
    }
  }
  
  public synchronized void busy() {
    assertState(QUEUED);
    cleanup();
    setState(BUSY);
    fireStatusChanged();
  }
  
  public synchronized void bye(final SipServletRequest request) throws IOException {
    assertState(IN_PROGRESS);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
      if(remoteConference != null) {
        remoteConference.removeParticipant(this);
      }
    } finally {
      cleanup();
      setState(COMPLETED);
      fireStatusChanged();
    }
  }
  
  @Override public synchronized void cancel() throws CallException {
    assertState(QUEUED);
    if(Direction.OUTBOUND_DIAL == getDirection()) {
      final SipServletRequest cancel = initialInvite.createCancel();
      try {
        cancel.send();
        cleanup();
        setState(CANCELLED);
        fireStatusChanged();
      } catch(final IOException exception) {
    	cleanup();
    	setState(FAILED);
    	fireStatusChanged();
        throw new CallException(exception);
      }
    }
  }
  
  public synchronized void cancel(final SipServletRequest request) throws IOException {
    assertState(RINGING);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
    } finally {
      cleanup();
      setState(CANCELLED);
      fireStatusChanged();
    }
  }
  
  private void cleanup() {
	server.destroyMediaSession(session);
	initialInvite.getSession().invalidate();	  
  }
  
  @Override public synchronized void dial() throws CallException {
    assertState(QUEUED);
    direction = Direction.OUTBOUND_DIAL;
    // Try to negotiate media with a packet relay end point.
    try {
	  relayEndpoint = session.getPacketRelayEndpoint();
      userAgentConnection = session.createConnection(relayEndpoint);
      userAgentConnection.addObserver(this);
      userAgentConnection.connect(ConnectionMode.SendRecv);
      wait();
      final byte[] offer = userAgentConnection.getLocalDescriptor().toString().getBytes();
      initialInvite.setContent(offer, "application/sdp");
      initialInvite.send();
    } catch(final Exception exception) {
      cleanup();
      setState(FAILED);
      fireStatusChanged();
      final StringBuilder buffer = new StringBuilder();
      buffer.append("There was an error while dialing out from ");
      buffer.append(initialInvite.getFrom().toString()).append(" to ");
      buffer.append(initialInvite.getTo().toString());
      throw new CallException(exception);
    }
  }
  
  public synchronized void established() {
    assertState(RINGING);
    relayOutboundConnection = session.createConnection(relayEndpoint);
    relayOutboundConnection.addObserver(this);
    relayOutboundConnection.connect(ConnectionMode.SendRecv);
  }
  
  public synchronized void established(final SipServletResponse successResponse) throws IOException {
    assertState(QUEUED);
    final byte[] answer = successResponse.getRawContent();
    final ConnectionDescriptor remoteDescriptor = new ConnectionDescriptor(new String(answer));
    userAgentConnection.modify(remoteDescriptor);
    final SipServletRequest ack = successResponse.createAck();
    ack.send();
  }
  
  public synchronized void failed() {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(QUEUED);
    possibleStates.add(RINGING);
    possibleStates.add(IN_PROGRESS);
    assertState(possibleStates);
    final State currentState = getState();
    if(QUEUED.equals(currentState) || RINGING.equals(currentState)) {
      cleanup();
    } else if(IN_PROGRESS.equals(currentState)) {
      terminate();
    }
    setState(FAILED);
    fireStatusChanged();
  }
  
  private void fail(int code) {
    final SipServletResponse fail = initialInvite.createResponse(code);
    try {
      fail.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
    setState(FAILED);
  }
  
  private void fireStatusChanged() {
    for(final CallObserver observer : observers) {
      observer.onStatusChanged(this);
    }
  }
  
  @Override public String getDigits() {
    return digits;
  }

  @Override public Direction getDirection() {
    return direction;
  }
  
  @Override public String getForwardedFrom() {
    return null;
  }

  @Override public Sid getSid() {
    return sid;
  }

  @Override public String getOriginator() {
    final SipURI from = (SipURI)initialInvite.getFrom().getURI();
    return from.getUser();
  }
  
  @Override public String getOriginatorName() {
    return initialInvite.getFrom().getDisplayName();
  }

  @Override public String getRecipient() {
    final SipURI to = (SipURI)initialInvite.getTo().getURI();
    return to.getUser();
  }

  @Override public Status getStatus() {
    return Status.getValueOf(getState().getName());
  }

  @Override public synchronized void hangup() {
    assertState(IN_PROGRESS);
    if(remoteConference != null) {
      remoteConference.removeParticipant(this);
    }
    terminate();
	setState(COMPLETED);
	fireStatusChanged();
  }
  
  @Override public boolean isMuted() {
    return muted;
  }
  
  public synchronized void join(final MgcpConference conference) {
    assertState(IN_PROGRESS);
    remoteEndpoint = conference.getConferenceEndpoint();
    remoteOutboundConnection = session.createConnection(remoteEndpoint);
    remoteOutboundConnection.addObserver(this);
    remoteOutboundConnection.connect(ConnectionMode.Confrnce);
    try {
      wait();
    } catch(final InterruptedException ignored) {
      leave(conference);
    }
    remoteConference = conference;
  }
  
  public synchronized void leave(final MgcpConference conference) {
    assertState(IN_PROGRESS);
    if(remoteOutboundConnection != null) {
      session.destroyConnection(remoteOutboundConnection);
      try { wait(); }
      catch(final InterruptedException ignored) { }
      remoteOutboundConnection.removeObserver(this);
      remoteOutboundConnection = null;
      if(remoteInboundConnection != null) {
        remoteInboundConnection.removeObserver(this);
        remoteInboundConnection = null;
        remoteEndpoint = null;
      }
      remoteConference = null;
    }
  }
  
  @Override public synchronized void mute() {
    relayOutboundConnection.modify(ConnectionMode.RecvOnly);
    try { wait(); }
    catch(final InterruptedException ignored) {
      unmute();
    }
    muted = true;
  }

  @Override public synchronized void play(final List<URI> announcements, final int iterations) throws CallException {
    assertState(IN_PROGRESS);
    ivrEndpoint.play(announcements, iterations);
    try { wait(); }
    catch(final InterruptedException ignored) { stopMedia(); }
  }
  
  @Override public synchronized void playAndCollect(final List<URI> prompts, final int maxNumberOfDigits, final int minNumberOfDigits,
	      final long firstDigitTimer, final long interDigitTimer, final String endInputKey) throws CallException {
    assertState(IN_PROGRESS);
    ivrEndpoint.playCollect(prompts, maxNumberOfDigits, minNumberOfDigits, firstDigitTimer, interDigitTimer, endInputKey);
    try { wait(); }
    catch(final InterruptedException ignored) { stopMedia(); }
  }
  
  @Override public synchronized void playAndRecord(final List<URI> prompts, final URI recordId, final long postSpeechTimer,
      final long recordingLength, final String patterns) throws CallException {
  	assertState(IN_PROGRESS);
  	ivrEndpoint.playRecord(prompts, recordId, postSpeechTimer, recordingLength, patterns);
  	try { wait(); }
  	catch(final InterruptedException ignored) { stopMedia(); }
  }

  @Override public synchronized void reject() {
    assertState(RINGING);
    final SipServletResponse busy = initialInvite.createResponse(SipServletResponse.SC_BUSY_HERE);
    try {
      busy.send();
    } catch(final IOException exception) {
      cleanup();
      setState(FAILED);
      fireStatusChanged();
      LOGGER.error(exception);
    }
  }
  
  @Override public synchronized void removeObserver(CallObserver observer) {
    observers.remove(observer);
  }
  
  @Override public synchronized void setExpires(final int minutes) {
    initialInvite.getApplicationSession().setExpires(minutes);
  }
  
  @Override public synchronized void stopMedia() {
    assertState(IN_PROGRESS);
    ivrEndpoint.stop();
    try { wait(); }
  	catch(final InterruptedException ignored) { }
  }
  
  private void terminate() {
	final SipSession sipSession = initialInvite.getSession();
    final SipServletRequest bye = sipSession.createRequest("BYE");
    try {
      bye.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }
  
  @Override public synchronized void unmute() {
    relayOutboundConnection.modify(ConnectionMode.SendRecv);
    try { wait(); }
    catch(final InterruptedException ignored) { }
    muted = false;
  }
  
  public synchronized void updateInitialInvite(final SipServletRequest initialInvite) {
    this.initialInvite = initialInvite;
  }

  @Override public synchronized void operationCompleted(final MgcpIvrEndpoint endpoint) {
    digits = endpoint.getDigits();
    notify();
  }

  @Override public synchronized void operationFailed(final MgcpIvrEndpoint endpoint) {
    notify();
  }

  @Override public synchronized void halfOpen(final MgcpConnection connection) {
    if(connection == relayOutboundConnection) {
      cnfEndpoint = session.getConferenceEndpoint();
      relayInboundConnection = session.createConnection(cnfEndpoint, connection.getLocalDescriptor());
      relayInboundConnection.addObserver(this);
      relayInboundConnection.connect(ConnectionMode.Confrnce);
    } else if(connection == ivrOutboundConnection) {
      ivrInboundConnection = session.createConnection(cnfEndpoint, connection.getLocalDescriptor());
      ivrInboundConnection.addObserver(this);
      ivrInboundConnection.connect(ConnectionMode.Confrnce);
    } else if(connection == remoteOutboundConnection) {
      remoteInboundConnection = session.createConnection(cnfEndpoint, connection.getLocalDescriptor());
      remoteInboundConnection.addObserver(this);
      remoteInboundConnection.connect(ConnectionMode.Confrnce);
    } else if(connection == userAgentConnection) {
      notify();
    }
  }

  @Override public synchronized void open(final MgcpConnection connection) {
    if(connection == relayInboundConnection) {
      relayOutboundConnection.modify(connection.getLocalDescriptor());
    } if(connection == relayOutboundConnection) {
      ivrEndpoint = session.getIvrEndpoint();
      ivrEndpoint.addObserver(this);
      ivrOutboundConnection = session.createConnection(ivrEndpoint);
      ivrOutboundConnection.addObserver(this);
      ivrOutboundConnection.connect(ConnectionMode.SendRecv);
    } else if(connection == ivrInboundConnection) {
      ivrOutboundConnection.modify(connection.getLocalDescriptor());
    } if(connection == ivrOutboundConnection) {
      final List<State> possibleStates = new ArrayList<State>();
      possibleStates.add(QUEUED);
      possibleStates.add(RINGING);
      assertState(possibleStates);
      setExpires(480);
      setState(IN_PROGRESS);
      fireStatusChanged();
      if(Direction.INBOUND == getDirection()) {
        notify();
      }
    } else if(connection == remoteInboundConnection) {
      remoteOutboundConnection.modify(connection.getLocalDescriptor());
    } else if(connection == remoteOutboundConnection) {
      notify();
    } else if(connection == userAgentConnection) {
      if(direction == Direction.INBOUND) {
        notify();
      } else if(direction == Direction.OUTBOUND_DIAL) {
    	relayOutboundConnection = session.createConnection(relayEndpoint);
    	relayOutboundConnection.addObserver(this);
    	relayOutboundConnection.connect(ConnectionMode.SendRecv);
      }
    }
  }

  @Override public synchronized void disconnected(final MgcpConnection connection) {
    if(connection == remoteOutboundConnection) {
      if(remoteInboundConnection != null) {
    	session.destroyConnection(remoteInboundConnection);
      } else {
        notify();
      }
    } else if(connection == remoteInboundConnection) {
      notify();
    }
  }

  @Override public synchronized void failed(final MgcpConnection connection) {
	failed();
  	notify();
  }

  @Override public synchronized void modified(final MgcpConnection connection) {
    if(connection == relayOutboundConnection) {
      notify();
    }
  }
}

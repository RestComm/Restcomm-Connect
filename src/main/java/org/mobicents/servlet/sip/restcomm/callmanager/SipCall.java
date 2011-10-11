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

import java.io.IOException;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class SipCall extends FSM implements Call, MediaEventListener<SdpPortManagerEvent> {
  // Logger.
  private static final Logger LOGGER = Logger.getLogger(SipCall.class);
  //Call Directions.
  public static final String INBOUND = "inbound";
  public static final String OUTBOUND_DIAL = "outbound-dial";
  // Call states.
  public static final State IDLE = new State("idle");
  public static final State QUEUED = new State("queued");
  public static final State RINGING = new State("ringing");
  public static final State IN_PROGRESS = new State("in-progress");
  public static final State COMPLETED = new State("completed");
  public static final State BUSY = new State("busy");
  public static final State FAILED = new State("failed");
  public static final State NO_ANSWER = new State("no-answer");
  public static final State CANCELLED = new State("cancelled");
  static {
    IDLE.addTransition(RINGING);
    IDLE.addTransition(QUEUED);
    RINGING.addTransition(IN_PROGRESS);
    RINGING.addTransition(FAILED);
    RINGING.addTransition(CANCELLED);
    IN_PROGRESS.addTransition(COMPLETED);
    IN_PROGRESS.addTransition(FAILED);
  }

  private final CallManager manager;
  private final SipServletRequest invite;
  private final MediaSession session;
  private MediaGroup media;
  private NetworkConnection connection;
  private volatile Jsr309MediaPlayer player;
  private volatile Jsr309MediaRecorder recorder;
  private volatile Jsr309DtmfDetector detector;
  private volatile Jsr309SpeechSynthesizer synthesizer;
  private volatile String direction;
  private volatile boolean bridged;
  private volatile boolean connected;
  private volatile boolean joined;
  
  public SipCall(final SipServletRequest request, final MediaSession session, final CallManager manager) {
	// Initialize the state machine.
    super(IDLE);
    addState(IDLE);
    addState(QUEUED);
    addState(RINGING);
    addState(IN_PROGRESS);
    addState(COMPLETED);
    addState(BUSY);
    addState(FAILED);
    addState(NO_ANSWER);
    addState(CANCELLED);
    // Finish initialization.
    this.manager = manager;
    this.invite = request;
    this.session = session;
    this.bridged = false;
    this.connected = false;
    this.joined = false;
  }
  
  public synchronized void alert(final SipServletRequest request) throws CallException {
    assertState(IDLE);
	direction = INBOUND;
	final SipServletResponse ringing = request.createResponse(SipServletResponse.SC_RINGING);
	try {
	  ringing.send();
	  setState(RINGING);
	} catch(final IOException exception) {
	  setState(FAILED);
	  cleanup();
	  throw new CallException(exception);
	}
  }
  
  @Override public synchronized void answer() throws CallException {
    assertState(RINGING);
    try {
      connection = session.createNetworkConnection(NetworkConnection.BASIC);
      final SdpPortManager sdp = connection.getSdpPortManager();
      sdp.addListener(this);
      final byte[] offer = invite.getRawContent();
      sdp.processSdpOffer(offer);
      // Wait 30 seconds for the call to be established.
      wait(30 * 1000);
      // Make sure nothing went wrong.
      if(!getState().equals(IN_PROGRESS)) {
    	final StringBuilder buffer = new StringBuilder();
    	buffer.append("The call to recipient ").append(getRecipient())
    	    .append(" from sender ").append(getOriginator())
    	    .append(" could not be completed.");
        throw new CallException(buffer.toString());
      }
    } catch(final Exception exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      throw new CallException(exception);
    }
  }
  
  public synchronized void answered(final SipServletResponse response) throws CallException {
    assertState(QUEUED);
    final SipServletRequest ack = response.createAck();
    try {
      ack.send();
      setState(IN_PROGRESS);
      notify();
    } catch(final IOException exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      throw new CallException(exception);
    }
  }
  
  @Override public synchronized void bridge(final Call call) throws CallException {
    assertState(IN_PROGRESS);
    final SipCall sipCall = (SipCall)call;
    try {
      sipCall.connection.join(Direction.DUPLEX, connection);
      bridged = true;
    } catch(final MsControlException exception) {
      setState(FAILED);
      terminate();
      throw new CallException(exception);
    }
  }
  
  public synchronized void bye(final SipServletRequest request) throws CallException {
    assertState(IN_PROGRESS);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
      setState(COMPLETED);
    } catch(final IOException exception) {
      cleanup();
      throw new CallException(exception);
    }
    cleanup();
  }
  
  public synchronized void cancel(final SipServletRequest request) throws CallException {
    assertState(RINGING);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
      setState(CANCELLED);
    } catch(final IOException exception) {
      cleanup();
      throw new CallException(exception);
    }
    cleanup();
  }
  
  private void cleanup() {
    session.release();
    invite.getSession().invalidate();
  }
  
  @Override public synchronized void connect() throws CallException {
	assertState(IN_PROGRESS);
    try {
      media = session.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
      media.join(Direction.DUPLEX, connection);
      player = new Jsr309MediaPlayer(media.getPlayer());
      recorder = new Jsr309MediaRecorder(media.getRecorder());
      detector = new Jsr309DtmfDetector(media.getSignalDetector());
      synthesizer = new Jsr309SpeechSynthesizer(media.getPlayer());
      connected = true;
    } catch(final MsControlException exception) {
      setState(FAILED);
      terminate();
      throw new CallException(exception);
    }
  }
  
  @Override public synchronized void dial() throws CallException {
    assertState(IDLE);
    direction = OUTBOUND_DIAL;
    setState(QUEUED);
    try {
	  connection = session.createNetworkConnection(NetworkConnection.BASIC);
      final SdpPortManager sdp = connection.getSdpPortManager();
      final byte[] offer = sdp.getMediaServerSessionDescription();
      invite.setContent(offer, "application/sdp");
      invite.send();
      // Wait 30 seconds for the call to be established.
      wait(30 * 1000);
      // Make sure nothing went wrong.
      if(!getState().equals(IN_PROGRESS)) {
    	final StringBuilder buffer = new StringBuilder();
    	buffer.append("The call to recipient ").append(getRecipient())
    	    .append(" from sender ").append(getOriginator())
    	    .append(" could not be completed.");
        throw new CallException(buffer.toString());
      }
	} catch(final Exception exception) {
	  setState(FAILED);
	  cleanup();
	  throw new CallException(exception);
	}
  }
  
  @Override public synchronized void disconnect() throws CallException {
  	if(connected) {
  	  try {
  	    media.unjoin(connection);
  	    media.release();
  	    media = null;
  	    player = null;
  	    recorder = null;
  	    detector = null;
  	    synthesizer = null;
  	    connected = false;
  	  } catch(final MsControlException exception) {
  		setState(FAILED);
  		terminate();
  	    throw new CallException(exception);
  	  }
  	}
  }
  
  public synchronized void established() throws CallException {
	assertState(RINGING);
	setState(IN_PROGRESS);
    notify();
  }
  
  private void fail(int code) {
    setState(FAILED);
    final SipServletResponse fail = invite.createResponse(code);
    try {
      fail.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }
  
  @Override public CallManager getCallManager() {
    return manager;
  }
  
  @Override public String getDirection() {
    return direction;
  }

  @Override public String getId() {
    return invite.getSession().getId();
  }
  
  @Override public String getOriginator() {
	final SipURI from = (SipURI)invite.getFrom().getURI();
    return from.getUser();
  }
  
  @Override public MediaPlayer getPlayer() {
    return player;
  }
  
  @Override public String getRecipient() {
    final SipURI to = (SipURI)invite.getTo().getURI();
    return to.getUser();
  }
  
  @Override public MediaRecorder getRecorder() {
    return recorder;
  }
  
  @Override public DtmfDetector getSignalDetector() {
    return detector;
  }
  
  @Override public SpeechSynthesizer getSpeechSynthesizer() {
    return synthesizer;
  }
  
  @Override public String getStatus() {
    return getState().getName();
  }

  @Override public synchronized void hangup() {
    assertState(IN_PROGRESS);
    terminate();
	setState(COMPLETED);
  }
  
  @Override public boolean isBridged() {
    return bridged;
  }
  
  @Override public boolean isConnected() {
    return connected;
  }
  
  @Override public boolean isInConference() {
    return joined;
  }
  
  @Override public synchronized void join(final Conference conference) throws CallException {
	final Jsr309Conference jsr309Conference = (Jsr309Conference)conference;
	try {
	  jsr309Conference.getMixer().join(Direction.DUPLEX, connection);
	  joined = true;
	} catch(final Exception exception) {
	  setState(FAILED);
	  terminate();
	  throw new CallException(exception);
	}
  }
  
  @Override public synchronized void leave(final Conference conference) throws CallException {
    if(joined) {
	  final Jsr309Conference jsr309Conference = (Jsr309Conference)conference;
	  try {
	    jsr309Conference.getMixer().unjoin(connection);
	    joined = false;
	  } catch(final Exception exception) {
	    setState(FAILED);
	    terminate();
	    throw new CallException(exception);
	  }
    }
  }
  
  public void onEvent(final SdpPortManagerEvent event) {
    if(event.isSuccessful()) {
      final SipServletResponse ok = invite.createResponse(SipServletResponse.SC_OK);
      try {
        final byte[] answer = event.getMediaServerSdp();
        ok.setContent(answer, "application/sdp");
        ok.send();
      } catch(final IOException exception) {
    	fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
        LOGGER.error(exception);
      }
    } else {
      if(event.getError().equals(SdpPortManagerEvent.SDP_NOT_ACCEPTABLE)) {
        fail(SipServletResponse.SC_NOT_ACCEPTABLE_HERE);
      } else if(event.getError().equals(SdpPortManagerEvent.RESOURCE_UNAVAILABLE)) {
    	fail(SipServletResponse.SC_BUSY_HERE);
      } else {
        fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      }
      LOGGER.error(event.getErrorText());
    }
  }
  
  @Override public synchronized void reject() {
    assertState(RINGING);
    final SipServletResponse busy = invite.createResponse(SipServletResponse.SC_BUSY_HERE);
    try {
      busy.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }
  
  private void terminate() {
	final SipSession sipSession = invite.getSession();
    final SipServletRequest bye = sipSession.createRequest("BYE");
    try {
      bye.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }
  
  @Override public synchronized void unbridge(final Call call) throws CallException {
    if(bridged) {
      final SipCall sipCall = (SipCall)call;
      try {
        sipCall.connection.unjoin(connection);
        bridged = false;
      } catch(final MsControlException exception) {
        setState(FAILED);
        terminate();
        throw new CallException(exception);
      }
    }
  }
}

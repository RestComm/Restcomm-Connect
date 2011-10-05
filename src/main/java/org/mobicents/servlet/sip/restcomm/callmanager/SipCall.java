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
import java.util.ArrayList;
import java.util.List;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.JoinEvent;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.callmanager.events.CallEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.CallEventType;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalEventType;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class SipCall extends Call implements EventListener<SignalEvent>,
    JoinEventListener, MediaEventListener<SdpPortManagerEvent> {
  // Logger.
  private static final Logger logger = Logger.getLogger(SipCall.class);
  
  private final CallManager manager;
  private final SipServletRequest request;
  private final SipSession session;
  private MediaSession media;
  private NetworkConnection connection;
  private MediaGroup group;
  private Player player;
  private Recorder recorder;
  private SignalDetector detector;
  private String direction;
  
  public SipCall(final SipServletRequest request, final CallManager manager) {
    super();
    this.manager = manager;
    this.request = request;
    this.session = request.getSession();
    final Jsr309MediaServerManager mediaServerManager = Jsr309MediaServerManager.getInstance();
    final Jsr309MediaServer mediaServer = mediaServerManager.getMediaServer();
    try {
      final MsControlFactory msControlFactory = mediaServer.getMsControlFactory();
	  this.media = msControlFactory.createMediaSession();
	} catch(final MsControlException exception) {
	  logger.error(exception);
	}
  }
  
  private void alert() {
	assertState(IDLE);
	direction = INBOUND;
    setState(RINGING);
  }
  
  @Override public void answer() throws CallException {
    assertState(RINGING);
    try {
      connection = media.createNetworkConnection(NetworkConnection.BASIC);
      final SdpPortManager sdp = connection.getSdpPortManager();
      sdp.addListener(this);
      final byte[] offer = request.getRawContent();
      sdp.processSdpOffer(offer);
    } catch(final Exception exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      throw new CallException(exception);
    }
  }
  
  private void answered() {
    assertState(QUEUED);
    setState(IN_PROGRESS);
    fire(new CallEvent(this, CallEventType.IN_CALL));
  }
  
  @Override public void bridge(final Call call) throws CallException {
	if(call.getState().equals(IN_PROGRESS)) {
	  assertState(IN_PROGRESS);
	} else {
	  throw new IllegalStateException("Cannot bridge to a call in a " + call.getState() + " state.");
	}
	// Bridge the calls.
	final SipCall sipCall = (SipCall)call;
    try {
	  this.connection.join(Direction.DUPLEX, sipCall.connection);
	} catch(final MsControlException exception) {
	  throw new CallException(exception);
	}
  }
  
  private void bye(final SignalEvent event) throws CallException {
	assertState(IN_PROGRESS);
    final SipServletRequest request = event.getRequest();
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
    try {
      response.send();
      setState(COMPLETED);
    } catch(final IOException exception) {
      cleanup();
      fire(new CallEvent(this, CallEventType.HANGUP));
      throw new CallException(exception);
    }
    cleanup();
    fire(new CallEvent(this, CallEventType.HANGUP));
  }
  
  private void cancel(final SignalEvent event) throws CallException {
    assertState(RINGING);
    final SipServletRequest request = event.getRequest();
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
    try {
      response.send();
      setState(CANCELLED);
    } catch(final IOException exception) {
      cleanup();
      throw new CallException(exception);
    }
    cleanup();
  }
  
  private void cleanup() {
    media.release();
    session.invalidate();
  }
  
  @Override public void connect() throws CallException {
	final List<State> possibleStates = new ArrayList<State>();
	possibleStates.add(RINGING);
	possibleStates.add(QUEUED);
	possibleStates.add(IN_PROGRESS);
	assertState(possibleStates);
    try {
      group = media.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
      group.addListener(this);
      group.joinInitiate(Direction.DUPLEX, connection, null);
    } catch(final MsControlException exception) {
      terminate();
      setState(FAILED);
      throw new CallException(exception);
    }
  }
  
  @Override public void dial() throws CallException {
    assertState(IDLE);
    direction = OUTBOUND_DIAL;
    setState(QUEUED);
    try {
	  connection = media.createNetworkConnection(NetworkConnection.BASIC);
      final SdpPortManager sdp = connection.getSdpPortManager();
      final byte[] offer = sdp.getMediaServerSessionDescription();
      request.setContent(offer, "application/sdp");
      request.send();
	} catch(final Exception exception) {
	  setState(FAILED);
	  cleanup();
	  throw new CallException(exception);
	}
  }
  
  private void fail(int code) {
    final SipServletResponse response = request.createResponse(code);
    try {
      response.send();
    } catch(final IOException exception) {
      logger.error(exception);
    }
    setState(FAILED);
    cleanup();
  }
  
  @Override public CallManager getCallManager() {
    return manager;
  }
  
  @Override public String getDirection() {
    return direction;
  }

  @Override public String getId() {
    return session.getId();
  }
  
  @Override public String getOriginator() {
	final SipURI from = (SipURI)request.getFrom().getURI();
    return from.getUser();
  }
  
  @Override public org.mobicents.servlet.sip.restcomm.callmanager.Player getPlayer() {
    return new Jsr309Player(player);
  }
  
  @Override public String getRecipient() {
    final SipURI to = (SipURI)request.getTo().getURI();
    return to.getUser();
  }
  
  @Override public org.mobicents.servlet.sip.restcomm.callmanager.Recorder getRecorder() {
    return new Jsr309Recorder(recorder);
  }
  
  @Override public org.mobicents.servlet.sip.restcomm.callmanager.SignalDetector getSignalDetector() {
    return new Jsr309SignalDetector(detector);
  }
  
  @Override public SpeechSynthesizer getSpeechSynthesizer() {
    return new Jsr309SpeechSynthesizer(player);
  }
  
  @Override public String getStatus() {
    return getState().getName();
  }

  @Override public void hangup() {
    assertState(IN_PROGRESS);
    terminate();
	setState(COMPLETED);
  }
  
  @Override public void join(final Conference conference) {
    final Jsr309Conference sipConference = (Jsr309Conference)conference;
    final MediaMixer mixer = sipConference.getMixer();
  }
  
  public void onEvent(final SignalEvent event) {
	try {
	  if(event.getType().equals(SignalEventType.ALERT)) {
	    alert();
	  } else if(event.getType().equals(SignalEventType.ANSWERED)) {
		answered();
	  } else if(event.getType().equals(SignalEventType.BYE)) {
	    bye(event);
	  } else if(event.getType().equals(SignalEventType.CANCEL)) {
	    cancel(event);
	  } else if(event.getType().equals(SignalEventType.CONNECTED)) {
	    connect();
	  } else if(event.getType().equals(SignalEventType.FAILED)) {
	    
	  }
	} catch(final CallException exception) {
	  logger.error(exception);
	}
  }
  
  public void onEvent(final JoinEvent event) {
    if(event.isSuccessful()) {
      if(event.getEventType().equals(JoinEvent.JOINED)) {
        try {
           player = group.getPlayer();
           recorder = group.getRecorder();
           detector = group.getSignalDetector();
           setState(IN_PROGRESS);
           // Fire an answered event.
           fire(new CallEvent(this, CallEventType.IN_CALL));
        } catch(final MsControlException exception) {
          terminate();
          setState(FAILED);
          logger.error(exception);
        }
      }
    } else {
      terminate();
      setState(FAILED);
      logger.error(event.getErrorText());
    }
  }
  
  public void onEvent(final SdpPortManagerEvent event) {
    if(event.isSuccessful()) {
      final SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
      try {
        final byte[] answer = event.getMediaServerSdp();
        response.setContent(answer, "application/sdp");
        response.send();
      } catch(final IOException exception) {
        setState(FAILED);
        cleanup();
        logger.error(exception);
      }
    } else {
      logger.error(event.getErrorText());
      if(event.getError().equals(SdpPortManagerEvent.SDP_NOT_ACCEPTABLE)) {
        fail(SipServletResponse.SC_NOT_ACCEPTABLE_HERE);
      } else if(event.getError().equals(SdpPortManagerEvent.RESOURCE_UNAVAILABLE)) {
    	fail(SipServletResponse.SC_BUSY_HERE);
      } else {
        fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      }
    }
  }
  
  @Override public void reject() {
    assertState(RINGING);
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_BUSY_HERE);
    try {
      response.send();
    } catch(final IOException exception) {
      logger.error(exception);
    }
    cleanup();
  }
  
  private void terminate() {
    final SipServletRequest request = session.createRequest("BYE");
    try {
      request.send();
    } catch(final IOException exception) {
      logger.error(exception);
    }
    cleanup();
  }
  
  @Override public void unjoin(Conference conference) {
    
  }
}

package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class IvrEndPoint extends FSM implements EndPoint, JainMgcpListener {
  private static final Logger LOGGER = Logger.getLogger(IvrEndPoint.class);
  private static final String PREFIX = "/mobicents/media/IVR/";
  private static final PackageName PACKAGE_NAME = PackageName.factory("AU");
  private static final RequestedEvent[] REQUESTED_EVENTS = new RequestedEvent[2];
  // IVR end point states.
  private static final State IDLE = new State("idle");
  private static final State CONNECTED = new State("connected");
  private static final State DISCONNECTED = new State("disconnected");
  private static final State FAILED = new State("failed");
  private static final State PLAY = new State("play");
  private static final State PLAY_COLLECT = new State("play-collect");
  private static final State PLAY_RECORD = new State("play-record");
  static {
    final RequestedAction[] action = new RequestedAction[] { RequestedAction.NotifyImmediately };
    REQUESTED_EVENTS[0] = new RequestedEvent(new EventName(PACKAGE_NAME, MgcpEvent.factory("oc")), action);
    REQUESTED_EVENTS[1] = new RequestedEvent(new EventName(PACKAGE_NAME, MgcpEvent.factory("of")), action);
    // Initialize state transitions.
    IDLE.addTransition(CONNECTED);
    IDLE.addTransition(FAILED);
    CONNECTED.addTransition(DISCONNECTED);
    CONNECTED.addTransition(FAILED);
    CONNECTED.addTransition(PLAY);
    CONNECTED.addTransition(PLAY_COLLECT);
    CONNECTED.addTransition(PLAY_RECORD);
    PLAY.addTransition(CONNECTED);
    PLAY.addTransition(FAILED);
    PLAY_COLLECT.addTransition(CONNECTED);
    PLAY_COLLECT.addTransition(FAILED);
    PLAY_RECORD.addTransition(CONNECTED);
    PLAY_RECORD.addTransition(FAILED);
  }
  
  private final MgcpServer server;
  private final MgcpMediaSession mediaSession;
  
  private ConnectionIdentifier connectionId;
  private EndpointIdentifier endPointId;
  private volatile RequestIdentifier requestId;
  private volatile String digits;
  private volatile String recordingId;
  
  public IvrEndPoint(final MgcpServer server, final MgcpMediaSession mediaSession) {
    super(IDLE);
    this.server = server;
    this.mediaSession = mediaSession;
  }
  
  public void connect(final EndpointIdentifier otherEndPoint) throws EndPointException {
    assertState(IDLE);
    // Try to create a new connection to a packet relay end point.
 	final String localEndPointName = new StringBuilder().append(PREFIX).append("$").toString();
 	final EndpointIdentifier endPointId = new EndpointIdentifier(localEndPointName, server.getDomainName());
     try {
       final CreateConnection request = new CreateConnection(this, mediaSession.getCallId(), endPointId, ConnectionMode.SendRecv);
       request.setNotifiedEntity(server.getCallAgent());
       request.setSecondEndpointIdentifier(otherEndPoint);
       request.setTransactionHandle(server.generateTransactionId());
       server.sendEvent(request, this);
       // Wait up to 30 seconds for a response.
       synchronized(this) {
         try {
           final int timeout = 30;
           wait(timeout * 1000);
         } catch(final InterruptedException ignored) { }
       }
     } catch(final Exception exception) {
       throw new EndPointException(exception);
     }
     // Make sure the connection was established successfully.
     final State currentState = getState();
     if(!currentState.equals(CONNECTED)) {
       final StringBuilder buffer = new StringBuilder();
       if(currentState.equals(IDLE)) {
     	setState(FAILED);
         buffer.append("Timed out while establishing a connection to an IVR end point.\n");
       } else {
         buffer.append("Could not establish a connection to an IVR end point.\n");
       }
       throw new EndPointException(buffer.toString());
     }
  }
  
  public void disconnect() {
    assertState(CONNECTED);
    // Try to delete the connection to the IVR end point.
    final DeleteConnection request = new DeleteConnection(this, mediaSession.getCallId(), endPointId, connectionId);
    request.setTransactionHandle(server.generateTransactionId());
    server.sendEvent(request, this);
    setState(DISCONNECTED);
  }
  
  public String getDigits() {
    return digits;
  }

  @Override public EndpointIdentifier getEndPointId() {
    return endPointId;
  }
  
  public String getRecordingId() {
    return recordingId;
  }
  
  public synchronized void play(final List<URI> announcements, final int iterations) throws EndPointException {
	assertState(CONNECTED);
	// Create the signal parameters.
    final AdvancedAudioParametersBuilder builder = new AdvancedAudioParametersBuilder();
    for(final URI announcement : announcements) {
      builder.addAnnouncement(announcement);
    }
    builder.setIterations(iterations);
    final String parameters = builder.build();
    // Create the signal.
    final EventName[] signal = new EventName[1];
    signal[0] = new EventName(PACKAGE_NAME, MgcpEvent.factory("pa").withParm(parameters));
    // Create notification request.
 	requestId = server.generateRequestIdentifier();
 	final NotificationRequest request = new NotificationRequest(this, endPointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    request.setTransactionHandle(server.generateTransactionId());
    // Send the request.
    server.sendEvent(request, this);
    setState(PLAY);
    // Wait for a response.
    try {
     wait();
    } catch(final InterruptedException ignored) { }
    // Make sure the request completed successfully.
    final State currentState = getState();
    if(!currentState.equals(CONNECTED)) {
      throw new EndPointException("There was an error while playing.");
    }
  }
  
  public void playCollect(final List<URI> prompts, final int maxNumberOfDigits, final int minNumberOfDigits,
      final long firstDigitTimer, final long interDigitTimer, final String endInputKey) throws EndPointException {
    assertState(CONNECTED);
    // Create the signal parameters.
    final AdvancedAudioParametersBuilder builder = new AdvancedAudioParametersBuilder();
    for(final URI prompt : prompts) {
      builder.addInitialPrompt(prompt);
    }
    builder.setClearDigitBuffer(true);
    builder.setMaxNumberOfDigits(maxNumberOfDigits);
    builder.setMinNumberOfDigits(minNumberOfDigits);
    builder.setFirstDigitTimer(firstDigitTimer);
    builder.setInterDigitTimer(interDigitTimer);
    builder.setEndInputKey(endInputKey);
    final String parameters = builder.build();
    // Create the signal.
    final EventName[] signal = new EventName[1];
    signal[0] = new EventName(PACKAGE_NAME, MgcpEvent.factory("pc").withParm(parameters));
    // Create notification request.
 	requestId = server.generateRequestIdentifier();
 	final NotificationRequest request = new NotificationRequest(this, endPointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    request.setTransactionHandle(server.generateTransactionId());
    // Send the request.
    server.sendEvent(request, this);
    setState(PLAY_COLLECT);
    // Wait for a response.
    try {
     wait();
    } catch(final InterruptedException ignored) { }
    // Make sure the request completed successfully.
    final State currentState = getState();
    if(!currentState.equals(CONNECTED)) {
      throw new EndPointException("There was an error while playing and collecting.");
    }
  }
  
  public synchronized void playRecord(final List<URI> prompts, final long preSpeechTimer, final long recordingLength,
      final String endInputKey) throws EndPointException {
    assertState(CONNECTED);
    // Create the signal parameters.
    final AdvancedAudioParametersBuilder builder = new AdvancedAudioParametersBuilder();
    for(final URI prompt : prompts) {
      builder.addInitialPrompt(prompt);
    }
    builder.setClearDigitBuffer(true);
    builder.setPreSpeechTimer(preSpeechTimer);
    builder.setRecordingLength(recordingLength);
    builder.setEndInputKey(endInputKey);
    final String parameters = builder.build();
    // Create the signal.
    final EventName[] signal = new EventName[1];
    signal[0] = new EventName(PACKAGE_NAME, MgcpEvent.factory("pr").withParm(parameters));
    // Create notification request.
 	requestId = server.generateRequestIdentifier();
 	final NotificationRequest request = new NotificationRequest(this, endPointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    request.setTransactionHandle(server.generateTransactionId());
    // Send the request.
    server.sendEvent(request, this);
    setState(PLAY_RECORD);
    // Wait for a response.
    try {
     wait();
    } catch(final InterruptedException ignored) { }
    // Make sure the request completed successfully.
    final State currentState = getState();
    if(!currentState.equals(CONNECTED)) {
      throw new EndPointException("There was an error while playing and recording.");
    }
  }
  
  private Map<String, String> parseAdvancedAudioParameters(final String input) {
    final Map<String, String> parameters = new HashMap<String, String>();
    final String[] tokens = input.split(" ");
    for(final String token : tokens) {
      final String[] values = token.split("=");
      parameters.put(values[0], values[1]);
    }
    return parameters;
  }

  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent event) {
    final int command = event.getObjectIdentifier();
    switch(command) {
      case Constants.CMD_NOTIFY: {
    	final Notify request = (Notify)event;
    	if(request.getRequestIdentifier().equals(requestId)) {
    	  final EventName[] observedEvents = request.getObservedEvents();
    	  // We are only waiting for "operation completed" or "operation failed" events.
    	  if(observedEvents.length == 1) {
    	    final MgcpEvent response = observedEvents[0].getEventIdentifier();
    	    final Map<String, String> parameters = parseAdvancedAudioParameters(response.getParms());
    	    // Process parameters.
    	    final int returnCode = Integer.parseInt(parameters.get("rc"));
    	    if(returnCode == 100) {
    	      final State currentState = getState();
    	      if(currentState.equals(PLAY_COLLECT)) {
    	        digits = parameters.get("dc");
    	      } else if(currentState.equals(PLAY_RECORD)) {
    	        recordingId = parameters.get("ri");
    	      }
    	    } else {
    	      final StringBuilder buffer = new StringBuilder();
    	      buffer.append(response.getName()).append(" failed with the following error code: ").append(returnCode);
    	      LOGGER.error(buffer.toString());
    	      setState(FAILED);
    	    }
    	  } else {
    	    LOGGER.error("The Notify request does not contain any observed events.");
    	    setState(FAILED);
    	  }
    	  // Wake up waiting thread.
    	  synchronized(this) {
    	    notify();
    	  }
    	}
        break;
      }
      default: {
        return;
      }
    }
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent event) {
    final ReturnCode code = event.getReturnCode();
	if(code.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
	  return;
	} else if(code.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
	  final State currentState = getState();
	  if(currentState.equals(IDLE)) {
		// We are connected.
		final CreateConnectionResponse response = (CreateConnectionResponse)event;
		connectionId = response.getConnectionIdentifier();
		endPointId = response.getSpecificEndpointIdentifier();
	    synchronized(this) {
	      notify();
	    }
	  } else if(currentState.equals(CONNECTED)) {
		// We are disconnected.
		connectionId = null;
		endPointId = null;
	  }
	} else {
	  setState(FAILED);
	  final String error = new StringBuilder().append(code.getValue())
	      .append(" ").append(code.getComment()).toString();
	  LOGGER.error(error);
	  synchronized(this) {
	    notify();
	  }
	}
  }
  
  @Override public void release() {
    final State currentState = getState();
    if(currentState.equals(CONNECTED)) {
      disconnect();
    } else if(currentState.equals(PLAY) || currentState.equals(PLAY_COLLECT) ||
        currentState.equals(PLAY_RECORD)) {
      stop();
      disconnect();
    }
  }
  
  public void stop() {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(PLAY);
    possibleStates.add(PLAY_COLLECT);
    possibleStates.add(PLAY_RECORD);
    assertState(possibleStates);
    // Create the signal.
    final EventName[] signal = new EventName[1];
    signal[0] = new EventName(PACKAGE_NAME, MgcpEvent.factory("es"));
    // Create notification request.
 	requestId = server.generateRequestIdentifier();
 	final NotificationRequest request = new NotificationRequest(this, endPointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    request.setTransactionHandle(server.generateTransactionId());
    // Send the request.
    server.sendEvent(request, this);
  }
}

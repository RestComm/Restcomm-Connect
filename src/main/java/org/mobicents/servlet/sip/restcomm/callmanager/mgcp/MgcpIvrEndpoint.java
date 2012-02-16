package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.au.AdvancedAudioParametersBuilder;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MgcpIvrEndpoint extends FiniteStateMachine implements JainMgcpListener, MgcpEndpoint {
  private static final Logger LOGGER = Logger.getLogger(MgcpIvrEndpoint.class);
  private static final PackageName PACKAGE_NAME = PackageName.factory("AU");
  private static final RequestedEvent[] REQUESTED_EVENTS = new RequestedEvent[2];
  private static final State IDLE = new State("IDLE");
  private static final State PLAY = new State("PLAY");
  private static final State PLAY_COLLECT = new State("PLAY_COLLECT");
  private static final State PLAY_RECORD = new State("PLAY_RECORD");
  private static final State FAILED = new State("FAILED");
  static {
    final RequestedAction[] action = new RequestedAction[] { RequestedAction.NotifyImmediately };
    REQUESTED_EVENTS[0] = new RequestedEvent(new EventName(PACKAGE_NAME, MgcpEvent.factory("oc")), action);
    REQUESTED_EVENTS[1] = new RequestedEvent(new EventName(PACKAGE_NAME, MgcpEvent.factory("of")), action);
    IDLE.addTransition(PLAY);
    IDLE.addTransition(PLAY_COLLECT);
    IDLE.addTransition(PLAY_RECORD);
    IDLE.addTransition(FAILED);
    PLAY.addTransition(IDLE);
    PLAY.addTransition(FAILED);
    PLAY_COLLECT.addTransition(IDLE);
    PLAY_COLLECT.addTransition(FAILED);
    PLAY_RECORD.addTransition(IDLE);
    PLAY_RECORD.addTransition(FAILED);
  }
	  
  private final MgcpServer server;
  private final EndpointIdentifier any;
  private volatile EndpointIdentifier endpointId;
  
  private volatile RequestIdentifier requestId;
  private String digits;
  
  private final List<MgcpIvrEndpointObserver> observers;
  
  public MgcpIvrEndpoint(final MgcpServer server) {
    super(IDLE);
    addState(IDLE);
    addState(PLAY);
    addState(PLAY_COLLECT);
    addState(PLAY_RECORD);
    addState(FAILED);
    server.addNotifyListener(this);
    this.server = server;
    this.any = new EndpointIdentifier("mobicents/ivr/$", server.getDomainName());
    this.observers = Collections.synchronizedList(new ArrayList<MgcpIvrEndpointObserver>());
  }
  
  public void addObserver(final MgcpIvrEndpointObserver observer) {
    observers.add(observer);
  }
  
  private void fireOperationCompleted() {
    for(final MgcpIvrEndpointObserver observer : observers) {
      observer.operationCompleted(this);
    }
  }
  
  private void fireOperationFailed() {
    for(final MgcpIvrEndpointObserver observer : observers) {
      observer.operationFailed(this);
    }
  }
  
  public String getDigits() {
	assertState(IDLE);
    return digits;
  }

  @Override public EndpointIdentifier getId() {
    if(endpointId != null) {
      return endpointId;
    } else {
      return any;
    }
  }
  
  public synchronized void play(final List<URI> announcements, final int iterations) {
	assertState(IDLE);
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
 	final NotificationRequest request = new NotificationRequest(this, endpointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    // Send the request.
    server.sendCommand(request, this);
    setState(PLAY);
  }
  
  public synchronized void playCollect(final List<URI> prompts, final int maxNumberOfDigits, final int minNumberOfDigits,
      final long firstDigitTimer, final long interDigitTimer, final String endInputKey) {
    assertState(IDLE);
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
 	final NotificationRequest request = new NotificationRequest(this, endpointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    // Send the request.
    server.sendCommand(request, this);
    setState(PLAY_COLLECT);
  }
  
  public synchronized void playRecord(final List<URI> prompts, final long preSpeechTimer, final long recordingLength,
      final String endInputKey) {
    assertState(IDLE);
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
 	final NotificationRequest request = new NotificationRequest(this, endpointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    // Send the request.
    server.sendCommand(request, this);
    setState(PLAY_RECORD);
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
  
  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent command) {
    final int commandValue = command.getObjectIdentifier();
    switch(commandValue) {
      case Constants.CMD_NOTIFY: {
    	final Notify request = (Notify)command;
    	if(request.getRequestIdentifier().toString().equals(requestId.toString())) {
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
    	      }
    	      setState(IDLE);
    	      fireOperationCompleted();
    	    } else {
    	      setState(FAILED);
    	      fireOperationFailed();
    	      final StringBuilder buffer = new StringBuilder();
    	      buffer.append(response.getName()).append(" failed with the following error code: ").append(returnCode);
    	      LOGGER.error(buffer.toString());
    	    }
    	  } else {
    		setState(FAILED);
    	    fireOperationFailed();
    	    LOGGER.error("The Notify request does not contain any observed events.");
    	  }
    	  // Notify the media server that the response was properly handled.
    	  final NotifyResponse response = new NotifyResponse(this, ReturnCode.Transaction_Executed_Normally);
    	  response.setTransactionHandle(command.getTransactionHandle());
    	  server.sendResponse(response);
    	}
        break;
      }
      default: {
        return;
      }
    }
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent response) {
    final ReturnCode code = response.getReturnCode();
	if(code.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
	  return;
	} else if(code.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
	  return;
	} else {
	  setState(FAILED);
	  fireOperationFailed();
	  final String error = new StringBuilder().append(code.getValue())
	      .append(" ").append(code.getComment()).toString();
	  LOGGER.error(error);
	}
  }
  
  public void release() {
    final State currentState = getState();
    if(currentState.equals(PLAY) || currentState.equals(PLAY_COLLECT) ||
        currentState.equals(PLAY_RECORD)) {
      stop();
    }
    server.removeNotifyListener(this);
  }
  
  public void removeObserver(final MgcpIvrEndpointObserver observer) {
    observers.remove(observer);
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
 	final NotificationRequest request = new NotificationRequest(this, endpointId, requestId);
    request.setSignalRequests(signal);
    request.setNotifiedEntity(server.getCallAgent());
    request.setRequestedEvents(REQUESTED_EVENTS);
    // Send the request.
    server.sendCommand(request, this);
  }

  @Override public synchronized void updateId(final EndpointIdentifier endpointId) {
    this.endpointId = endpointId;
  }
}

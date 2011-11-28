package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class ConferenceEndPoint extends FSM implements EndPoint, JainMgcpListener {
  private static final Logger LOGGER = Logger.getLogger(ConferenceEndPoint.class);
  private static final String PREFIX = "/mobicents/media/cnf/";
  // Conference end point states.
  private static final State IDLE = new State("idle");
  private static final State STARTED = new State("started");
  private static final State ENDED = new State("ended");
  static {
    IDLE.addTransition(STARTED);
    STARTED.addTransition(ENDED);
  }
  
  private final MgcpServer server;
  private final MgcpMediaSession mediaSession;
  private final List<EndpointIdentifier> participantEndpoints;
  private final ReentrantLock participantEndpointsLock;
  
  private ConnectionIdentifier connectionId;
  private EndpointIdentifier endPointId;
  
  public ConferenceEndPoint(final MgcpServer server, final MgcpMediaSession mediaSession) {
    super(IDLE);
    this.server = server;
    this.mediaSession = mediaSession;
    this.participantEndpoints = new ArrayList<EndpointIdentifier>();
    this.participantEndpointsLock = new ReentrantLock();
  }
  
  public void join(final EndpointIdentifier otherEndPoint, final ConnectionMode connectionMode) throws EndPointException {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(IDLE);
    possibleStates.add(STARTED);
    assertState(possibleStates);
    EndpointIdentifier endPointId = null;
    final State currentState = getState();
    if(currentState.equals(IDLE)) {
      final String localEndPointName = new StringBuilder().append(PREFIX).append("$").toString();
      endPointId = new EndpointIdentifier(localEndPointName, server.getDomainName());
    } else if(currentState.equals(STARTED)) {
      endPointId = this.endPointId;
    }
    try {
      final CreateConnection request = new CreateConnection(this, mediaSession.getCallId(), endPointId, connectionMode);
      request.setNotifiedEntity(server.getCallAgent());
      request.setSecondEndpointIdentifier(otherEndPoint);
      request.setTransactionHandle(server.generateTransactionId());
      server.sendEvent(request, this);
    } catch(final Exception exception) {
      throw new EndPointException(exception);
    }
  }
  
  public void leave(final EndpointIdentifier otherEndPoint) {
    assertState(STARTED);
    // Try to delete the connection to the conference end point.
    final DeleteConnection request = new DeleteConnection(this, mediaSession.getCallId(), endPointId, connectionId);
    request.setEndpointIdentifier(otherEndPoint);
    request.setTransactionHandle(server.generateTransactionId());
  }

  @Override public EndpointIdentifier getEndPointId() {
    return endPointId;
  }

  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent event) {
    // Nothing to do.
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent event) {
    final ReturnCode code = event.getReturnCode();
	if(code.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
	  return;
	} else if(code.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
	  final State currentState = getState();
	  if(currentState.equals(IDLE)) {
	    
	  } else if(currentState.equals(STARTED)) {
	    
	  }
	} else {
	  
	}
  }
  
  @Override public void release() {
    final State currentState = getState();
    if(currentState.equals(STARTED)) {
      
    }
  }
}

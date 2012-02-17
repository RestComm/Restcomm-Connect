package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

@NotThreadSafe public class MgcpLink extends FiniteStateMachine implements JainMgcpListener {
  private static final Logger LOGGER = Logger.getLogger(MgcpLink.class);
  private static final State CREATED = new State("CREATED");
  private static final State CONNECTED = new State("CONNECTED");
  private static final State DISCONNECTED = new State("DISCONNECTED");
  private static final State FAILED = new State("FAILED");
  static {
    CREATED.addTransition(CONNECTED);
    CREATED.addTransition(FAILED);
    CONNECTED.addTransition(DISCONNECTED);
    CONNECTED.addTransition(FAILED);
  }
  
  private final MgcpServer server;
  private final MgcpSession session;
  private final MgcpEndpoint firstEndpoint;
  private final MgcpEndpoint secondEndpoint;
  
  private ConnectionIdentifier firstConnectionId;
  private ConnectionIdentifier secondConnectionId;
  
  private final List<MgcpLinkObserver> observers;
  
  public MgcpLink(final MgcpServer server, final MgcpSession session, final MgcpEndpoint firstEndpoint,
      final MgcpEndpoint secondEndpoint) {
    super(CREATED);
    addState(CREATED);
    addState(CONNECTED);
    addState(DISCONNECTED);
    addState(FAILED);
    this.server = server;
    this.session = session;
    this.firstEndpoint = firstEndpoint;
    this.secondEndpoint = secondEndpoint;
    this.observers = Collections.synchronizedList(new ArrayList<MgcpLinkObserver>());
  }
  
  public void addObserver(final MgcpLinkObserver observer) {
    observers.add(observer);
  }
  
  public void connect(final ConnectionMode connectionMode) throws MgcpLinkException {
    assertState(CREATED);
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final CreateConnection crcx = new CreateConnection(this, callId, firstEndpoint.getId(), connectionMode);
      crcx.setNotifiedEntity(server.getCallAgent());
      try {
	    crcx.setSecondEndpointIdentifier(secondEndpoint.getId());
	  } catch(final ConflictingParameterException ignored) { }
      server.sendCommand(crcx, this);
    } catch(final MgcpServerException exception) {
      setState(FAILED);
      fireFailed();
      throw new MgcpLinkException(exception);
    }
  }
  
  public void disconnect() throws MgcpLinkException {
    assertState(CONNECTED);
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      DeleteConnection dlcx = new DeleteConnection(this, callId, firstEndpoint.getId(), firstConnectionId);
      server.sendCommand(dlcx, this);
      dlcx = new DeleteConnection(this, callId, secondEndpoint.getId(), secondConnectionId);
      server.sendCommand(dlcx, this);
      setState(DISCONNECTED);
      fireDisconnected();
    } catch(final MgcpServerException exception) {
      setState(FAILED);
      fireFailed();
      throw new MgcpLinkException(exception);
    }
  }
  
  private void fireConnected() {
    for(final MgcpLinkObserver observer : observers) {
      observer.connected(this);
    }
  }
  
  private void fireDisconnected() {
    for(final MgcpLinkObserver observer : observers) {
      observer.disconnected(this);
    }
  }
  
  private void fireFailed() {
    for(final MgcpLinkObserver observer : observers) {
      observer.failed(this);
    }
  }
  
  public void modifyFirstConnection(final ConnectionMode connectionMode) {
    modify(firstConnectionId, firstEndpoint.getId(), connectionMode);
  }
  
  public void modifySecondConnection(final ConnectionMode connectionMode) {
    modify(secondConnectionId, secondEndpoint.getId(), connectionMode);
  }
  
  private void modify(final ConnectionIdentifier connectionId, final EndpointIdentifier endpointId,
      final ConnectionMode connectionMode) throws MgcpLinkException {
    assertState(CONNECTED);
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final ModifyConnection mdcx = new ModifyConnection(this, callId, endpointId, connectionId);
      mdcx.setMode(connectionMode);
      server.sendCommand(mdcx, this);
    } catch(final MgcpServerException exception) {
      throw new MgcpLinkException(exception);
    }
  }

  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent ignored) {
    // Nothing to do.
  }
  
  private void created(final CreateConnectionResponse response) {
    firstConnectionId = response.getConnectionIdentifier();
    firstEndpoint.updateId(response.getSpecificEndpointIdentifier());
    secondConnectionId = response.getSecondConnectionIdentifier();
    secondEndpoint.updateId(response.getSecondEndpointIdentifier());
    setState(CONNECTED);
    fireConnected();
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent response) {
    final ReturnCode returnCode = response.getReturnCode();
    if(returnCode.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
  	  return;
  	} else if(returnCode.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
  	  final State currentState = getState();
  	  if(currentState.equals(CREATED)) {
  	    created((CreateConnectionResponse)response);
  	  }
  	} else {
	  setState(FAILED);
	  fireFailed();
	  final String error = new StringBuilder().append(returnCode.getValue())
	      .append(" ").append(returnCode.getComment()).toString();
	  LOGGER.error(error);
  	}
  }
  
  public void removeObserver(final MgcpLinkObserver observer) {
    observers.remove(observer);
  }
}

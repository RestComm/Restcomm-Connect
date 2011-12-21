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
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;

public final class MgcpConnection extends FiniteStateMachine implements JainMgcpListener {
  private static final Logger LOGGER = Logger.getLogger(MgcpConnection.class);
  private static final State CREATED = new State("CREATED");
  private static final State HALF_OPEN = new State("HALF_OPEN");
  private static final State OPEN = new State("OPEN");
  private static final State DISCONNECTED = new State("DISCONNECTED");
  private static final State FAILED = new State("FAILED");
  static {
    CREATED.addTransition(HALF_OPEN);
    CREATED.addTransition(OPEN);
    CREATED.addTransition(FAILED);
    HALF_OPEN.addTransition(OPEN);
    HALF_OPEN.addTransition(DISCONNECTED);
    HALF_OPEN.addTransition(FAILED);
    OPEN.addTransition(DISCONNECTED);
    OPEN.addTransition(FAILED);
  }
  
  private final MgcpServer server;
  private final MgcpSession session;
  private final MgcpEndpoint endpoint;
  private ConnectionIdentifier connectionId;
  private ConnectionDescriptor localDescriptor;
  private ConnectionDescriptor remoteDescriptor;
  
  private final List<MgcpConnectionObserver> observers;
  
  public MgcpConnection(final MgcpServer server, final MgcpSession session, final MgcpEndpoint endpoint) {
    super(CREATED);
    addState(CREATED);
    addState(HALF_OPEN);
    addState(OPEN);
    addState(DISCONNECTED);
    addState(FAILED);
    this.server = server;
    this.session = session;
    this.endpoint = endpoint;
    this.observers = Collections.synchronizedList(new ArrayList<MgcpConnectionObserver>());
  }
  
  public MgcpConnection(final MgcpServer server, final MgcpSession session, final MgcpEndpoint endpoint,
      final ConnectionDescriptor remoteDescriptor) {
    this(server, session, endpoint);
    this.remoteDescriptor = remoteDescriptor;
  }
  
  public void addObserver(final MgcpConnectionObserver observer) {
    observers.add(observer);
  }
  
  public void connect(final ConnectionMode connectionMode) throws MgcpConnectionException {
    assertState(CREATED);
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final CreateConnection crcx = new CreateConnection(this, callId, endpoint.getId(), connectionMode);
      crcx.setNotifiedEntity(server.getCallAgent());
      if(remoteDescriptor != null) {
        try {
		  crcx.setRemoteConnectionDescriptor(remoteDescriptor);
		} catch(final ConflictingParameterException ignored) { }
      }
      server.sendCommand(crcx, this);
    } catch(final MgcpServerException exception) {
      setState(FAILED);
      fireFailed();
      throw new MgcpConnectionException(exception);
    }
  }
  
  public void disconnect() throws MgcpConnectionException {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(HALF_OPEN);
    possibleStates.add(OPEN);
    assertState(possibleStates);
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final DeleteConnection dlcx = new DeleteConnection(this, callId, endpoint.getId(), connectionId);
      server.sendCommand(dlcx, this);
      setState(DISCONNECTED);
      fireDisconnected();
    } catch(final MgcpServerException exception) {
      setState(FAILED);
      fireFailed();
      throw new MgcpConnectionException(exception);
    }
  }
  
  private void fireDisconnected() {
    for(final MgcpConnectionObserver observer : observers) {
      observer.disconnected(this);
    }
  }
  
  private void fireFailed() {
    for(final MgcpConnectionObserver observer : observers) {
      observer.failed(this);
    }
  }
  
  private void fireHalfOpen() {
    for(final MgcpConnectionObserver observer : observers) {
      observer.halfOpen(this);
    }
  }
  
  private void fireOpen() {
    for(final MgcpConnectionObserver observer : observers) {
      observer.open(this);
    }
  }
  
  public ConnectionIdentifier getConnectionIdentifier() {
    return connectionId;
  }
  
  public MgcpEndpoint getEndpoint() {
    return endpoint;
  }
  
  public ConnectionDescriptor getLocalDescriptor() {
    return localDescriptor;
  }
  
  public ConnectionDescriptor getRemoteDescriptor() {
    return remoteDescriptor;
  }
  
  public void modify(final ConnectionMode connectionMode) throws MgcpConnectionException {
    modify(null, connectionMode);
  }
  
  public void modify(final ConnectionDescriptor remoteDescriptor) {
    modify(remoteDescriptor, null);
  }
  
  public void modify(final ConnectionDescriptor remoteDescriptor, final ConnectionMode connectionMode) throws MgcpConnectionException {
    final List<State> possibleStates = new ArrayList<State>();
    possibleStates.add(HALF_OPEN);
    possibleStates.add(OPEN);
    assertState(possibleStates);
    // Make sure there is something to modify.
    if(remoteDescriptor == null && connectionMode == null) {
      return;
    }
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final ModifyConnection mdcx = new ModifyConnection(this, callId, endpoint.getId(), connectionId);
      // Modify the remote session description.
      if(remoteDescriptor != null) {
        mdcx.setRemoteConnectionDescriptor(remoteDescriptor);
      }
      // Modify the connection mode.
      if(connectionMode != null) {
        mdcx.setMode(connectionMode);
      }
      server.sendCommand(mdcx, this);
    } catch(final MgcpServerException exception) {
      throw new MgcpConnectionException(exception);
    }
  }
  
  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent ignored) {
    // Nothing to do.
  }
  
  private void created(final CreateConnectionResponse response) {
    connectionId = response.getConnectionIdentifier();
    localDescriptor = response.getLocalConnectionDescriptor();
    endpoint.updateId(response.getSpecificEndpointIdentifier());
    if(remoteDescriptor != null) {
      setState(OPEN);
      fireOpen();
    } else {
      setState(HALF_OPEN);
      fireHalfOpen();
    }
  }
  
  private void halfOpen(final ModifyConnectionResponse response) {
    localDescriptor = response.getLocalConnectionDescriptor();
    setState(OPEN);
    fireOpen();
  }
  
  private void open(final ModifyConnectionResponse response) {
    localDescriptor = response.getLocalConnectionDescriptor();
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent response) {
    final ReturnCode returnCode = response.getReturnCode();
    if(returnCode.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
  	  return;
  	} else if(returnCode.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
  	  final State currentState = getState();
  	  if(currentState.equals(CREATED)) {
  	    created((CreateConnectionResponse)response);
  	  } else if(currentState.equals(HALF_OPEN)) {
  	    halfOpen((ModifyConnectionResponse)response);
  	  } else if(currentState.equals(OPEN)) {
        open((ModifyConnectionResponse)response);
  	  }
  	} else {
  	  setState(FAILED);
  	  fireFailed();
  	  final String error = new StringBuilder().append(returnCode.getValue())
  	      .append(" ").append(returnCode.getComment()).toString();
  	  LOGGER.error(error);
  	}
  }
  
  public void removeObserver(final MgcpConnectionObserver observer) {
    observers.remove(observer);
  }
}

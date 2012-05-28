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
package org.mobicents.servlet.sip.restcomm.mgcp;

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
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
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
  private ConnectionDescriptor firstConnectionDescriptor;
  private ConnectionDescriptor secondConnectionDescriptor;
  
  private final List<MgcpLinkObserver> observers;
  
  private volatile boolean modifyingFirstConnection;
  private volatile boolean modifyingSecondConnection;
  
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
    this.modifyingFirstConnection = false;
    this.modifyingSecondConnection = false;
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
  
  private void fireModified() {
    for(final MgcpLinkObserver observer : observers) {
      observer.modified(this);
    }
  }
  
  public ConnectionDescriptor getFirstConnectionDescriptor() {
    return firstConnectionDescriptor;
  }
  
  public ConnectionDescriptor getSecondConnectionDescriptor() {
    return secondConnectionDescriptor;
  }
  
  public void modifyFirstConnection(final ConnectionMode connectionMode, final ConnectionDescriptor remoteDescriptor) throws MgcpLinkException {
    if(!modifyingFirstConnection) {
      modify(firstConnectionId, firstEndpoint.getId(), connectionMode, remoteDescriptor);
      modifyingFirstConnection = true;
    } else {
      throw new MgcpLinkException("Invalid state this connection is already being modified.");
    }
  }
  
  public void modifySecondConnection(final ConnectionMode connectionMode, final ConnectionDescriptor remoteDescriptor) throws MgcpLinkException {
    if(!modifyingSecondConnection) {
      modify(secondConnectionId, secondEndpoint.getId(), connectionMode, remoteDescriptor);
      modifyingSecondConnection = true;
    } else {
      throw new MgcpLinkException("Invalid state this connection is already being modified.");
    }
  }
  
  private void modify(final ConnectionIdentifier connectionId, final EndpointIdentifier endpointId,
      final ConnectionMode connectionMode, final ConnectionDescriptor remoteDescriptor) throws MgcpLinkException {
    assertState(CONNECTED);
    // Make sure there is something to modify.
    if(remoteDescriptor == null && connectionMode == null) {
      return;
    }
    try {
      final CallIdentifier callId = new CallIdentifier(Integer.toString(session.getId()));
      final ModifyConnection mdcx = new ModifyConnection(this, callId, endpointId, connectionId);
      if(connectionMode != null) {
        mdcx.setMode(connectionMode);
      }
      if(remoteDescriptor != null) {
        mdcx.setRemoteConnectionDescriptor(remoteDescriptor);
      }
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
    firstConnectionDescriptor = response.getLocalConnectionDescriptor();
    setState(CONNECTED);
    fireConnected();
  }
  
  private void modified(final ModifyConnectionResponse response) {
	if(modifyingFirstConnection) {
	  firstConnectionDescriptor = response.getLocalConnectionDescriptor();
	  modifyingFirstConnection = false;
	} else if(modifyingSecondConnection) {
	  secondConnectionDescriptor = response.getLocalConnectionDescriptor();
	  modifyingSecondConnection = false;
	}
    fireModified();
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent response) {
    final ReturnCode returnCode = response.getReturnCode();
    if(returnCode.getValue() == ReturnCode.TRANSACTION_BEING_EXECUTED) {
  	  return;
  	} else if(returnCode.getValue() == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
  	  final State currentState = getState();
  	  if(currentState.equals(CREATED)) {
  	    created((CreateConnectionResponse)response);
  	  } else if(currentState.equals(CONNECTED)) {
  	    if(response instanceof ModifyConnectionResponse) {
  	      modified((ModifyConnectionResponse)response);
  	    }
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

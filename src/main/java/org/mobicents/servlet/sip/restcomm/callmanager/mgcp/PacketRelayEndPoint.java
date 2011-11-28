package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class PacketRelayEndPoint extends FSM implements EndPoint, JainMgcpListener {
  private static final Logger LOGGER = Logger.getLogger(PacketRelayEndPoint.class);
  private static final String PREFIX = "/mobicents/media/packetrelay/";
  // Packet relay end point states.
  private static final State IDLE = new State("idle");
  private static final State CONNECTED = new State("connected");
  private static final State DISCONNECTED = new State("disconnected");
  private static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(CONNECTED);
    IDLE.addTransition(FAILED);
    CONNECTED.addTransition(DISCONNECTED);
    CONNECTED.addTransition(FAILED);
  }
  
  private final MgcpServer server;
  private final MgcpMediaSession mediaSession;
  
  private ConnectionDescriptor connectionDescriptor;
  private ConnectionIdentifier connectionId;
  private EndpointIdentifier endPointId;
  
  public PacketRelayEndPoint(final MgcpServer server, final MgcpMediaSession mediaSession) {
    super(IDLE);
    this.server = server;
    this.mediaSession = mediaSession;
  }
  
  public void connect(final String offer) throws EndPointException {
	assertState(IDLE);
	// Try to create a new connection to a packet relay end point.
	final String localEndPointName = new StringBuilder().append(PREFIX).append("$").toString();
	final EndpointIdentifier endPointId = new EndpointIdentifier(localEndPointName, server.getDomainName());
    try {
      final CreateConnection request = new CreateConnection(this, mediaSession.getCallId(), endPointId, ConnectionMode.SendRecv);
      request.setNotifiedEntity(server.getCallAgent());
      final ConnectionDescriptor descriptor = new ConnectionDescriptor(offer);
      request.setRemoteConnectionDescriptor(descriptor);
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
        buffer.append("Timed out while establishing a connection to a packet relay end point.\n");
      } else {
        buffer.append("Could not establish a connection to a packet relay end point using the following session description: \n")
            .append(offer);
      }
      throw new EndPointException(buffer.toString());
    }
  }
  
  public void disconnect() {
    assertState(CONNECTED);
    // Try to delete the connection to the packet relay end point.
    final DeleteConnection request = new DeleteConnection(this, mediaSession.getCallId(), endPointId, connectionId);
    request.setTransactionHandle(server.generateTransactionId());
    server.sendEvent(request, this);
    setState(DISCONNECTED);
  }
  
  public ConnectionDescriptor getConnectionDescriptor() {
    return connectionDescriptor;
  }
  
  public ConnectionIdentifier getConnectionId() {
    return connectionId;
  }
  
  public EndpointIdentifier getEndPointId() {
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
	    // We are connected.
	    final CreateConnectionResponse response = (CreateConnectionResponse)event;
	    connectionDescriptor = response.getLocalConnectionDescriptor();
	    connectionId = response.getConnectionIdentifier();
	    endPointId = response.getSpecificEndpointIdentifier();
	    setState(CONNECTED);
	    synchronized(this) {
	      notify();
	    }
	  } else if(currentState.equals(CONNECTED)) {
	    // We are disconnected.
		connectionDescriptor = null;
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
      mediaSession.destoryEndPoint(this);
    }
  }
}

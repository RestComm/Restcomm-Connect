package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

public final class MgcpConference implements Conference, MgcpConnectionObserver {
  private final String name;
  private final MgcpSession session;
  private final MgcpConferenceEndpoint endpoint;
  private final Map<MgcpConnection, MgcpConnection>connections;

  public MgcpConference(final String name, final MgcpServer server) {
    super();
    this.name = name;
    this.session = server.createMediaSession();
    this.endpoint = session.getConferenceEndpoint();
    this.connections = new ConcurrentHashMap<MgcpConnection, MgcpConnection>();
  }
  
  public synchronized void addCall(final Call call) throws InterruptedException {
	final MgcpCall mgcpCall = (MgcpCall)call;
    final MgcpEndpoint remoteEndpoint = mgcpCall.getJoinableEndpoint();
    final MgcpConnection outboundConnection = session.createConnection(remoteEndpoint);
    outboundConnection.addObserver(this);
    outboundConnection.connect(ConnectionMode.Confrnce);
    wait();
  }
	
  @Override public String getName() {
    return name;
  }

  @Override public synchronized void halfOpen(final MgcpConnection connection) {
    final MgcpConnection inboundConnection = session.createConnection(endpoint, connection.getLocalDescriptor());
    inboundConnection.addObserver(this);
    inboundConnection.connect(ConnectionMode.Confrnce);
  }

  @Override public synchronized void open(final MgcpConnection connection) {
    
  }

  @Override public synchronized void disconnected(final MgcpConnection connection) {
    
  }

  @Override public synchronized void failed(final MgcpConnection connection) {
    
  }

  @Override public synchronized void modified(final MgcpConnection connection) {
    // Nothing to do.
  }
}

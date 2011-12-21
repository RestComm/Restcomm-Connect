package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.util.ArrayList;
import java.util.List;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpSession {
  private final int id;
  private final MgcpServer server;
  
  private final List<MgcpConnection> connections;
  private final List<MgcpLink> links;
  
  public MgcpSession(final int id, final MgcpServer server) {
    super();
    this.id = id;
    this.server = server;
    this.connections = new ArrayList<MgcpConnection>();
    this.links = new ArrayList<MgcpLink>();
  }
  
  public MgcpConnection createConnection(final MgcpEndpoint endpoint) {
    final MgcpConnection connection = new MgcpConnection(server, this, endpoint);
    connections.add(connection);
    return connection;
  }
  
  public MgcpConnection createConnection(final MgcpEndpoint endpoint, final ConnectionDescriptor remoteDescriptor) {
    final MgcpConnection connection = new MgcpConnection(server, this, endpoint, remoteDescriptor);
    connections.add(connection);
    return connection;
  }
  
  public MgcpLink createLink(final MgcpEndpoint firstEndpoint, final MgcpEndpoint secondEndpoint) {
    final MgcpLink link = new MgcpLink(server, this, firstEndpoint, secondEndpoint);
    links.add(link);
    return link;
  }
  
  public void destroyConnection(final MgcpConnection connection) {
    connections.remove(connection);
    connection.disconnect();
  }
  
  private void destroyConnections() {
    for(final MgcpConnection connection : connections) {
      connection.disconnect();
    }
    connections.clear();
  }
  
  public void destroyLink(final MgcpLink link) {
    links.remove(link);
    link.disconnect();
  }
  
  private void destroyLinks() {
    for(final MgcpLink link : links) {
      link.disconnect();
    }
    links.clear();
  }
  
  public MgcpConferenceEndpoint getConferenceEndpoint() {
    return new MgcpConferenceEndpoint(server);
  }
  
  public int getId() {
    return id;
  }
  
  public MgcpIvrEndpoint getIvrEndpoint() {
    return new MgcpIvrEndpoint(server);
  }
  
  public MgcpPacketRelayEndpoint getPacketRelayEndpoint() {
    return new MgcpPacketRelayEndpoint(server);
  }

  public void release() {
    destroyConnections();
    destroyLinks();
  }
}

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
package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class MgcpSession {
  private final int id;
  private final MgcpServer server;
  
  private final List<MgcpConnection> connections;
  private final List<MgcpEndpoint> endpoints;
  
  public MgcpSession(final int id, final MgcpServer server) {
    super();
    this.id = id;
    this.server = server;
    this.connections = new CopyOnWriteArrayList<MgcpConnection>();
    this.endpoints = new CopyOnWriteArrayList<MgcpEndpoint>();
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
  
  public void destroyConnection(final MgcpConnection connection) {
    if(connections.remove(connection)) {
      connection.disconnect();
    }
  }
  
  private void destroyConnections() {
    for(final MgcpConnection connection : connections) {
      connection.disconnect();
    }
    connections.clear();
  }
  
  public MgcpConferenceEndpoint getConferenceEndpoint() {
    final MgcpConferenceEndpoint endpoint = new MgcpConferenceEndpoint(server);
    endpoints.add(endpoint);
    return endpoint;
  }
  
  public int getId() {
    return id;
  }
  
  public MgcpIvrEndpoint getIvrEndpoint() {
	final MgcpIvrEndpoint endpoint = new MgcpIvrEndpoint(server);
	endpoints.add(endpoint);
    return endpoint;
  }
  
  public MgcpPacketRelayEndpoint getPacketRelayEndpoint() {
	final MgcpPacketRelayEndpoint endpoint = new MgcpPacketRelayEndpoint(server);
	endpoints.add(endpoint);
    return endpoint;
  }

  public void release() {
    destroyConnections();
    releaseEndpoints();
  }
  
  private void releaseEndpoints() {
    for(final MgcpEndpoint endpoint : endpoints) {
      endpoint.release();
    }
    endpoints.clear();
  }
}

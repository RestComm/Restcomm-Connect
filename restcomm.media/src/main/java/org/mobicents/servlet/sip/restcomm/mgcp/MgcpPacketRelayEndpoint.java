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

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MgcpPacketRelayEndpoint implements MgcpEndpoint {
  private final EndpointIdentifier any;
  private volatile EndpointIdentifier endpointId;
  
  public MgcpPacketRelayEndpoint(final MgcpServer server) {
    super();
    this.any = new EndpointIdentifier("mobicents/relay/$", server.getDomainName());
  }
  
  @Override public EndpointIdentifier getId() {
    if(endpointId != null) {
      return endpointId;
    } else {
      return any;
    }
  }
  
  @Override public void release() {
    // Nothing to do.
  }

  @Override public synchronized void updateId(final EndpointIdentifier endpointId) {
    this.endpointId = endpointId;
  }
}

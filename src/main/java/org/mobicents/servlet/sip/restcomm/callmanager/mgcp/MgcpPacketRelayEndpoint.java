package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

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

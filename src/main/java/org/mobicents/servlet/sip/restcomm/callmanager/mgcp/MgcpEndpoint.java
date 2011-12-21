package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

public interface MgcpEndpoint {
  public EndpointIdentifier getId();
  public void updateId(EndpointIdentifier endpointId);
  public void release();
}

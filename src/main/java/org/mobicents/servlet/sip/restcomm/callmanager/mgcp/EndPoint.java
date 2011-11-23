package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

public interface EndPoint {
  public EndpointIdentifier getEndPointId();
  public void release();
}

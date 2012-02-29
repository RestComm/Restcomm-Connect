package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;

public interface MgcpLinkObserver {
  public void connected(MgcpLink link);
  public void disconnected(MgcpLink link);
  public void failed(MgcpLink link);
  public void modified(ConnectionDescriptor descriptor, MgcpLink link);
}

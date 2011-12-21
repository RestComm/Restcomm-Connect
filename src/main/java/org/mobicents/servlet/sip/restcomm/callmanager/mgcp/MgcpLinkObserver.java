package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public interface MgcpLinkObserver {
  public void connected(MgcpLink link);
  public void disconnected(MgcpLink link);
  public void failed(MgcpLink link);
}

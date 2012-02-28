package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public interface MgcpConnectionObserver {
  public void halfOpen(MgcpConnection connection);
  public void open(MgcpConnection connection);
  public void disconnected(MgcpConnection connection);
  public void failed(MgcpConnection connection);
  public void modified(MgcpConnection connection);
}

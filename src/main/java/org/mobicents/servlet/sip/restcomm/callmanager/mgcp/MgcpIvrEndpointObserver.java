package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

public interface MgcpIvrEndpointObserver {
  public void operationCompleted(MgcpIvrEndpoint endpoint);
  public void operationFailed(MgcpIvrEndpoint endpoint);
}

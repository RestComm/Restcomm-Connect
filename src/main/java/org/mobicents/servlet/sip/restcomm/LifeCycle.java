package org.mobicents.servlet.sip.restcomm;

public interface LifeCycle {
  public void initialize() throws RuntimeException;
  public void shutdown();
}

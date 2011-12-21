package org.mobicents.servlet.sip.restcomm;

public interface LifeCycle {
  public void start() throws RuntimeException;
  public void shutdown();
}

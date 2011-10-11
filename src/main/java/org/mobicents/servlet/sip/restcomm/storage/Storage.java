package org.mobicents.servlet.sip.restcomm.storage;

import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.LifeCycle;

public interface Storage extends Configurable, LifeCycle {
  public String getHttpUri();
  public String getPath();
  public byte[] readObject(String path);
  public void removeObject(String path);
  public void writeObject(String path, byte[] object);
}

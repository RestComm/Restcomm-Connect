package org.mobicents.servlet.sip.restcomm.dao;

import org.mobicents.servlet.sip.restcomm.SandBox;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface SandBoxDao {
  public void addSandBox(SandBox sandBox);
  public SandBox getSandBox(Sid accountSid);
  public void removeSandBox(Sid accountSid);
  public void updateSandBox(SandBox sandBox);
}

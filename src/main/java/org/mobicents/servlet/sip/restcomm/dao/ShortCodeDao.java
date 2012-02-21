package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.ShortCode;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface ShortCodeDao {
  public void addShortCode(ShortCode shortCode);
  public ShortCode getShortCode(Sid sid);
  public List<ShortCode> getShortCodes(Sid accountSid);
  public void removeShortCode(Sid sid);
  public void removeShortCodes(Sid accountSid);
  public void updateShortCode(ShortCode shortCode);
}

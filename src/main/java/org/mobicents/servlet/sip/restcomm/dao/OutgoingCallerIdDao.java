package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.OutgoingCallerId;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface OutgoingCallerIdDao {
  public void addOutgoingCallerId(OutgoingCallerId outgoingCallerId);
  public OutgoingCallerId getOutgoingCallerId(Sid sid);
  public List<OutgoingCallerId> getOutgoingCallerIds(Sid accountSid);
  public void removeOutgoingCallerId(Sid sid);
  public void removeOutgoingCallerIds(Sid accountSid);
  public void updateOutgoingCallerId(OutgoingCallerId outgoingCallerId);
}

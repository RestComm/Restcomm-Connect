package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface IncomingPhoneNumbersDao {
  public void addIncomingPhoneNumber(IncomingPhoneNumber incomingPhoneNumber);
  public IncomingPhoneNumber getIncomingPhoneNumber(Sid sid);
  public List<IncomingPhoneNumber> getIncomingPhoneNumbers(Sid accountSid);
  public IncomingPhoneNumber getIncomingPhoneNumber(String phoneNumber);
  public void removeIncomingPhoneNumber(Sid sid);
  public void removeIncomingPhoneNumbers(Sid accountSid);
  public void updateIncomingPhoneNumber(IncomingPhoneNumber incomingPhoneNumber);
}

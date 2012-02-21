package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;

public interface SmsMessageDao {
  public void addSmsMessage(SmsMessage smsMessage);
  public SmsMessage getSmsMessage(Sid sid);
  public List<SmsMessage> getSmsMessages(Sid accountSid);
  public void removeSmsMessage(Sid sid);
  public void removeSmsMessages(Sid accountSid);
}

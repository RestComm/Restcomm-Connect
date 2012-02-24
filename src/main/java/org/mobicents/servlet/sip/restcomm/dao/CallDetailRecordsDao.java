package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface CallDetailRecordsDao {
  public void addCallDetailRecord(CallDetailRecord cdr);
  public CallDetailRecord getCallDetailRecord(Sid sid);
  public List<CallDetailRecord> getCallDetailRecords(Sid accountSid);
  public List<CallDetailRecord> getCallDetailRecordsByRecipient(String recipient);
  public List<CallDetailRecord> getCallDetailRecordsBySender(String sender);
  public List<CallDetailRecord> getCallDetailRecordsByStatus(String status);
  public List<CallDetailRecord> getCallDetailRecordsByStartTime(DateTime startTime);
  public List<CallDetailRecord> getCallDetailRecordsByParentCall(Sid parentCallSid);
  public void removeCallDetailRecord(Sid sid);
  public void removeCallDetailRecords(Sid accountSid);
  public void updateCallDetailRecord(CallDetailRecord cdr);
}

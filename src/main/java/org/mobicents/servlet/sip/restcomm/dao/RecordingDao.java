package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.Recording;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface RecordingDao {
  public void addRecording(Recording recording);
  public Recording getRecording(Sid sid);
  public Recording getRecordingByCall(Sid callSid);
  public List<Recording> getRecordings(Sid accountSid);
  public void removeRecording(Sid sid);
  public void removeRecordings(Sid accountSid);
}

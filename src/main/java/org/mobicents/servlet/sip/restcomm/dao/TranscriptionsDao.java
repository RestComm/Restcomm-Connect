package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.Transcription;

public interface TranscriptionsDao {
  public void addTranscription(Transcription transcription);
  public Transcription getTranscription(Sid sid);
  public Transcription getTranscriptionByRecording(Sid recordingSid);
  public List<Transcription> getTranscriptions(Sid accountSid);
  public void removeTranscription(Sid sid);
  public void removeTranscriptions(Sid accountSid);
}

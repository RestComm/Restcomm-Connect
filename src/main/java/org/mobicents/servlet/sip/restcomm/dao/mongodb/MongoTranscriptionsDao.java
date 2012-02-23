package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.Transcription;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoTranscriptionsDao implements TranscriptionsDao {
  private final DBCollection collection;
  
  public MongoTranscriptionsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_transcriptions");
  }
  
  @Override public void addTranscription(final Transcription transcription) {
    
  }

  @Override public Transcription getTranscription(final Sid sid) {
    return null;
  }

  @Override public Transcription getTranscriptionByRecording(final Sid recordingSid) {
    return null;
  }

  @Override public List<Transcription> getTranscriptions(final Sid accountSid) {
    return null;
  }

  @Override public void removeTranscription(final Sid sid) {
    
  }

  @Override	public void removeTranscriptions(final Sid accountSid) {
    
  }
  
  private DBObject toDbObject(final Transcription transcription) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", transcription.getSid().toString());
    object.put("date_created", transcription.getDateCreated().toDate());
    object.put("date_updated", transcription.getDateUpdated().toDate());
    object.put("account_sid", transcription.getAccountSid().toString());
    object.put("status", transcription.getStatus());
    object.put("recording_sid", transcription.getRecordingSid().toString());
    object.put("duration", transcription.getDuration());
    object.put("transcription_text", transcription.getTranscriptionText());
    object.put("price", transcription.getPrice().toString());
    object.put("uri", transcription.getUri().toString());
    return object;
  }
  
  private Transcription toTranscription(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String status = (String)object.get("status");
    final Sid recordingSid = new Sid((String)object.get("recording_sid"));
    final Integer duration = (Integer)object.get("duration");
    final String transcriptionText = (String)object.get("transcription_text");
    final BigDecimal price = new BigDecimal((String)object.get("price"));
    final URI uri = URI.create((String)object.get("uri"));
    return new Transcription(sid, dateCreated, dateUpdated, accountSid, status, recordingSid, duration,
        transcriptionText, price, uri);
  }
}

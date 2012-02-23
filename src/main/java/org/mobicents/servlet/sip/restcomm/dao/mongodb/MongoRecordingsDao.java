package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Recording;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoRecordingsDao implements RecordingsDao {
  private final DBCollection collection;

  public MongoRecordingsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_recordings");
  }
  
  @Override public void addRecording(final Recording recording) {
    
  }

  @Override public Recording getRecording(final Sid sid) {
    return null;
  }

  @Override public Recording getRecordingByCall(final Sid callSid) {
    return null;
  }

  @Override public List<Recording> getRecordings(final Sid accountSid) {
    return null;
  }

  @Override public void removeRecording(final Sid sid) {
    
  }

  @Override public void removeRecordings(final Sid accountSid) {
    
  }
  
  private DBObject toDbObject(final Recording recording) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", recording.getSid().toString());
    object.put("date_created", recording.getDateCreated().toDate());
    object.put("date_updated", recording.getDateUpdated().toDate());
    object.put("account_sid", recording.getAccountSid().toString());
    object.put("call_sid", recording.getCallSid().toString());
    object.put("duration", recording.getDuration());
    object.put("api_version", recording.getApiVersion());
    object.put("uri", recording.getUri().toString());
    return object;
  }
  
  private Recording toRecording(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final Sid callSid = new Sid((String)object.get("call_sid"));
    final Integer duration = (Integer)object.get("duration");
    final String apiVersion= (String)object.get("api_version");
    final URI uri = URI.create((String)object.get("uri"));
    return new Recording(sid, dateCreated, dateUpdated, accountSid, callSid, duration, apiVersion, uri);
  }
}

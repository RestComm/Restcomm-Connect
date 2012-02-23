package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoCallDetailRecordsDao implements CallDetailRecordsDao {
  private final DBCollection collection;
  
  public MongoCallDetailRecordsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_call_detail_records");
  }
  
  @Override public void addCallDetailRecord(final CallDetailRecord cdr) {
    
  }

  @Override public CallDetailRecord getCallDetailRecord(final Sid sid) {
    return null;
  }

  @Override public List<CallDetailRecord> getCallDetailRecords(final Sid accountSid) {
    return null;
  }

  @Override	public List<CallDetailRecord> getCallDetailRecordsByRecipient(final String recipient) {
    return null;
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsBySender(final String sender) {
    return null;
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByStatus(final String status) {
    return null;
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByStartTime(final DateTime startTime) {
    return null;
  }

  @Override	public List<CallDetailRecord> getCallDetailRecordsByParentCall(final Sid parenCallSid) {
    return null;
  }

  @Override public void removeCallDetailRecord(final Sid sid) {
    
  }

  @Override public void removeCallDetailRecords(final Sid accountSid) {
    
  }

  @Override public void updateCallDetailRecord(final CallDetailRecord cdr) {
    
  }
  
  private CallDetailRecord toCallDetailRecord(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final Sid parentCallSid = new Sid((String)object.get("parent_call_sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String to = (String)object.get("to");
    final String from = (String)object.get("from");
    final Sid phoneNumberSid = new Sid((String)object.get("phone_number_sid"));
    final String status = (String)object.get("status");
    final DateTime startTime = new DateTime((String)object.get("start_time"));
    final DateTime endTime = new DateTime((String)object.get("end_time"));
    final Integer duration = (Integer)object.get("duration");
    final BigDecimal price = new BigDecimal((String)object.get("price"));
    final String answeredBy = (String)object.get("answered_by");
    final String forwardedFrom = (String)object.get("forwarded_from");
    final String callerName = (String)object.get("caller_name");
    final URI uri = URI.create((String)object.get("uri"));
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from, phoneNumberSid,
        status, startTime, endTime, duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  private DBObject toDbObject(final CallDetailRecord cdr) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", cdr.getSid().toString());
    object.put("parent_call_sid", cdr.getParentCallSid().toString());
    object.put("date_created", cdr.getDateCreated().toDate());
    object.put("date_updated", cdr.getDateUpdated().toDate());
    object.put("account_sid", cdr.getAccountSid().toString());
    object.put("to", cdr.getTo());
    object.put("from", cdr.getFrom());
    object.put("phone_number_sid", cdr.getPhoneNumberSid().toString());
    object.put("status", cdr.getStatus());
    object.put("start_time", cdr.getStartTime().toDate());
    object.put("end_time", cdr.getEndTime().toDate());
    object.put("duration", cdr.getDuration());
    object.put("price", cdr.getPrice().toString());
    object.put("answered_by", cdr.getAnsweredBy());
    object.put("forwarded_from", cdr.getForwardedFrom());
    object.put("caller_name", cdr.getCallerName());
    object.put("uri", cdr.getUri().toString());
    return object;
  }
}

package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.OutgoingCallerId;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoOutgoingCallerIdsDao implements OutgoingCallerIdsDao {
  private final DBCollection collection;

  public MongoOutgoingCallerIdsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_outgoing_caller_ids");
  }
  
  @Override public void addOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
    
  }

  @Override public OutgoingCallerId getOutgoingCallerId(final Sid sid) {
    return null;
  }

  @Override	public List<OutgoingCallerId> getOutgoingCallerIds(final Sid accountSid) {
    return null;
  }

  @Override public void removeOutgoingCallerId(final Sid sid) {
    
  }

  @Override public void removeOutgoingCallerIds(final Sid accountSid) {
    
  }

  @Override public void updateOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
    
  }
  
  private DBObject toDbObject(final OutgoingCallerId outgoingCallerId) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", outgoingCallerId.getSid().toString());
    object.put("date_created", outgoingCallerId.getDateCreated().toDate());
    object.put("date_updated", outgoingCallerId.getDateUpdated().toDate());
    object.put("friendly_name", outgoingCallerId.getFriendlyName());
    object.put("account_sid", outgoingCallerId.getAccountSid().toString());
    object.put("phone_number", outgoingCallerId.getPhoneNumber());
    object.put("uri", outgoingCallerId.getUri().toString());
    return object;
  }
  
  private OutgoingCallerId toOutgoingCallerId(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final String friendlyName = (String)object.get("friendly_name");
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String phoneNumber = (String)object.get("phone_number");
    final URI uri = URI.create((String)object.get("uri"));
    return new OutgoingCallerId(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, uri);
  }
}

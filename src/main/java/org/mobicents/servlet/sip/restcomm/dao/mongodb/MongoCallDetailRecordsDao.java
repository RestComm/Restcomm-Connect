/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordsDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoCallDetailRecordsDao implements CallDetailRecordsDao {
  private static final Logger logger = Logger.getLogger(MongoCallDetailRecordsDao.class);
  private final DBCollection collection;
  
  public MongoCallDetailRecordsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_call_detail_records");
  }
  
  @Override public void addCallDetailRecord(final CallDetailRecord cdr) {
    final WriteResult result = collection.insert(toDbObject(cdr));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public CallDetailRecord getCallDetailRecord(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toCallDetailRecord(result);
    } else {
      return null;
    }
  }

  @Override public List<CallDetailRecord> getCallDetailRecords(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    return getCallDetailRecords(query);
  }

  @Override	public List<CallDetailRecord> getCallDetailRecordsByRecipient(final String recipient) {
    final BasicDBObject query = new BasicDBObject();
    query.put("recipient", recipient);
    return getCallDetailRecords(query);
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsBySender(final String sender) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sender", sender);
    return getCallDetailRecords(query);
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByStatus(final String status) {
    final BasicDBObject query = new BasicDBObject();
    query.put("status", status);
    return getCallDetailRecords(query);
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByStartTime(final DateTime startTime) {
    final BasicDBObject query = new BasicDBObject();
    final BasicDBObject startQuery = new BasicDBObject();
    startQuery.put("$gte", startTime.toDate());
    query.put("start_time", startQuery);
    final DateTime endTime = startTime.plusDays(1);
    final BasicDBObject endQuery = new BasicDBObject();
    endQuery.put("$lt", endTime.toDate());
    query.put("start_time", endQuery);
    return getCallDetailRecords(query);
  }

  @Override	public List<CallDetailRecord> getCallDetailRecordsByParentCall(final Sid parentCallSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("parent_call_sid", parentCallSid.toString());
    return getCallDetailRecords(query);
  }
  
  private List<CallDetailRecord> getCallDetailRecords(final DBObject query) {
    final List<CallDetailRecord> callDetailRecords = new ArrayList<CallDetailRecord>();
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      callDetailRecords.add(toCallDetailRecord(results.next()));
    }
    return callDetailRecords;
  }

  @Override public void removeCallDetailRecord(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeCallDetailRecords(query);
  }

  @Override public void removeCallDetailRecords(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeCallDetailRecords(query);
  }
  
  private void removeCallDetailRecords(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateCallDetailRecord(final CallDetailRecord cdr) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", cdr.getSid().toString());
    final WriteResult result = collection.update(query, toDbObject(cdr));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private CallDetailRecord toCallDetailRecord(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final Sid parentCallSid = readSid(object.get("parent_call_sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String recipient = readString(object.get("recipient"));
    final String sender = readString(object.get("sender"));
    final Sid phoneNumberSid = readSid(object.get("phone_number_sid"));
    final String status = readString(object.get("status"));
    final DateTime startTime = readDateTime(object.get("start_time"));
    final DateTime endTime = readDateTime(object.get("end_time"));
    final Integer duration = readInteger(object.get("duration"));
    final BigDecimal price = readBigDecimal(object.get("price"));
    final String answeredBy = readString(object.get("answered_by"));
    final String forwardedFrom = readString(object.get("forwarded_from"));
    final String callerName = readString(object.get("caller_name"));
    final URI uri = readUri(object.get("uri"));
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, recipient, sender, phoneNumberSid,
        status, startTime, endTime, duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  private DBObject toDbObject(final CallDetailRecord cdr) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(cdr.getSid()));
    object.put("parent_call_sid", writeSid(cdr.getParentCallSid()));
    object.put("date_created", writeDateTime(cdr.getDateCreated()));
    object.put("date_updated", writeDateTime(cdr.getDateUpdated()));
    object.put("account_sid", writeSid(cdr.getAccountSid()));
    object.put("recipient", cdr.getTo());
    object.put("sender", cdr.getFrom());
    object.put("phone_number_sid", writeSid(cdr.getPhoneNumberSid()));
    object.put("status", cdr.getStatus());
    object.put("start_time", writeDateTime(cdr.getStartTime()));
    object.put("end_time", writeDateTime(cdr.getEndTime()));
    object.put("duration", cdr.getDuration());
    object.put("price", writeBigDecimal(cdr.getPrice()));
    object.put("answered_by", cdr.getAnsweredBy());
    object.put("forwarded_from", cdr.getForwardedFrom());
    object.put("caller_name", cdr.getCallerName());
    object.put("uri", writeUri(cdr.getUri()));
    return object;
  }
}

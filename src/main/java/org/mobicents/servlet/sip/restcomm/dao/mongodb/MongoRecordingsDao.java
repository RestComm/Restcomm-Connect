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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Recording;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoRecordingsDao implements RecordingsDao {
  private static final Logger logger = Logger.getLogger(MongoRecordingsDao.class);
  private final DBCollection collection;

  public MongoRecordingsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_recordings");
  }
  
  @Override public void addRecording(final Recording recording) {
    final WriteResult result = collection.insert(toDbObject(recording));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public Recording getRecording(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    return getRecording(query);
  }

  @Override public Recording getRecordingByCall(final Sid callSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("call_sid", callSid.toString());
    return getRecording(query);
  }
  
  private Recording getRecording(final DBObject query) {
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toRecording(result);
    } else {
      return null;
    }
  }

  @Override public List<Recording> getRecordings(final Sid accountSid) {
    final List<Recording> recordings = new ArrayList<Recording>();
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      recordings.add(toRecording(results.next()));
    }
    return recordings;
  }

  @Override public void removeRecording(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeRecordings(query);
  }

  @Override public void removeRecordings(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("accoun_sid", accountSid.toString());
    removeRecordings(query);
  }
  
  private void removeRecordings(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
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

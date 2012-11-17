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

import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readDouble;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.sip.restcomm.entities.Recording;

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
    object.put("sid", writeSid(recording.getSid()));
    object.put("date_created", writeDateTime(recording.getDateCreated()));
    object.put("date_updated", writeDateTime(recording.getDateUpdated()));
    object.put("account_sid", writeSid(recording.getAccountSid()));
    object.put("call_sid", writeSid(recording.getCallSid()));
    object.put("duration", recording.getDuration());
    object.put("api_version", recording.getApiVersion());
    object.put("uri", writeUri(recording.getUri()));
    return object;
  }
  
  private Recording toRecording(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final Sid callSid = readSid(object.get("call_sid"));
    final Double duration = readDouble(object.get("duration"));
    final String apiVersion= readString(object.get("api_version"));
    final URI uri = readUri(object.get("uri"));
    return new Recording(sid, dateCreated, dateUpdated, accountSid, callSid, duration, apiVersion, uri);
  }
}

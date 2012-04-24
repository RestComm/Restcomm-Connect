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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.presence.PresenceRecord;
import org.mobicents.servlet.sip.restcomm.dao.PresenceRecordsDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoPresenceRecordsDao implements PresenceRecordsDao {
  private static final Logger logger = Logger.getLogger(MongoPresenceRecordsDao.class);
  private final DBCollection collection;
  
  public MongoPresenceRecordsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_presence_records");
  }
	
  @Override public void addPresenceRecord(final PresenceRecord record) {
    final WriteResult result = collection.insert(toDbObject(record));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public List<PresenceRecord> getPresenceRecords(final String aor) {
    final BasicDBObject query = new BasicDBObject();
    query.put("address_of_record", aor);
    final List<PresenceRecord> records = new ArrayList<PresenceRecord>();
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      records.add(toPresenceRecord(results.next()));
    }
    return records;
  }
  
  @Override public boolean hasPresenceRecord(final String aor) {
    final BasicDBObject query = new BasicDBObject();
	query.put("address_of_record", aor);
    return collection.count(query) > 0;
  }

  @Override public void removePresenceRecord(final String uri) {
    final BasicDBObject query = new BasicDBObject();
	query.put("uri", uri);
	removePresenceRecords(query);
  }

  @Override public void removePresenceRecords(final String aor) {
    final BasicDBObject query = new BasicDBObject();
    query.put("address_of_record", aor);
    removePresenceRecords(query);
  }
  
  private void removePresenceRecords(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updatePresenceRecord(final PresenceRecord record) {
    final BasicDBObject query = new BasicDBObject();
    query.put("address_of_record", record.getAddressOfRecord());
    final WriteResult result = collection.update(query, toDbObject(record));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final PresenceRecord record) {
    final BasicDBObject object = new BasicDBObject();
    object.put("address_of_record", record.getAddressOfRecord());
    object.put("display_name", record.getDisplayName());
    object.put("uri", record.getUri());
    object.put("user_agent", record.getUserAgent());
    object.put("ttl", record.getTimeToLive());
    object.put("expires", writeDateTime(record.getExpires()));
    return object;
  }
  
  private PresenceRecord toPresenceRecord(final DBObject object) {
    final String aor = readString(object.get("address_of_record"));
    final String name = readString(object.get("display_name"));
    final String uri = readString(object.get("uri"));
    final String ua = readString(object.get("user_agent"));
    final Integer ttl = readInteger(object.get("ttl"));
    final DateTime expires = readDateTime(object.get("expires"));
    return new PresenceRecord(aor, name, uri, ua, ttl, expires);
  }
}

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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.sip.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.sip.restcomm.entities.Registration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoRegistrationsDao implements RegistrationsDao {
  private static final Logger logger = Logger.getLogger(MongoRegistrationsDao.class);
  private final DBCollection collection;
  
  public MongoRegistrationsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_presence_records");
  }
	
  @Override public void addRegistration(final Registration registration) {
    final WriteResult result = collection.insert(toDbObject(registration));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  @Override public Registration getRegistrationByLocation(final String uri) {
	final BasicDBObject query = new BasicDBObject();
    query.put("location", uri);
	final DBObject result = collection.findOne(query);
	if(result != null) {
	  return toPresenceRecord(result);
    } else {
      return null;
    }
  }

  @Override public List<Registration> getRegistrations(final String aor) {
    final BasicDBObject query = new BasicDBObject();
    query.put("address_of_record", aor);
    return getPresenceRecord(query);
  }
  
  @Override public List<Registration> getRegistrationsByUser(final String user) {
    final BasicDBObject query = new BasicDBObject();
    query.put("user_name", user);
    return getPresenceRecord(query);
  }
  
  private List<Registration> getPresenceRecord(final DBObject query) {
    final List<Registration> records = new ArrayList<Registration>();
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      records.add(toPresenceRecord(results.next()));
    }
    return records;
  }
  
  @Override public boolean hasRegistration(final String aor) {
    final BasicDBObject query = new BasicDBObject();
	query.put("address_of_record", aor);
    return collection.count(query) > 0;
  }
  
  @Override public boolean hasRegistration(final Registration registration) {
    final BasicDBObject query = new BasicDBObject();
    query.put("display_name", registration.getDisplayName());
    query.put("address_of_record", registration.getAddressOfRecord());
	query.put("location", registration.getLocation());
	query.put("user_agent", registration.getUserAgent());
    return collection.count(query) > 0;
  }

  @Override public void removeRegistration(final String uri) {
    final BasicDBObject query = new BasicDBObject();
	query.put("location", uri);
	removePresenceRecords(query);
  }

  @Override public void removeRegistrations(final String aor) {
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

  @Override public void updateRegistration(final Registration registration) {
    final BasicDBObject query = new BasicDBObject();
    query.put("address_of_record", registration.getAddressOfRecord());
    final WriteResult result = collection.update(query, toDbObject(registration));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final Registration registration) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(registration.getSid()));
    object.put("date_created", writeDateTime(registration.getDateCreated()));
    object.put("date_updated", writeDateTime(registration.getDateUpdated()));
    object.put("date_expires", writeDateTime(registration.getDateExpires()));
    object.put("address_of_record", registration.getAddressOfRecord());
    object.put("display_name", registration.getDisplayName());
    object.put("user_name", registration.getUserName());
    object.put("location", registration.getLocation());
    object.put("user_agent", registration.getUserAgent());
    object.put("ttl", registration.getTimeToLive());
    return object;
  }
  
  private Registration toPresenceRecord(final DBObject object) {
	final Sid sid = readSid(object.get("sid"));
	final DateTime dateCreated = readDateTime(object.get("date_created"));
	final DateTime dateUpdated = readDateTime(object.get("date_updated"));
	final DateTime dateExpires = readDateTime(object.get("date_expires"));
    final String addressOfRecord = readString(object.get("address_of_record"));
    final String displayName = readString(object.get("display_name"));
    final String userName = readString(object.get("user_name"));
    final String location = readString(object.get("location"));
    final String userAgent = readString(object.get("user_agent"));
    final Integer timeToLive = readInteger(object.get("ttl"));
    return new Registration(sid, dateCreated, dateUpdated, dateExpires, addressOfRecord, displayName,
        userName, userAgent, timeToLive, location);
  }
}

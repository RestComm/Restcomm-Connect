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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoApplicationsDao implements ApplicationsDao {
  private static final Logger logger = Logger.getLogger(MongoApplicationsDao.class);
  private final DBCollection collection;

  public MongoApplicationsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_applications");
  }

  @Override public void addApplication(final Application application) {
    final WriteResult result = collection.insert(toDbObject(application));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public Application getApplication(final Sid sid) {
	final BasicDBObject query = new BasicDBObject();
	query.put("sid", sid.toString());
	final DBObject result = collection.findOne(query);
	if(result != null) {
	  return toApplication(result);
	} else {
	  return null;
	}
  }

  @Override public List<Application> getApplications(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
	query.put("account_sid", accountSid.toString());
	final List<Application> applications = new ArrayList<Application>();
	final DBCursor results = collection.find(query);
	while(results.hasNext()) {
	  applications.add(toApplication(results.next()));
	}
	return applications;
  }

  @Override public void removeApplication(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeApplications(query);
  }

  @Override public void removeApplications(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeApplications(query);
  }
  
  private void removeApplications(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateApplication(final Application application) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", application.getSid().toString());
    final WriteResult result = collection.update(query, toDbObject(application));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private Application toApplication(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final String friendlyName = readString(object.get("friendly_name"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String apiVersion = readString(object.get("api_version"));
    final URI voiceUrl = readUri(object.get("voice_url"));
    final String voiceMethod = readString(object.get("voice_method"));
    final URI voiceFallbackUrl = readUri(object.get("voice_fallback_url"));
    final String voiceFallbackMethod = readString(object.get("voice_fallback_method"));
    final URI statusCallback = readUri(object.get("status_callback"));
    final String statusCallbackMethod = readString(object.get("status_callback_method"));
    final Boolean hasVoiceCallerIdLookup = readBoolean(object.get("voice_caller_id_lookup"));
    final URI smsUrl = readUri(object.get("sms_url"));
    final String smsMethod = readString(object.get("sms_method"));
    final URI smsFallbackUrl = readUri(object.get("sms_fallback_url"));
    final String smsFallbackMethod = readString(object.get("sms_fallback_method"));
    final URI smsStatusCallback = readUri(object.get("sms_status_callback"));
    final URI uri = readUri(object.get("uri"));
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
        voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl, smsMethod,
        smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  private DBObject toDbObject(final Application application) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(application.getSid()));
    object.put("date_created", writeDateTime(application.getDateCreated()));
    object.put("date_updated", writeDateTime(application.getDateUpdated()));
    object.put("friendly_name", application.getFriendlyName());
    object.put("account_sid", writeSid(application.getAccountSid()));
    object.put("api_version", application.getApiVersion());
    object.put("voice_url", writeUri(application.getVoiceUrl()));
    object.put("voice_method", application.getVoiceMethod());
    object.put("voice_fallback_url", writeUri(application.getVoiceFallbackUrl()));
    object.put("voice_fallback_method", application.getVoiceFallbackMethod());
    object.put("status_callback", writeUri(application.getStatusCallback()));
    object.put("status_callback_method", application.getStatusCallbackMethod());
    object.put("voice_caller_id_lookup", application.hasVoiceCallerIdLookup());
    object.put("sms_url", writeUri(application.getSmsUrl()));
    object.put("sms_method", application.getSmsMethod());
    object.put("sms_fallback_url", writeUri(application.getSmsFallbackUrl()));
    object.put("sms_fallback_method", application.getSmsFallbackMethod());
    object.put("sms_status_callback", writeUri(application.getSmsStatusCallback()));
    object.put("uri", writeUri(application.getUri()));
    return object;
  }
}

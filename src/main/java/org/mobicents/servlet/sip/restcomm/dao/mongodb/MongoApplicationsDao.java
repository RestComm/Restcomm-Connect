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
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoApplicationsDao implements ApplicationsDao {
  private final DBCollection collection;

  public MongoApplicationsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_applications");
  }

  @Override public void addApplication(final Application application) {
    
  }

  @Override public Application getApplication(final Sid sid) {
    return null;
  }

  @Override public List<Application> getApplications(final Sid accountSid) {
    return null;
  }

  @Override public void removeApplication(final Sid sid) {
    
  }

  @Override public void removeApplications(final Sid accountSid) {
    
  }

  @Override public void updateApplication(final Application application) {
    
  }
  
  private Application toApplication(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final String friendlyName = (String)object.get("friendly_name");
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String apiVersion = (String)object.get("api_version");
    final URI voiceUrl = URI.create((String)object.get("voice_url"));
    final String voiceMethod = (String)object.get("voice_method");
    final URI voiceFallbackUrl = URI.create((String)object.get("voice_fallback_url"));
    final String voiceFallbackMethod = (String)object.get("voice_fallback_method");
    final URI statusCallback = URI.create((String)object.get("status_callback"));
    final String statusCallbackMethod = (String)object.get("status_callback_method");
    final Boolean hasVoiceCallerIdLookup = (Boolean)object.get("voice_caller_id_lookup");
    final URI smsUrl = URI.create((String)object.get("sms_url"));
    final String smsMethod = (String)object.get("sms_method");
    final URI smsFallbackUrl = URI.create((String)object.get("sms_fallback_url"));
    final String smsFallbackMethod = (String)object.get("sms_fallback_method");
    final URI smsStatusCallback = URI.create((String)object.get("sms_status_callback"));
    final URI uri = URI.create((String)object.get("uri"));
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
        voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl, smsMethod,
        smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  private DBObject toDbObject(final Application application) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", application.getSid().toString());
    object.put("date_created", application.getDateCreated().toDate());
    object.put("date_updated", application.getDateUpdated().toDate());
    object.put("friendly_name", application.getFriendlyName());
    object.put("account_sid", application.getAccountSid().toString());
    object.put("api_version", application.getApiVersion());
    object.put("voice_url", application.getVoiceUrl().toString());
    object.put("voice_method", application.getVoiceMethod());
    object.put("voice_fallback_url", application.getVoiceFallbackUrl().toString());
    object.put("voice_fallback_method", application.getVoiceFallbackMethod());
    object.put("status_callback", application.getStatusCallback().toString());
    object.put("status_callback_method", application.getStatusCallbackMethod());
    object.put("voice_caller_id_lookup", application.hasVoiceCallerIdLookup());
    object.put("sms_url", application.getSmsUrl().toString());
    object.put("sms_method", application.getSmsMethod());
    object.put("sms_fallback_url", application.getSmsFallbackUrl().toString());
    object.put("sms_fallback_method", application.getSmsFallbackMethod());
    object.put("sms_status_callback", application.getSmsStatusCallback().toString());
    object.put("uri", application.getUri().toString());
    return object;
  }
}

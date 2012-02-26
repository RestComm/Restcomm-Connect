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
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoIncomingPhoneNumbersDao implements IncomingPhoneNumbersDao {
  private static final Logger logger = Logger.getLogger(MongoIncomingPhoneNumbersDao.class);
  private final DBCollection collection;

  public MongoIncomingPhoneNumbersDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_incoming_phone_numbers");
  }
  
  @Override public void addIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    final WriteResult result = collection.insert(toDbObject(incomingPhoneNumber));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final Sid sid) {
	final BasicDBObject query = new BasicDBObject();
	query.put("sid", sid.toString());
    return getIncomingPhoneNumber(query);
  }
  
  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final String phoneNumber) {
    final BasicDBObject query = new BasicDBObject();
    query.put("phone_number", phoneNumber);
    return getIncomingPhoneNumber(query);
  }
  
  private IncomingPhoneNumber getIncomingPhoneNumber(final DBObject query) {
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toIncomingPhoneNumber(result);
    } else {
      return null;
    }
  }

  @Override public List<IncomingPhoneNumber> getIncomingPhoneNumbers(final Sid accountSid) {
    final List<IncomingPhoneNumber> incomingPhoneNumbers = new ArrayList<IncomingPhoneNumber>();
	final BasicDBObject query = new BasicDBObject();
	query.put("account_sid", accountSid.toString());
	final DBCursor results = collection.find(query);
	while(results.hasNext()) {
	  incomingPhoneNumbers.add(toIncomingPhoneNumber(results.next()));
	}
    return incomingPhoneNumbers;
  }

  @Override public void removeIncomingPhoneNumber(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeIncomingPhoneNumbers(query);
  }

  @Override	public void removeIncomingPhoneNumbers(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeIncomingPhoneNumbers(query);
  }
  
  private void removeIncomingPhoneNumbers(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", incomingPhoneNumber.getSid().toString());
    final WriteResult result = collection.update(query, toDbObject(incomingPhoneNumber));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final IncomingPhoneNumber incomingPhoneNumber) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(incomingPhoneNumber.getSid()));
    object.put("date_created", writeDateTime(incomingPhoneNumber.getDateCreated()));
    object.put("date_updated", writeDateTime(incomingPhoneNumber.getDateUpdated()));
    object.put("friendly_name", incomingPhoneNumber.getFriendlyName());
    object.put("account_sid", writeSid(incomingPhoneNumber.getAccountSid()));
    object.put("phone_number", incomingPhoneNumber.getPhoneNumber());
    object.put("api_version", incomingPhoneNumber.getApiVersion());
    object.put("voice_caller_id_lookup", incomingPhoneNumber.hasVoiceCallerIdLookup());
    object.put("voice_url", writeUri(incomingPhoneNumber.getVoiceUrl()));
    object.put("voice_method", incomingPhoneNumber.getVoiceMethod());
    object.put("voice_fallback_url", writeUri(incomingPhoneNumber.getVoiceFallbackUrl()));
    object.put("voice_fallback_method", incomingPhoneNumber.getVoiceFallbackMethod());
    object.put("status_callback", writeUri(incomingPhoneNumber.getStatusCallback()));
    object.put("status_callback_method", incomingPhoneNumber.getStatusCallbackMethod());
    object.put("voice_application_sid", writeSid(incomingPhoneNumber.getVoiceApplicationSid()));
    object.put("sms_url", writeUri(incomingPhoneNumber.getSmsUrl()));
    object.put("sms_method", incomingPhoneNumber.getSmsMethod());
    object.put("sms_fallback_url", writeUri(incomingPhoneNumber.getSmsFallbackUrl()));
    object.put("sms_fallback_method", incomingPhoneNumber.getSmsFallbackMethod());
    object.put("sms_application_sid", writeSid(incomingPhoneNumber.getSmsApplicationSid()));
    object.put("uri", writeUri(incomingPhoneNumber.getUri()));
    return object;
  }
  
  private IncomingPhoneNumber toIncomingPhoneNumber(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final String friendlyName = readString(object.get("friendly_name"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String phoneNumber = readString(object.get("phone_number"));
    final String apiVersion = readString(object.get("api_version"));
    final Boolean hasVoiceCallerIdLookup = readBoolean(object.get("voice_caller_id_lookup"));
    final URI voiceUrl = readUri(object.get("voice_url"));
    final String voiceMethod = readString(object.get("voice_method"));
    final URI voiceFallbackUrl = readUri(object.get("voice_fallback_url"));
    final String voiceFallbackMethod = readString(object.get("voice_fallback_method"));
    final URI statusCallback = readUri(object.get("status_callback"));
    final String statusCallbackMethod = readString(object.get("status_callback_method"));
    final Sid voiceApplicationSid = readSid(object.get("voice_application_sid"));
    final URI smsUrl = readUri(object.get("sms_url"));
    final String smsMethod = readString(object.get("sms_method"));
    final URI smsFallbackUrl = readUri(object.get("sms_fallback_url"));
    final String smsFallbackMethod = readString(object.get("sms_fallback_method"));
    final Sid smsApplicationSid = readSid(object.get("sms_application_sid"));
    final URI uri = readUri(object.get("uri"));
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, apiVersion,
        hasVoiceCallerIdLookup, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod,
        voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
}

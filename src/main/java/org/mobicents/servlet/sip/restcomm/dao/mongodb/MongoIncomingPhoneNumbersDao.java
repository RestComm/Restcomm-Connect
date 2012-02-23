package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoIncomingPhoneNumbersDao implements IncomingPhoneNumbersDao {
  private final DBCollection collection;

  public MongoIncomingPhoneNumbersDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_incoming_phone_numbers");
  }
  
  @Override public void addIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    
  }

  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final Sid sid) {
    return null;
  }

  @Override public List<IncomingPhoneNumber> getIncomingPhoneNumbers(final Sid accountSid) {
    return null;
  }

  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final String phoneNumber) {
    return null;
  }

  @Override public void removeIncomingPhoneNumber(final Sid sid) {
    
  }

  @Override	public void removeIncomingPhoneNumbers(final Sid accountSid) {
    
  }

  @Override public void updateIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    
  }
  
  private DBObject toDbObject(final IncomingPhoneNumber incomingPhoneNumber) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", incomingPhoneNumber.getSid().toString());
    object.put("date_created", incomingPhoneNumber.getDateCreated().toDate());
    object.put("date_updated", incomingPhoneNumber.getDateUpdated().toDate());
    object.put("friendly_name", incomingPhoneNumber.getFriendlyName());
    object.put("account_sid", incomingPhoneNumber.getAccountSid().toString());
    object.put("phone_number", incomingPhoneNumber.getPhoneNumber());
    object.put("api_version", incomingPhoneNumber.getApiVersion());
    object.put("voice_caller_id_lookup", incomingPhoneNumber.hasVoiceCallerIdLookup());
    object.put("voice_url", incomingPhoneNumber.getVoiceUrl().toString());
    object.put("voice_method", incomingPhoneNumber.getVoiceMethod());
    object.put("voice_fallback_url", incomingPhoneNumber.getVoiceFallbackUrl().toString());
    object.put("voice_fallback_method", incomingPhoneNumber.getVoiceFallbackMethod());
    object.put("status_callback", incomingPhoneNumber.getStatusCallback().toString());
    object.put("status_callback_method", incomingPhoneNumber.getStatusCallbackMethod());
    object.put("voice_application_sid", incomingPhoneNumber.getVoiceApplicationSid().toString());
    object.put("sms_url", incomingPhoneNumber.getSmsUrl().toString());
    object.put("sms_method", incomingPhoneNumber.getSmsMethod());
    object.put("sms_fallback_url", incomingPhoneNumber.getSmsFallbackUrl().toString());
    object.put("sms_fallback_method", incomingPhoneNumber.getSmsFallbackMethod());
    object.put("sms_application_sid", incomingPhoneNumber.getSmsApplicationSid().toString());
    object.put("uri", incomingPhoneNumber.getUri().toString());
    return object;
  }
  
  private IncomingPhoneNumber toIncomingPhoneNumber(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((String)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((String)object.get("date_updated"));
    final String friendlyName = (String)object.get("friendly_name");
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String phoneNumber = (String)object.get("phone_number");
    final String apiVersion = (String)object.get("api_version");
    final Boolean hasVoiceCallerIdLookup = (Boolean)object.get("voice_caller_id_lookup");
    final URI voiceUrl = URI.create((String)object.get("voice_url"));
    final String voiceMethod = (String)object.get("voice_method");
    final URI voiceFallbackUrl = URI.create((String)object.get("voice_fallback_url"));
    final String voiceFallbackMethod = (String)object.get("voice_fallback_method");
    final URI statusCallback = URI.create((String)object.get("status_callback"));
    final String statusCallbackMethod = (String)object.get("status_callback_method");
    final Sid voiceApplicationSid = new Sid((String)object.get("voice_application_sid"));
    final URI smsUrl = URI.create((String)object.get("sms_url"));
    final String smsMethod = (String)object.get("sms_method");
    final URI smsFallbackUrl = URI.create((String)object.get("sms_fallback_url"));
    final String smsFallbackMethod = (String)object.get("sms_fallback_method");
    final Sid smsApplicationSid = new Sid((String)object.get("sms_application_sid"));
    final URI uri = URI.create((String)object.get("uri"));
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, apiVersion,
        hasVoiceCallerIdLookup, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod,
        voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
}

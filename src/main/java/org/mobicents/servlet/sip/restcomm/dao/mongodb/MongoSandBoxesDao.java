package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.SandBox;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoSandBoxesDao implements SandBoxesDao {
  private final DBCollection collection;

  public MongoSandBoxesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_sand_boxes");
  }
  
  @Override public void addSandBox(final SandBox sandBox) {
    
  }

  @Override public SandBox getSandBox(final Sid accountSid) {
    return null;
  }

  @Override public void removeSandBox(final Sid accountSid) {
    
  }

  @Override public void updateSandBox(final SandBox sandBox) {
    
  }
  
  private DBObject toDbObject(final SandBox sandBox) {
    final BasicDBObject object = new BasicDBObject();
    object.put("date_created", sandBox.getDateCreated().toDate());
    object.put("date_updated", sandBox.getDateUpdated().toDate());
    object.put("pin", sandBox.getPin());
    object.put("account_sid", sandBox.getAccountSid().toString());
    object.put("phone_number", sandBox.getPhoneNumber());
    object.put("application_sid", sandBox.getApplicationSid().toString());
    object.put("api_version", sandBox.getApiVersion());
    object.put("voice_url", sandBox.getVoiceUrl().toString());
    object.put("voice_method", sandBox.getVoiceMethod());
    object.put("sms_url", sandBox.getSmsUrl().toString());
    object.put("sms_method", sandBox.getSmsMethod());
    object.put("status_callback", sandBox.getStatusCallback());
    object.put("status_callback_method", sandBox.getStatusCallbackMethod());
    object.put("uri", sandBox.getUri().toString());
    return object;
  }
  
  private SandBox toSandBox(final DBObject object) {
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final String pin = (String)object.get("pin");
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String phoneNumber = (String)object.get("phone_number");
    final Sid applicationSid = new Sid((String)object.get("application_sid"));
    final String apiVersion = (String)object.get("api_version");
    final URI voiceUrl = URI.create((String)object.get("voice_url"));
    final String voiceMethod = (String)object.get("voice_method");
    final URI smsUrl = URI.create((String)object.get("sms_url"));
    final String smsMethod = (String)object.get("sms_method");
    final URI statusCallback = URI.create((String)object.get("status_callback"));
    final String statusCallbackMethod = (String)object.get("status_callback_method");
    final URI uri = URI.create((String)object.get("uri"));
    return new SandBox(dateCreated, dateUpdated, pin, accountSid, phoneNumber, applicationSid, apiVersion,
        voiceUrl, voiceMethod, smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
}

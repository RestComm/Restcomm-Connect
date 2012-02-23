package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.ShortCode;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ShortCodesDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoShortCodesDao implements ShortCodesDao {
  private final DBCollection collection;
  
  public MongoShortCodesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_short_codes");
  }
  
  @Override public void addShortCode(final ShortCode shortCode) {
    
  }

  @Override public ShortCode getShortCode(final Sid sid) {
    return null;
  }

  @Override public List<ShortCode> getShortCodes(final Sid accountSid) {
    return null;
  }

  @Override public void removeShortCode(final Sid sid) {
    
  }

  @Override public void removeShortCodes(final Sid accountSid) {
    
  }

  @Override public void updateShortCode(final ShortCode shortCode) {
    
  }
  
  private DBObject toDbObject(final ShortCode shortCode) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", shortCode.getSid().toString());
    object.put("date_created", shortCode.getDateCreated().toDate());
    object.put("date_updated", shortCode.getDateUpdated().toDate());
    object.put("friendly_name", shortCode.getFriendlyName());
    object.put("account_sid", shortCode.getAccountSid().toString());
    object.put("short_code", shortCode.getShortCode());
    object.put("api_version", shortCode.getApiVersion());
    object.put("sms_url", shortCode.getSmsUrl().toString());
    object.put("sms_method", shortCode.getSmsMethod());
    object.put("sms_fallback_url", shortCode.getSmsFallbackUrl().toString());
    object.put("sms_fallback_method", shortCode.getSmsFallbackMethod());
    object.put("uri", shortCode.getUri().toString());
    return object;
  }
  
  private ShortCode toShortCode(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final String friendlyName = (String)object.get("friendly_name");
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final Integer shortCode = (Integer)object.get("short_code");
    final String apiVersion = (String)object.get("api_version");
    final URI smsUrl = URI.create((String)object.get("sms_url"));
    final String smsMethod = (String)object.get("sms_method");
    final URI smsFallbackUrl = URI.create((String)object.get("sms_fallback_url"));
    final String smsFallbackMethod = (String)object.get("sms_fallback_method");
    final URI uri = URI.create((String)object.get("uri"));
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion,
        smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
}

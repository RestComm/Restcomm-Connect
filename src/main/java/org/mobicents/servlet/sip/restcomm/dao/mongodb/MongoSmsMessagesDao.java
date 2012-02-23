package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoSmsMessagesDao implements SmsMessagesDao {
  private final DBCollection collection;
  
  public MongoSmsMessagesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_sms_messages");
  }
  
  @Override public void addSmsMessage(final SmsMessage smsMessage) {
    
  }

  @Override public SmsMessage getSmsMessage(final Sid sid) {
    return null;
  }

  @Override public List<SmsMessage> getSmsMessages(final Sid accountSid) {
    return null;
  }

  @Override public void removeSmsMessage(final Sid sid) {
    
  }

  @Override	public void removeSmsMessages(final Sid accountSid) {
    
  }
  
  private DBObject toDbObject(final SmsMessage smsMessage) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", smsMessage.getSid().toString());
    object.put("date_created", smsMessage.getDateCreated().toDate());
    object.put("date_updated", smsMessage.getDateUpdated().toDate());
    object.put("date_sent", smsMessage.getDateSent().toDate());
    object.put("account_sid", smsMessage.getAccountSid().toString());
    object.put("sender", smsMessage.getSender());
    object.put("recipient", smsMessage.getRecipient());
    object.put("body", smsMessage.getBody());
    object.put("status", smsMessage.getStatus());
    object.put("direction", smsMessage.getDirection());
    object.put("price", smsMessage.getPrice().toString());
    object.put("api_version", smsMessage.getApiVersion());
    object.put("uri", smsMessage.getUri().toString());
    return object;
  }
  
  private SmsMessage toSmsMessage(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final DateTime dateSent = new DateTime((Date)object.get("date_sent"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String sender = (String)object.get("sender");
    final String recipient = (String)object.get("recipient");
    final String body = (String)object.get("body");
    final String status = (String)object.get("status");
    final String direction = (String)object.get("direction");
    final BigDecimal price = new BigDecimal((String)object.get("price"));
    final String apiVersion = (String)object.get("api_version");
    final URI uri = URI.create((String)object.get("uri"));
    return new SmsMessage(sid, dateCreated, dateUpdated, dateSent, accountSid, sender, recipient,
        body, status, direction, price, apiVersion, uri);
  }
}

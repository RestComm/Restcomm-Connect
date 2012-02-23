package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@ThreadSafe public final class MongoNotificationsDao implements NotificationsDao {
  private final DBCollection collection;

  public MongoNotificationsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_notifications");
  }
  
  @Override public void addNotification(final Notification notification) {
    
  }

  @Override public Notification getNotification(final Sid sid) {
    return null;
  }

  @Override public List<Notification> getNotifications(final Sid accountSid) {
    return null;
  }

  @Override public List<Notification> getNotificationsByCall(final Sid callSid) {
    return null;
  }

  @Override public List<Notification> getNotificationsByLogLevel(final int logLevel) {
    return null;
  }

  @Override public List<Notification> getNotificationsByMessageDate(final DateTime messageDate) {
    return null;
  }

  @Override public void removeNotification(final Sid sid) {
    
  }

  @Override public void removeNotifications(final Sid accountSid) {
    
  }

  @Override public void removeNotificationsByCall(final Sid callSid) {
    
  }
  
  private DBObject toDbObject(final Notification notification) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", notification.getSid().toString());
    object.put("date_created", notification.getDateCreated().toDate());
    object.put("date_updated", notification.getDateUpdated().toDate());
    object.put("account_sid", notification.getAccountSid().toString());
    object.put("call_sid", notification.getCallSid().toString());
    object.put("api_version", notification.getApiVersion());
    object.put("log", notification.getLog());
    object.put("error_code", notification.getErrorCode());
    object.put("more_info", notification.getMoreInfo().toString());
    object.put("message_text", notification.getMessageText());
    object.put("message_date", notification.getMessageDate().toDate());
    object.put("request_url", notification.getRequestUrl().toString());
    object.put("request_method", notification.getRequestMethod());
    object.put("request_variables", notification.getRequestVariables());
    object.put("response_headers", notification.getResponseHeaders());
    object.put("response_body", notification.getResponseBody());
    object.put("uri", notification.getUri().toString());
    return object;
  }
  
  private Notification toNotification(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final Sid callSid = new Sid((String)object.get("call_sid"));
    final String apiVersion = (String)object.get("api_version");
    final Integer log = (Integer)object.get("log");
    final Integer errorCode = (Integer)object.get("error_code");
    final URI moreInfo = URI.create((String)object.get("more_info"));
    final String messageText = (String)object.get("message_text");
    final DateTime messageDate = new DateTime((String)object.get("message_date"));
    final URI requestUrl = URI.create((String)object.get("request_url"));
    final String requestMethod = (String)object.get("request_method");
    final String requestVariables = (String)object.get("request_variables");
    final String responseHeaders = (String)object.get("response_headers");
    final String responseBody = (String)object.get("response_body");
    final URI uri = URI.create((String)object.get("uri"));
    return new Notification(sid, dateCreated, dateUpdated, accountSid, callSid, apiVersion, log,
        errorCode, moreInfo, messageText, messageDate, requestUrl, requestMethod, requestVariables,
        responseHeaders, responseBody, uri);
  }
}

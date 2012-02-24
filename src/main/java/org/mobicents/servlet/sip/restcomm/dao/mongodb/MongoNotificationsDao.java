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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoNotificationsDao implements NotificationsDao {
  private static final Logger logger = Logger.getLogger(MongoNotificationsDao.class);
  private final DBCollection collection;

  public MongoNotificationsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_notifications");
  }
  
  @Override public void addNotification(final Notification notification) {
    final WriteResult result = collection.insert(toDbObject(notification));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public Notification getNotification(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toNotification(result);
    } else {
      return null;
    }
  }

  @Override public List<Notification> getNotifications(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    return getNotifications(query);
  }

  @Override public List<Notification> getNotificationsByCall(final Sid callSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("call_sid", callSid.toString());
    return getNotifications(query);
  }

  @Override public List<Notification> getNotificationsByLogLevel(final int logLevel) {
    final BasicDBObject query = new BasicDBObject();
    query.put("log", logLevel);
    return getNotifications(query);
  }

  @Override public List<Notification> getNotificationsByMessageDate(final DateTime messageDate) {
	final BasicDBObject query = new BasicDBObject();
	query.put("message_date", messageDate.toDate());
    return getNotifications(query);
  }
  
  private List<Notification> getNotifications(final DBObject query) {
    final List<Notification> notifications = new ArrayList<Notification>();
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      notifications.add(toNotification(results.next()));
    }
    return notifications;
  }

  @Override public void removeNotification(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeNotifications(query);
  }

  @Override public void removeNotifications(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeNotifications(query);
  }

  @Override public void removeNotificationsByCall(final Sid callSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("call_sid", callSid.toString());
    removeNotifications(query);
  }
  
  private void removeNotifications(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
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

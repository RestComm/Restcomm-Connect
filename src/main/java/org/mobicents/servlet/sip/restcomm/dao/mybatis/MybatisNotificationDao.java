package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.NotificationDao;

@ThreadSafe public final class MybatisNotificationDao implements NotificationDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.NotificationDao.";
  private final SqlSessionFactory sessions;

  public MybatisNotificationDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addNotification(final Notification notification) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addNotification", toMap(notification));
    } finally {
      session.close();
    }
  }

  @Override public Notification getNotification(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
	  final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getNotification", sid.toString());
      if(result != null) {
        return toNotification(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<Notification> getNotifications(final Sid accountSid) {
    return getNotifications(namespace + "getNotifications", accountSid.toString());
  }

  @Override public List<Notification> getNotificationsByCall(final Sid callSid) {
    return getNotifications(namespace + "getNotificationsByCall", callSid.toString());
  }

  @Override public List<Notification> getNotificationsByLogLevel(final int logLevel) {
    return getNotifications(namespace + "getNotificationsByLogLevel", logLevel);
  }

  @Override public List<Notification> getNotificationsByMessageDate(final DateTime messageDate) {
    return getNotifications(namespace + "getNotificationsByMessageDate", messageDate.toDate());
  }
  
  private List<Notification> getNotifications(final String selector, final Object input) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(selector, input);
      final List<Notification> notifications = new ArrayList<Notification>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          notifications.add(toNotification(result));
        }
      }
      return notifications;
    } finally {
      session.close();
    }
  }

  @Override public void removeNotification(final Sid sid) {
    removeNotifications(namespace + "removeNotification", sid);
  }

  @Override public void removeNotifications(final Sid accountSid) {
    removeNotifications(namespace + "removeNotifications", accountSid);
  }

  @Override public void removeNotificationsByCall(final Sid callSid) {
    removeNotifications(namespace + "removeNotificationsByCall", callSid);
  }
  
  private void removeNotifications(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final Notification notification) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", notification.getSid().toString());
    map.put("date_created", notification.getDateCreated().toDate());
    map.put("date_updated", notification.getDateUpdated().toDate());
    map.put("account_sid", notification.getAccountSid().toString());
    map.put("call_sid", notification.getCallSid().toString());
    map.put("api_version", notification.getApiVersion());
    map.put("log", notification.getLog());
    map.put("error_code", notification.getErrorCode());
    map.put("more_info", notification.getMoreInfo().toString());
    map.put("message_text", notification.getMessageText());
    map.put("message_date", notification.getMessageDate().toDate());
    map.put("request_url", notification.getRequestUrl().toString());
    map.put("request_method", notification.getRequestMethod());
    map.put("request_variables", notification.getRequestVariables());
    map.put("response_headers", notification.getResponseHeaders());
    map.put("response_body", notification.getResponseBody());
    map.put("uri", notification.getUri().toString());
    return map;
  }
  
  private Notification toNotification(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final Sid callSid = new Sid((String)map.get("call_sid"));
    final String apiVersion = (String)map.get("api_version");
    final Integer log = (Integer)map.get("log");
    final Integer errorCode = (Integer)map.get("error_code");
    final URI moreInfo = URI.create((String)map.get("more_info"));
    final String messageText = (String)map.get("message_text");
    final DateTime messageDate = new DateTime((Date)map.get("message_date"));
    final URI requestUrl = URI.create((String)map.get("request_url"));
    final String requestMethod = (String)map.get("request_method");
    final String requestVariables = (String)map.get("request_variables");
    final String responseHeaders = (String)map.get("response_headers");
    final String responseBody = (String)map.get("response_body");
    final URI uri = URI.create((String)map.get("uri"));
    return new Notification(sid, dateCreated, dateUpdated, accountSid, callSid, apiVersion,
        log, errorCode, moreInfo, messageText, messageDate, requestUrl, requestMethod,
        requestVariables, responseHeaders, responseBody, uri);
  }
}

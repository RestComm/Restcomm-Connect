package org.mobicents.servlet.sip.restcomm.dao;

import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.Sid;

public interface NotificationsDao {
  public void addNotification(Notification notification);
  public Notification getNotification(Sid sid);
  public List<Notification> getNotifications(Sid accountSid);
  public List<Notification> getNotificationsByCall(Sid callSid);
  public List<Notification> getNotificationsByLogLevel(int logLevel);
  public List<Notification> getNotificationsByMessageDate(DateTime messageDate);
  public void removeNotification(Sid sid);
  public void removeNotifications(Sid accountSid);
  public void removeNotificationsByCall(Sid callSid);
}

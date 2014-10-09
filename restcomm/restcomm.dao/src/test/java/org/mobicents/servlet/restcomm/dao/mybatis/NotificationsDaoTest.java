/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class NotificationsDaoTest {
    private static MybatisDaoManager manager;

    public NotificationsDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void createReadDelete() {
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid call = Sid.generate(Sid.Type.CALL);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final String method = "GET";
        final Notification.Builder builder = Notification.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setCallSid(call);
        builder.setApiVersion("2012-04-24");
        builder.setLog(0);
        builder.setErrorCode(11100);
        builder.setMoreInfo(url);
        builder.setMessageText("hello world!");
        builder.setMessageDate(DateTime.now());
        builder.setRequestUrl(url);
        builder.setRequestMethod(method);
        builder.setRequestVariables("hello world!");
        builder.setResponseHeaders("hello world!");
        builder.setResponseBody("hello world!");
        builder.setUri(url);
        Notification notification = builder.build();
        final NotificationsDao notifications = manager.getNotificationsDao();
        // Create a new notification in the data store.
        notifications.addNotification(notification);
        // Read the notification from the data store.
        Notification result = notifications.getNotification(sid);
        // Verify the results.
        assertTrue(result.getSid().equals(notification.getSid()));
        assertTrue(result.getAccountSid().equals(notification.getAccountSid()));
        assertTrue(result.getCallSid().equals(notification.getCallSid()));
        assertTrue(result.getApiVersion().equals(notification.getApiVersion()));
        assertTrue(result.getLog() == notification.getLog());
        assertTrue(result.getErrorCode().equals(notification.getErrorCode()));
        assertTrue(result.getMoreInfo().equals(notification.getMoreInfo()));
        assertTrue(result.getMessageText().equals(notification.getMessageText()));
        assertTrue(result.getMessageDate().equals(notification.getMessageDate()));
        assertTrue(result.getRequestUrl().equals(notification.getRequestUrl()));
        assertTrue(result.getRequestMethod().equals(notification.getRequestMethod()));
        assertTrue(result.getRequestVariables().equals(notification.getRequestVariables()));
        assertTrue(result.getResponseHeaders().equals(notification.getResponseHeaders()));
        assertTrue(result.getResponseBody().equals(notification.getResponseBody()));
        assertTrue(result.getUri().equals(notification.getUri()));
        // Delete the notification.
        notifications.removeNotification(sid);
        // Validate that the notification was removed.
        assertTrue(notifications.getNotification(sid) == null);
    }

    @Test
    public void testByAccount() {
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid call = Sid.generate(Sid.Type.CALL);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final String method = "GET";
        final Notification.Builder builder = Notification.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setCallSid(call);
        builder.setApiVersion("2012-04-24");
        builder.setLog(0);
        builder.setErrorCode(11100);
        builder.setMoreInfo(url);
        builder.setMessageText("hello world!");
        builder.setMessageDate(DateTime.now());
        builder.setRequestUrl(url);
        builder.setRequestMethod(method);
        builder.setRequestVariables("hello world!");
        builder.setResponseHeaders("hello world!");
        builder.setResponseBody("hello world!");
        builder.setUri(url);
        Notification notification = builder.build();
        final NotificationsDao notifications = manager.getNotificationsDao();
        // Create a new notification in the data store.
        notifications.addNotification(notification);
        // Read the notification from the data store.
        assertTrue(notifications.getNotifications(account).size() == 1);
        // Delete the notification.
        notifications.removeNotifications(account);
        // Validate that the notification was removed.
        assertTrue(notifications.getNotifications(account).size() == 0);
    }

    @Test
    public void testByCall() {
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid call = Sid.generate(Sid.Type.CALL);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final String method = "GET";
        final Notification.Builder builder = Notification.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setCallSid(call);
        builder.setApiVersion("2012-04-24");
        builder.setLog(0);
        builder.setErrorCode(11100);
        builder.setMoreInfo(url);
        builder.setMessageText("hello world!");
        builder.setMessageDate(DateTime.now());
        builder.setRequestUrl(url);
        builder.setRequestMethod(method);
        builder.setRequestVariables("hello world!");
        builder.setResponseHeaders("hello world!");
        builder.setResponseBody("hello world!");
        builder.setUri(url);
        Notification notification = builder.build();
        final NotificationsDao notifications = manager.getNotificationsDao();
        // Create a new notification in the data store.
        notifications.addNotification(notification);
        // Read the notification from the data store.
        assertTrue(notifications.getNotificationsByCall(call).size() == 1);
        // Delete the notification.
        notifications.removeNotificationsByCall(call);
        // Validate that the notification was removed.
        assertTrue(notifications.getNotificationsByCall(call).size() == 0);
    }

    @Test
    public void testByLogLevel() {
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid call = Sid.generate(Sid.Type.CALL);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final String method = "GET";
        final Notification.Builder builder = Notification.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setCallSid(call);
        builder.setApiVersion("2012-04-24");
        builder.setLog(0);
        builder.setErrorCode(11100);
        builder.setMoreInfo(url);
        builder.setMessageText("hello world!");
        builder.setMessageDate(DateTime.now());
        builder.setRequestUrl(url);
        builder.setRequestMethod(method);
        builder.setRequestVariables("hello world!");
        builder.setResponseHeaders("hello world!");
        builder.setResponseBody("hello world!");
        builder.setUri(url);
        Notification notification = builder.build();
        final NotificationsDao notifications = manager.getNotificationsDao();
        // Create a new notification in the data store.
        notifications.addNotification(notification);
        // Read the notification from the data store.
        assertTrue(notifications.getNotificationsByLogLevel(0).size() == 1);
        // Delete the notification.
        notifications.removeNotification(sid);
        // Validate that the notification was removed.
        assertTrue(notifications.getNotification(sid) == null);
    }

    @Test
    public void testByMessageDate() {
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid call = Sid.generate(Sid.Type.CALL);
        final URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        final String method = "GET";
        final Notification.Builder builder = Notification.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setCallSid(call);
        builder.setApiVersion("2012-04-24");
        builder.setLog(0);
        builder.setErrorCode(11100);
        builder.setMoreInfo(url);
        builder.setMessageText("hello world!");
        final DateTime now = DateTime.now();
        builder.setMessageDate(now);
        builder.setRequestUrl(url);
        builder.setRequestMethod(method);
        builder.setRequestVariables("hello world!");
        builder.setResponseHeaders("hello world!");
        builder.setResponseBody("hello world!");
        builder.setUri(url);
        Notification notification = builder.build();
        final NotificationsDao notifications = manager.getNotificationsDao();
        // Create a new notification in the data store.
        notifications.addNotification(notification);
        // Read the notification from the data store.
        assertTrue(notifications.getNotificationsByMessageDate(now).size() == 1);
        // Delete the notification.
        notifications.removeNotification(sid);
        // Validate that the notification was removed.
        assertTrue(notifications.getNotification(sid) == null);
    }
}

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
package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface NotificationsDao {
    void addNotification(Notification notification);

    Notification getNotification(Sid sid);

    List<Notification> getNotifications(Sid accountSid);

    List<Notification> getNotificationsByCall(Sid callSid);

    List<Notification> getNotificationsByLogLevel(int logLevel);

    List<Notification> getNotificationsByMessageDate(DateTime messageDate);

    void removeNotification(Sid sid);

    void removeNotifications(Sid accountSid);

    void removeNotificationsByCall(Sid callSid);
}

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
package org.restcomm.connect.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.NotificationsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.NotificationList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.http.converter.NotificationConverter;
import org.restcomm.connect.http.converter.NotificationListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.identity.AuthType;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public abstract class NotificationsEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected NotificationsDao dao;
    protected Gson gson;
    protected XStream xstream;

    public NotificationsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        dao = storage.getNotificationsDao();
        final NotificationConverter converter = new NotificationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Notification.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new NotificationListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Response getNotification(final String accountSid, final String sid, final MediaType responseType) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Read:Notifications", AuthType.AuthToken);
        final Notification notification = dao.getNotification(new Sid(sid));
        if (notification == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(operatedAccount, notification.getAccountSid(), SecuredType.SECURED_STANDARD);
            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(notification), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(notification);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
    }

    protected Response getNotifications(final String accountSid, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Read:Notifications", AuthType.AuthToken);
        final List<Notification> notifications = dao.getNotifications(new Sid(accountSid));
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(notifications), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new NotificationList(notifications));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }
}

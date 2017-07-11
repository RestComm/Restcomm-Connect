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
import java.text.ParseException;
import java.util.ArrayList;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.http.converter.NotificationConverter;
import org.restcomm.connect.http.converter.NotificationListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.NotificationsDao;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.NotificationList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.NotificationFilter;

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
    protected NotificationListConverter listConverter;
    protected String instanceId;

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
        listConverter = new NotificationListConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Notification.class, converter);
        builder.registerTypeAdapter(NotificationList.class, listConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new NotificationListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        xstream.registerConverter(listConverter);

        instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
    }

    protected Response getNotification(final String accountSid, final String sid, final MediaType responseType) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Read:Notifications");
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

    protected Response getNotifications(final String accountSid, UriInfo info, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Read:Notifications");

        boolean localInstanceOnly = true;
        try {
            String localOnly = info.getQueryParameters().getFirst("localOnly");
            if (localOnly != null && localOnly.equalsIgnoreCase("false"))
                localInstanceOnly = false;
        } catch (Exception e) {
        }

        // shall we include sub-accounts cdrs in our query ?
        boolean querySubAccounts = false; // be default we don't
        String querySubAccountsParam = info.getQueryParameters().getFirst("SubAccounts");
        if (querySubAccountsParam != null && querySubAccountsParam.equalsIgnoreCase("true"))
            querySubAccounts = true;

        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");
        String startTime = info.getQueryParameters().getFirst("StartTime");
        String endTime = info.getQueryParameters().getFirst("EndTime");
        String error_code = info.getQueryParameters().getFirst("ErrorCode");
        String request_url = info.getQueryParameters().getFirst("RequestUrl");
        String message_text = info.getQueryParameters().getFirst("MessageText");

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page.equals("0")) ? 0
                : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer.parseInt(pageSize));

        // Shall we query cdrs of sub-accounts too ?
        // if we do, we need to find the sub-accounts involved first
        List<String> ownerAccounts = null;
        if (querySubAccounts) {
            ownerAccounts = new ArrayList<String>();
            ownerAccounts.add(accountSid); // we will also return parent account cdrs
            ownerAccounts.addAll(accountsDao.getSubAccountSidsRecursive(new Sid(accountSid)));
        }

        NotificationFilter filterForTotal;

        try {

            if (localInstanceOnly) {
                filterForTotal = new NotificationFilter(accountSid, ownerAccounts, startTime, endTime, error_code, request_url,
                        message_text, null, null);
            } else {
                filterForTotal = new NotificationFilter(accountSid, ownerAccounts, startTime, endTime, error_code, request_url,
                        message_text, null, null, instanceId);
            }
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final int total = dao.getTotalNotification(filterForTotal);

        if (Integer.parseInt(page) > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        NotificationFilter filter;

        try {
            if (localInstanceOnly) {
                filter = new NotificationFilter(accountSid, ownerAccounts, startTime, endTime, error_code, request_url,
                        message_text, limit, offset);
            } else {
                filter = new NotificationFilter(accountSid, ownerAccounts, startTime, endTime, error_code, request_url,
                        message_text, limit, offset, instanceId);
            }
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final List<Notification> cdrs = dao.getNotifications(filter);

        listConverter.setCount(total);
        listConverter.setPage(Integer.parseInt(page));
        listConverter.setPageSize(Integer.parseInt(pageSize));
        listConverter.setPathUri(info.getRequestUri().getPath());

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new NotificationList(cdrs));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(new NotificationList(cdrs)), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }
}

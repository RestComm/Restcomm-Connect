/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.connect.notification;

import java.net.URI;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.NotificationsDao;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.Sid;



/**
 *
 * @author charles.roufay
 */
public class GlobalNotification {


    public static int getERROR_NOTIFICATION() {
        return ERROR_NOTIFICATION;
    }

    public static int getWARNING_NOTIFICATION() {
        return WARNING_NOTIFICATION;
    }
    private static final int WARNING_NOTIFICATION = 1;
    private static final int ERROR_NOTIFICATION = 0;
    private final Sid accountId = new Sid("ACae6e420f425248d6a26948c17a9e2acf");

    public GlobalNotification(Configuration configuration, DaoManager storage) {
        this.runtime = configuration.subset("runtime-settings");
        this.apiVersion = runtime.getString("api-version");
        this.storage = storage;
    }
    private final String apiVersion;
    private final Configuration runtime;
    private final DaoManager storage;
    private Notification notification = null;
    private NotificationsDao notifications = null;
        // application data.
    //private HttpRequestDescriptor request;
    //private HttpResponseDescriptor response;
    //notifications.addNotification(notification);
    private URI uri = URI.create("http://documentation.telestax.com");
    public void sendNotification(final int errorWarningCode, final int apiErrorCode, final String errorMessage) {
        notification = notification(errorWarningCode, apiErrorCode, errorMessage, uri,"","","");
        notifications = storage.getNotificationsDao();
        notifications.addNotification(notification);
    }
       protected Notification notification(final int errorWarningCode, final int apiErrorCode,
               final String errorMessage,
               final URI requestUrl,
               final String requestMethod,
               final String requestVariables,
               final String responseHeaders) {

        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(accountId);
        builder.setApiVersion(apiVersion);
        builder.setLog(errorWarningCode);
        builder.setErrorCode(apiErrorCode);
        final String base = runtime.getString("error-dictionary-uri");
        StringBuilder buffer = new StringBuilder();
        buffer.append(base);
        if (!base.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append(apiErrorCode).append(".html");
        final URI info = URI.create(buffer.toString());
        builder.setMoreInfo(info);
        builder.setMessageText(errorMessage);
        final DateTime now = DateTime.now();
        builder.setMessageDate(now);
        builder.setRequestUrl(requestUrl);
        builder.setRequestMethod(requestMethod);
        builder.setRequestVariables(requestVariables);
        builder.setResponseHeaders(responseHeaders);
        buffer = new StringBuilder();
        buffer.append("/").append(apiVersion).append("/Accounts/");
        buffer.append(accountId.toString()).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
         }

}

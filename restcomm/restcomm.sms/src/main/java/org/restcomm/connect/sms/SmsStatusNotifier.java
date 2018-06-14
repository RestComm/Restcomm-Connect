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
package org.restcomm.connect.sms;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.http.client.Downloader;
import org.restcomm.connect.http.client.HttpRequestDescriptor;
import org.restcomm.connect.interpreter.RCConfProvider;
import org.restcomm.connect.interpreter.RCDaoManagerProvider;
import static org.restcomm.connect.sms.SmsService.WARNING_NOTIFICATION;

public class SmsStatusNotifier extends RestcommUntypedActor {

    private static final String MSG_STATUS_PARAM = "MessageStatus";
    private static final String MSG_ID_PARAM = "MessageSid";
    private static final String ERROR_CODE_PARAM = "ErrorCode";
    private static final String ACCOUT_SID_PARAM = "AccountSid";
    private static final String FROM_PARAM = "From";
    private static final String TO_PARAM = "To";
    private static final String BODY_PARAM = "Body";


    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);


    private List<NameValuePair>  populateReqParams (SmsMessage message) {
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(FROM_PARAM, message.getSender()));
        parameters.add(new BasicNameValuePair(TO_PARAM, message.getRecipient()));
        parameters.add(new BasicNameValuePair(BODY_PARAM, message.getBody()));
        parameters.add(new BasicNameValuePair(ACCOUT_SID_PARAM, message.getAccountSid().toString()));
        //TODO pass error code
        //parameters.add(new BasicNameValuePair(ERROR_CODE_PARAM, message.getError().toString));
        parameters.add(new BasicNameValuePair(MSG_ID_PARAM, message.getSid().toString()));
        parameters.add(new BasicNameValuePair(MSG_STATUS_PARAM, message.getStatus().toString()));
        parameters.add(new BasicNameValuePair(BODY_PARAM, message.getBody()));
        return parameters;
    }
    // The storage engine.
    private void notifyStatus(SmsMessage message) {
        URI callback = message.getStatusCallback();
        if (callback != null) {
            String method = message.getStatusCallbackMethod();
            if (method != null && !method.isEmpty()) {

                if (!org.apache.http.client.methods.HttpGet.METHOD_NAME.equalsIgnoreCase(method)
                        && !org.apache.http.client.methods.HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
                    final Notification notification = notification(WARNING_NOTIFICATION, 14104, method
                            + " is not a valid HTTP method for <Sms>", message);
                    DaoManager storage = RCDaoManagerProvider.extensionProvider.get(getContext().system()).getBean();
                    storage.getNotificationsDao().addNotification(notification);
                    method = org.apache.http.client.methods.HttpPost.METHOD_NAME;
                }
            } else {
                method = org.apache.http.client.methods.HttpPost.METHOD_NAME;
            }


            List<NameValuePair> parameters = populateReqParams(message);
            HttpRequestDescriptor request = new HttpRequestDescriptor(callback, method, parameters);
            downloader().tell(request, self());

        }
    }

    private Notification notification(final int log, final int error, final String message, SmsMessage entity) {
        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(entity.getAccountSid());
        builder.setCallSid(entity.getSid());
        builder.setApiVersion(entity.getApiVersion());
        builder.setLog(log);
        builder.setErrorCode(error);
        Configuration conf = RCConfProvider.extensionProvider.get(getContext().system()).getConf();
        String base = conf.subset("runtime-settings").getString("error-dictionary-uri");
        try {
            base = UriUtils.resolve(new URI(base)).toString();
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException when trying to resolve Error-Dictionary URI: " + e);
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(base);
        if (!base.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append(error).append(".html");
        final URI info = URI.create(buffer.toString());
        builder.setMoreInfo(info);
        builder.setMessageText(message);
        final DateTime now = DateTime.now();
        builder.setMessageDate(now);
        buffer = new StringBuilder();
        buffer.append("/").append(entity.getApiVersion()).append("/Accounts/");
        buffer.append(entity.getAccountSid().toString()).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
    }

    protected ActorRef downloader() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Downloader();
            }
        });
        return getContext().actorOf(props);
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof SmsMessage) {
            SmsMessage message = (SmsMessage) o;
            notifyStatus(message);
        }

    }
}

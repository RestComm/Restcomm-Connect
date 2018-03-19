/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.restcomm.connect.commons.push;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import com.google.gson.Gson;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.common.http.CustomHttpClientBuilder;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import scala.concurrent.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public class PushNotificationServerHelper {

    private static final Logger logger = Logger.getLogger(PushNotificationServerHelper.class);

    private final HttpClient httpClient = CustomHttpClientBuilder.buildDefaultClient(RestcommConfiguration.getInstance().getMain());

    private final ExecutionContext dispatcher;

    private final boolean pushNotificationServerEnabled;

    private String pushNotificationServerUrl;

    private long pushNotificationServerDelay;


    public PushNotificationServerHelper(final ActorSystem actorSystem, final Configuration configuration) {
        this.dispatcher = actorSystem.dispatchers().lookup("restcomm-blocking-dispatcher");

        final Configuration runtime = configuration.subset("runtime-settings");
        this.pushNotificationServerEnabled = runtime.getBoolean("push-notification-server-enabled", false);
        if (this.pushNotificationServerEnabled) {
            this.pushNotificationServerUrl = runtime.getString("push-notification-server-url");
            this.pushNotificationServerDelay = runtime.getLong("push-notification-server-delay");
        }
    }

    public long sendPushNotificationIfNeeded(final String pushClientIdentity) {
        if (!pushNotificationServerEnabled || pushClientIdentity == null) {
            return 0;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Push server notification to client with identity: '" + pushClientIdentity + "' added to queue.");
        }
        Futures.future(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Map<String, String> params = new HashMap<>();
                params.put("Identity", pushClientIdentity);
                HttpPost httpPost = new HttpPost(pushNotificationServerUrl);
                try {
                    httpPost.setEntity(new StringEntity(new Gson().toJson(params), ContentType.APPLICATION_JSON));
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sending push server notification to client with identity: " + pushClientIdentity);
                    }
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        logger.warn("Error while sending push server notification to client with identity: " + pushClientIdentity + ", response: " + httpResponse.getEntity());
                    }
                } catch (Exception e) {
                    logger.error("Exception while sending push server notification, " + e);
                } finally {
                    httpPost.releaseConnection();
                }
                return null;
            }
        }, dispatcher);

        return pushNotificationServerDelay;
    }
}

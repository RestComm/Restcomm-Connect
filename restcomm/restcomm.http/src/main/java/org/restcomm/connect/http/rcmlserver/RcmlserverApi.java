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

package org.restcomm.connect.http.rcmlserver;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicNameValuePair;
import org.restcomm.connect.commons.common.http.CustomHttpClientBuilder;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.configuration.sets.RcmlServerConfigurationSet;
import org.restcomm.connect.commons.util.SecurityUtils;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.entities.Account;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverApi {
    enum NotificationType {
        accountRemoved
    }

    URI apiUrl;
    MainConfigurationSet mainConfig;

    public RcmlserverApi(MainConfigurationSet mainConfig, RcmlServerConfigurationSet rcmlserverConfig) {
        try {
            apiUrl = UriUtils.resolve(new URI(rcmlserverConfig.getBaseUrl()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.mainConfig = mainConfig;
    }

    /**
     * Notifies the application server (RVD usually) that an account is about to be closed.
     * The request is made in an asynchronous way.
     *
     * @param accountToBeRemoved
     */
    public Future<HttpResponse> notifyAccountRemovalAsync(Account accountToBeRemoved, String notifierUsername, String notifierPassword) {
        CloseableHttpAsyncClient client = CustomHttpClientBuilder.buildAsync(mainConfig);

        try {
            client.start();
            HttpPost request = new HttpPost(apiUrl + "/notifications");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("type", NotificationType.accountRemoved.toString()));
            nvps.add(new BasicNameValuePair("accountSid", accountToBeRemoved.getSid().toString()));
            String authHeader;
            if (notifierUsername != null) {
                authHeader = SecurityUtils.buildBasicAuthHeader(notifierUsername, notifierPassword);
            } else {
                authHeader = SecurityUtils.buildBasicAuthHeader(accountToBeRemoved.getSid().toString(), accountToBeRemoved.getAuthToken());
            }
            request.setHeader("Authorization", authHeader );
            request.setEntity(new UrlEncodedFormEntity(nvps));
            Future<HttpResponse> future = client.execute(request, null);
            return future;
        } catch ( UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace(); // TODO handle this in a better way
            }
        }
    }

}

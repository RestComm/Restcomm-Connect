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
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverApi {
    static final Logger logger = Logger.getLogger(RcmlserverApi.class.getName());

    enum NotificationType {
        accountRemoved
    }

    URI apiUrl;
    MainConfigurationSet mainConfig;

    public RcmlserverApi(MainConfigurationSet mainConfig, RcmlServerConfigurationSet rcmlserverConfig) {
        try {
            // resolve() should be run lazily to work. Make sure this constructor is invoked after the JBoss connectors have been set up.
            apiUrl = UriUtils.resolve(new URI(rcmlserverConfig.getBaseUrl()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.mainConfig = mainConfig;
    }

    public RcmlserverApi(MainConfigurationSet mainConfig, URI apiUrl) {
        this.mainConfig = mainConfig;
        this.apiUrl = apiUrl;
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
            //client. // TODO !!!
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace(); // TODO handle this in a better way
            }
        }
    }

    /**
     * Transmits account-removal notifications to the application server in an asynchronous way
     * 
     * @param closedAccounts
     * @param notifierUsername
     * @param notifierPassword
     * @return
     */
    public Thread notifyAccountsRemovalAsync(final List<Account> closedAccounts, final String notifierUsername, final String notifierPassword) {
        // first, create the batch of requests
        String notificationUrl = apiUrl + "/notifications";
        final List<HttpPost> requests = new ArrayList<HttpPost>();
        final List<String> accountSids = new ArrayList<String>();
        for (Account closedAccount : closedAccounts) {
            HttpPost request = new HttpPost(notificationUrl);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("type", NotificationType.accountRemoved.toString()));
            nvps.add(new BasicNameValuePair("accountSid", closedAccount.getSid().toString()));
            String authHeader;
            if (notifierUsername != null) {
                authHeader = SecurityUtils.buildBasicAuthHeader(notifierUsername, notifierPassword);
            } else {
                authHeader = SecurityUtils.buildBasicAuthHeader(closedAccount.getSid().toString(), closedAccount.getAuthToken());
            }
            request.setHeader("Authorization", authHeader );
            try {
                request.setEntity(new UrlEncodedFormEntity(nvps));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e); // crappy input
            }
            requests.add(request);
            accountSids.add(closedAccount.getSid().toString());
        }
        // Start an async thread and send notifications tom rcml server
        // No data is shared between the async thread and current thread.
        if (requests.size() > 0) {
            final CloseableHttpAsyncClient httpclient = CustomHttpClientBuilder.buildAsync(mainConfig);
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        httpclient.start();
                        final CountDownLatch latch = new CountDownLatch(requests.size());
                        for (int i = 0; i < requests.size(); i ++) {
                            final int index = i; // use a final variable that is accessibly from the asynchronous handler below
                            final HttpPost request = requests.get(i);
                            httpclient.execute(request, new FutureCallback<HttpResponse>() {
                                @Override
                                public void completed(final HttpResponse response) {
                                    latch.countDown();
                                    System.out.println("Account-closing notification for " + accountSids.get(index) + " successfully sent - " + response.getStatusLine());
                                }
                                @Override
                                public void failed(final Exception ex) {
                                    latch.countDown();
                                    System.out.println("Account-closing notification for " + accountSids.get(index) + " failed - " + ex.getMessage());
                                }
                                @Override
                                public void cancelled() {
                                    latch.countDown();
                                    System.out.println("Account-closing notification for " + accountSids.get(index) + " cancelled");
                                }
                            });
                        }
                        latch.await();
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted");
                    }
                    System.out.println("Asyncronous account-closing notification thread ended" );
                }

            });
            System.out.println("Starting asyncronous account-closing notification thread - " + t.getId());
            t.start();
            return t;
        }
        return null; // something went wrong or no accounts to iterate over
    }

}

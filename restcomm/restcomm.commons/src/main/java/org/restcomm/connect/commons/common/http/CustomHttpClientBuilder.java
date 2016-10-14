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

package org.restcomm.connect.commons.common.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.restcomm.connect.commons.HttpConnector;
import org.restcomm.connect.commons.HttpConnectorList;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.util.UriUtils;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class CustomHttpClientBuilder {

    private CustomHttpClientBuilder() {
        // TODO Auto-generated constructor stub
    }

    public static HttpClient build(MainConfigurationSet config) {
        int timeoutConnection = config.getResponseTimeout();
        return build(config, timeoutConnection);
    }

    public static HttpClient build(MainConfigurationSet config, int timeout) {
        SslMode mode = config.getSslMode();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        if ( mode == SslMode.strict ) {
            return  HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        } else {
            return buildAllowallClient(requestConfig);
        }
    }

    public static CloseableHttpAsyncClient buildAsync(MainConfigurationSet config) {
        SslMode mode = config.getSslMode();
        int timeoutConnection = config.getResponseTimeout();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutConnection)
                .setConnectionRequestTimeout(timeoutConnection)
                .setSocketTimeout(timeoutConnection)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        if ( mode == SslMode.strict ) {
            return HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
        } else {
            return buildAllowallAsyncClient(requestConfig);
        }
    }

    private static HttpClient buildAllowallClient(RequestConfig requestConfig) {
        HttpConnectorList httpConnectorList = UriUtils.getHttpConnectorList();
        HttpClient httpClient = null;
        //Enable SSL only if we have HTTPS connector
        List<HttpConnector> connectors = httpConnectorList.getConnectors();
        Iterator<HttpConnector> iterator = connectors.iterator();
        while (iterator.hasNext()) {
            HttpConnector connector = iterator.next();
            if (connector.isSecure()) {
                SSLConnectionSocketFactory sslsf;
                try {
                    SSLContextBuilder builder = new SSLContextBuilder();
                    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                    sslsf = new SSLConnectionSocketFactory(builder.build());
                    httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslsf).build();
                } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                    throw new RuntimeException("Error creating HttpClient", e);
                }
                break;
            }
        }
        if (httpClient == null) {
            httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        }

        return httpClient;
    }

    private static CloseableHttpAsyncClient buildAllowallAsyncClient(RequestConfig requestConfig) {
        HttpConnectorList httpConnectorList = UriUtils.getHttpConnectorList();
        CloseableHttpAsyncClient httpClient = null;
        //Enable SSL only if we have HTTPS connector
        List<HttpConnector> connectors = httpConnectorList.getConnectors();
        Iterator<HttpConnector> iterator = connectors.iterator();
        while (iterator.hasNext()) {
            HttpConnector connector = iterator.next();
            if (connector.isSecure()) {
                SSLConnectionSocketFactory sslsf;
                try {
                    SSLContextBuilder builder = new SSLContextBuilder();
                    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                    SSLContext sslContext = builder.build();
                    httpClient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).setSSLContext(sslContext).build(); // TODO use of SSLContext is not really tested
                } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                    throw new RuntimeException("Error creating HttpClient", e);
                }
                break;
            }
        }
        if (httpClient == null) {
            httpClient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
        }

        return httpClient;
    }
}

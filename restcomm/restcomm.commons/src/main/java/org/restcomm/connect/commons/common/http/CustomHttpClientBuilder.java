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
import org.apache.http.ssl.SSLContextBuilder;
import org.restcomm.connect.commons.HttpConnector;
import org.restcomm.connect.commons.HttpConnectorList;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.util.UriUtils;

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
            SSLConnectionSocketFactory sslsf = null;
            try {
                sslsf = new SSLConnectionSocketFactory(
                        SSLContextBuilder.create().build(),
                        getSSLPrototocolsFromSystemProperties(),
                        null,
//                        new String[]{"TLS_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_256_CBC_SHA"},
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Error creating HttpClient", e);
            }
            return  HttpClients.custom().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslsf).build();
        } else {
            return buildAllowallClient(requestConfig);
        }
    }

    private static String[] getSSLPrototocolsFromSystemProperties() {
        String protocols = System.getProperty("jdk.tls.client.protocols");
        if (protocols == null)
            protocols = System.getProperty("https.protocols");

        if (protocols != null) {
            String[] protocolsArray = protocols.split(",");
            return protocolsArray;
        }
        return null;
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
//                    sslsf = new SSLConnectionSocketFactory(builder.build());

                    sslsf = new SSLConnectionSocketFactory(
                            builder.build(),
                            getSSLPrototocolsFromSystemProperties(),
                            null,
                            SSLConnectionSocketFactory.getDefaultHostnameVerifier());


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
}

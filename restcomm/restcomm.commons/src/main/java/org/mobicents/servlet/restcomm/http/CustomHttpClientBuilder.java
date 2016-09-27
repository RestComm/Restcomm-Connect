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

package org.mobicents.servlet.restcomm.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class CustomHttpClientBuilder {
        // Logger.
    private static Logger logger = Logger.getLogger(CustomHttpClientBuilder.class.getName());
    private CustomHttpClientBuilder() {
    }


    public static HttpClient build(MainConfigurationSet config) {
        SslMode mode = config.getSslMode();
        int timeoutConnection = config.getResponseTimeout();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutConnection)
                .setConnectionRequestTimeout(timeoutConnection)
                .setSocketTimeout(timeoutConnection)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        if ( mode == SslMode.strict ) {
            return  HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        } else {
            return buildAllowallClient(requestConfig);
        }
    }


    private static CloseableHttpClient buildAllowallClient(RequestConfig requestConfig) {
        String[] protocols = getSSLPrototocolsFromSystemProperties();
        //SSLContext sslcontext = SSLContexts.createDefault();
        SSLContext sslcontext = null ;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            if (logger.isInfoEnabled()) {
                logger.error("Exception during the http client creation, problem creating the SSL Context: "+ e.getStackTrace());
            }
        }
        if (sslcontext != null) {
            // Allow All versions of TLS set in the -Dhttps.protocols
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, protocols, null, new NoopHostnameVerifier());
            CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslsf).build();
            return httpclient;
        } else {
            return null;
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
}

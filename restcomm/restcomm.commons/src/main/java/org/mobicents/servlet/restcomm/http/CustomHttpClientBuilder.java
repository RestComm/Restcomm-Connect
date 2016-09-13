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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

/**
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class CustomHttpClientBuilder {
        // Logger.
    private static Logger logger = Logger.getLogger(CustomHttpClientBuilder.class.getName());
    private CustomHttpClientBuilder() {
        // TODO Auto-generated constructor stub
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
            //return buildAllowallClient();
        }
    }


    private static CloseableHttpClient buildAllowallClient(RequestConfig requestConfig) {
        String[] protocols = getSSLPrototocolsFromSystemProperties();
        //SSLContext sslcontext = SSLContexts.createDefault();
        SSLContext sslcontext;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, protocols, null, new NoopHostnameVerifier());
       // CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
       CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslsf).build();
        return httpclient;
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

/**
        private static HttpClient buildAllowallClient(RequestConfig requestConfig) {
        HttpConnectorList httpConnectorList = UriUtils.getHttpConnectorList();
        HttpClient httpClient = null;
        //Enable SSL only if we have HTTPS connector
        List<HttpConnector> connectors = httpConnectorList.getConnectors();
        Iterator<HttpConnector> iterator = connectors.iterator();

                        while (iterator.hasNext()){
                    HttpConnector elemCon = iterator.next();
                logger.error("***https connectors content ***" +"getAddress : " + elemCon.getAddress() +"getPort : " + elemCon.getPort() + "getScheme : " + elemCon.getScheme()
                        );
                }
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
    * **/
}

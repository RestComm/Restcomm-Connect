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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mobicents.servlet.restcomm.HttpConnector;
import org.mobicents.servlet.restcomm.HttpConnectorList;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.util.UriUtils;

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
        SslMode mode = config.getSslMode();
        if ( mode == SslMode.strict )
            return new DefaultHttpClient();
        else
            return buildAllowallClient();
    }

    /**
     * Builds the default http client without taking into account system configuration
     * @return The newly created http client
     */
    public static HttpClient buildDefault() {
        return new DefaultHttpClient();
    }

    private static HttpClient buildAllowallClient() {
        HttpConnectorList httpConnectorList = UriUtils.getHttpConnectorList();
        HttpClient httpClient = new DefaultHttpClient();
        //Enable SSL only if we have HTTPS connector
        List<HttpConnector> connectors = httpConnectorList.getConnectors();
        Iterator<HttpConnector> iterator = connectors.iterator();
        while (iterator.hasNext()) {
            HttpConnector connector = iterator.next();
            if (connector.isSecure()) {
                SSLSocketFactory sslsf;
                try {
                    sslsf = new SSLSocketFactory(new TrustStrategy() {
                        public boolean isTrusted(
                            final X509Certificate[] chain, String authType) throws CertificateException {
                            // Oh, I am easy...
                            return true;
                        }
                    });
                } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
                    throw new RuntimeException("Error creating HttpClient", e);
                }
                httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme(connector.getScheme(), connector.getPort(), sslsf));
                break;
            }
        }

        return httpClient;
    }

}

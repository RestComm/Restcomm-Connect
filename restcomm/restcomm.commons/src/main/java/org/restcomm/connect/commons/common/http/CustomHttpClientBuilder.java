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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;

/**
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class CustomHttpClientBuilder {

    private CustomHttpClientBuilder() {
    }

    private static CloseableHttpClient defaultClient = null;
    private static CloseableHttpAsyncClient closeableHttpAsyncClient = null;

    public static synchronized void stopDefaultClient() {
        if (defaultClient != null) {
            HttpClientUtils.closeQuietly(defaultClient);
            defaultClient = null;
        }
        if (closeableHttpAsyncClient != null) {
            try {
                if(closeableHttpAsyncClient.isRunning())
                    closeableHttpAsyncClient.close();
            } catch (IOException e) {
            }
            closeableHttpAsyncClient = null;
        }
    }

    public static synchronized CloseableHttpClient buildDefaultClient(MainConfigurationSet config) {
        if (defaultClient == null) {
            defaultClient = build(config);
        }
        return defaultClient;
    }

    public static synchronized CloseableHttpAsyncClient buildCloseableHttpAsyncClient(MainConfigurationSet config) {
        if (closeableHttpAsyncClient == null) {
            closeableHttpAsyncClient = buildAsync(config);
            closeableHttpAsyncClient.start();
        }
        return closeableHttpAsyncClient;
    }

    public static CloseableHttpClient build(MainConfigurationSet config) {
        int timeoutConnection = config.getResponseTimeout();
        return build(config, timeoutConnection);
    }

    private static CloseableHttpAsyncClient buildAsync(MainConfigurationSet config) {
        int timeoutConnection = config.getResponseTimeout();
        return buildAsync(config, timeoutConnection);
    }

    private static CloseableHttpAsyncClient buildAsync(MainConfigurationSet config, int timeout) {
        HttpAsyncClientBuilder builder = HttpAsyncClients.custom();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(config.getDefaultHttpConnectionRequestTimeout())
                .setSocketTimeout(timeout)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        builder.setDefaultRequestConfig(requestConfig);

        SslMode mode = config.getSslMode();
        SSLIOSessionStrategy sessionStrategy = null;

        if (mode == SslMode.strict) {
            sessionStrategy = buildStrictSSLIOSessionStrategy();
        } else {
            sessionStrategy = buildAllowallSSLIOSessionStrategy();
        }
        builder.setSSLStrategy(sessionStrategy);

        builder.setMaxConnPerRoute(config.getDefaultHttpMaxConnsPerRoute());
        builder.setMaxConnTotal(config.getDefaultHttpMaxConns());
        //builder.setConnectionTimeToLive(config.getDefaultHttpTTL(), TimeUnit.MILLISECONDS);
        if (config.getDefaultHttpRoutes() != null
                && config.getDefaultHttpRoutes().size() > 0) {
            Registry<SchemeIOSessionStrategy> reg = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", sessionStrategy)
                    .build();
            try {
                final PoolingNHttpClientConnectionManager poolingmgr = new PoolingNHttpClientConnectionManager(
                        new DefaultConnectingIOReactor(),
                        null,
                        reg,
                        null,
                        null,
                        config.getDefaultHttpTTL(),
                        TimeUnit.MILLISECONDS);
                //ensure conn configuration is set again for new conn manager
                poolingmgr.setMaxTotal(config.getDefaultHttpMaxConns());
                poolingmgr.setDefaultMaxPerRoute(config.getDefaultHttpMaxConnsPerRoute());
                for (InetSocketAddress addr : config.getDefaultHttpRoutes().keySet()) {
                    HttpRoute r = new HttpRoute(new HttpHost(addr.getHostName(), addr.getPort()));
                    poolingmgr.setMaxPerRoute(r, config.getDefaultHttpRoutes().get(addr));
                }
                builder.setConnectionManager(poolingmgr);
            } catch (IOReactorException e) {
                throw new RuntimeException("Error creating CloseableHttpAsyncClient", e);
            }
        }
        return builder.build();
    }

    private static CloseableHttpClient build(MainConfigurationSet config, int timeout) {
        HttpClientBuilder builder = HttpClients.custom();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(config.getDefaultHttpConnectionRequestTimeout())
                .setSocketTimeout(timeout)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        builder.setDefaultRequestConfig(requestConfig);

        SslMode mode = config.getSslMode();
        SSLConnectionSocketFactory sslsf = null;
        if (mode == SslMode.strict) {
            sslsf = buildStrictFactory();
        } else {
            sslsf = buildAllowallFactory();
        }
        builder.setSSLSocketFactory(sslsf);

        builder.setMaxConnPerRoute(config.getDefaultHttpMaxConnsPerRoute());
        builder.setMaxConnTotal(config.getDefaultHttpMaxConns());
        builder.setConnectionTimeToLive(config.getDefaultHttpTTL(), TimeUnit.MILLISECONDS);
        if (config.getDefaultHttpRoutes() != null
                && config.getDefaultHttpRoutes().size() > 0) {
            if (sslsf == null) {
                //strict mode with no system https properties
                //taken from apache buider code
                PublicSuffixMatcher publicSuffixMatcherCopy = PublicSuffixMatcherLoader.getDefault();
                DefaultHostnameVerifier hostnameVerifierCopy = new DefaultHostnameVerifier(publicSuffixMatcherCopy);
                sslsf = new SSLConnectionSocketFactory(
                        SSLContexts.createDefault(),
                        hostnameVerifierCopy);
            }
            Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslsf)
                    .build();
            final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
                    reg,
                    null,
                    null,
                    null,
                    config.getDefaultHttpTTL(),
                    TimeUnit.MILLISECONDS);
            //ensure conn configuration is set again for new conn manager
            poolingmgr.setMaxTotal(config.getDefaultHttpMaxConns());
            poolingmgr.setDefaultMaxPerRoute(config.getDefaultHttpMaxConnsPerRoute());
            for (InetSocketAddress addr : config.getDefaultHttpRoutes().keySet()) {
                HttpRoute r = new HttpRoute(new HttpHost(addr.getHostName(), addr.getPort()));
                poolingmgr.setMaxPerRoute(r, config.getDefaultHttpRoutes().get(addr));
            }
            builder.setConnectionManager(poolingmgr);
        }
        return builder.build();
    }

    private static String[] getSSLPrototocolsFromSystemProperties() {
        String protocols = System.getProperty("jdk.tls.client.protocols");
        if (protocols == null) {
            protocols = System.getProperty("https.protocols");
        }

        if (protocols != null) {
            String[] protocolsArray = protocols.split(",");
            return protocolsArray;
        }
        return null;
    }

    private static SSLConnectionSocketFactory buildStrictFactory() {
        try {
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    SSLContextBuilder.create().build(),
                    getSSLPrototocolsFromSystemProperties(),
                    null,
                    //                        new String[]{"TLS_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_256_CBC_SHA"},
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            return sslsf;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating HttpClient", e);
        }
    }

    private static SSLConnectionSocketFactory buildAllowallFactory() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    builder.build(),
                    getSSLPrototocolsFromSystemProperties(),
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            return sslsf;
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Error creating HttpClient", e);
        }
    }

    private static SSLIOSessionStrategy buildStrictSSLIOSessionStrategy(){
        try {
            SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(
                    SSLContextBuilder.create().build(),
                    getSSLPrototocolsFromSystemProperties(),
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            return sessionStrategy;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating HttpAsycClient", e);
        }
    }

    private static SSLIOSessionStrategy buildAllowallSSLIOSessionStrategy(){
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());

            SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(
                    builder.build(),
                    getSSLPrototocolsFromSystemProperties(),
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            return sessionStrategy;
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Error creating HttpAsycClient", e);
        }
    }
}

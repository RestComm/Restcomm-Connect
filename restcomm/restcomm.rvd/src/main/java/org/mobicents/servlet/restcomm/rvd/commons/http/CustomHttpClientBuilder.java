package org.mobicents.servlet.restcomm.rvd.commons.http;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;


/**
 * Creates an HttpClient with the desired ssl behaviour according to configuration
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */

public class CustomHttpClientBuilder {

    private CustomHttpClientBuilder() {
    }

    public static CloseableHttpClient buildHttpClient() {
        SslMode sslMode = RvdConfiguration.getInstance().getSslMode();
        if ( sslMode == SslMode.strict ) {
            return buildStrictClient();
        }
        else
            return buildAllowallClient();
    }

    private static CloseableHttpClient buildStrictClient() {
        String[] protocols = getSSLPrototocolsFromSystemProperties();
        if (protocols == null)
            return HttpClients.createDefault();

        SSLContext sslcontext = SSLContexts.createDefault();
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, protocols, null, new DefaultHostnameVerifier());
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        return httpclient;
    }

    private static CloseableHttpClient buildAllowallClient() {
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
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

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

}

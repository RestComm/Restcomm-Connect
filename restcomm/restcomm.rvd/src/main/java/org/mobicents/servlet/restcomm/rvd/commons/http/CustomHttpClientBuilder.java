package org.mobicents.servlet.restcomm.rvd.commons.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
        if ( sslMode == SslMode.strict )
            return HttpClients.createDefault();
        else
            return buildAllowallClient();
    }

    private static CloseableHttpClient buildAllowallClient() {
        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslsf;
        try {
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        sslsf = new SSLConnectionSocketFactory(builder.build(),SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch ( NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException(e); // there is not much to do here.
        }
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        return httpclient;
    }

}

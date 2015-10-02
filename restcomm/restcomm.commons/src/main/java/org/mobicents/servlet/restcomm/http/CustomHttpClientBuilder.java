package org.mobicents.servlet.restcomm.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;


public class CustomHttpClientBuilder {

    private CustomHttpClientBuilder() {
        // TODO Auto-generated constructor stub
    }


    public static HttpClient buildHttpClient(MainConfigurationSet config) {
        SslMode mode = config.getSslMode();
        if ( mode == SslMode.strict )
            return new DefaultHttpClient();
        else
            return buildAllowallClient();
    }

    private static HttpClient buildAllowallClient() {
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
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sslsf));
        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 8443, sslsf));
        // TODO what happens with custom https ports ? Only 443 and 8443 are covered here.

        return httpClient;
    }

}

package org.restcomm.connect.rvd.commons.http;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;


import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.restcomm.connect.rvd.RvdConfiguration;

import javax.net.ssl.SSLContext;


/**
 * Creates an HttpClient with the desired ssl behaviour according to configuration
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */

public class CustomHttpClientBuilder {
    private SslMode sslMode;

    public CustomHttpClientBuilder(RvdConfiguration configuration) {
        this.sslMode = configuration.getSslMode();
    }

    // returns an apache http client
    public CloseableHttpClient buildHttpClient() {
        if ( sslMode == SslMode.strict ) {
            return buildStrictClient();
        }
        else
            return buildAllowallClient();
    }

    // returns a jersey client - experimental
    /*
    public static Client buildJerseyHttpClient() {
        SslMode sslMode = RvdConfiguration.getInstance().getSslMode();
        if ( sslMode == SslMode.strict ) {
            return buildStrictJerseyClient();
        }
        else
            return buildAllowallJerseyClient();
    }
    */

    private CloseableHttpClient buildStrictClient() {
        String[] protocols = getSSLPrototocolsFromSystemProperties();
        if (protocols == null)
            return HttpClients.createDefault();

        SSLContext sslcontext = SSLContexts.createDefault();
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, protocols, null, new DefaultHostnameVerifier());
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        return httpclient;
    }

    private CloseableHttpClient buildAllowallClient() {
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

    private String[] getSSLPrototocolsFromSystemProperties() {
        String protocols = System.getProperty("jdk.tls.client.protocols");
        if (protocols == null)
            protocols = System.getProperty("https.protocols");

        if (protocols != null) {
            String[] protocolsArray = protocols.split(",");
            return protocolsArray;
        }
        return null;
    }

    // experimental support for Jersey http client
/*
    private static Client buildStrictJerseyClient() {
        return Client.create();
    }

    private static Client buildAllowallJerseyClient() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {}

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
             return null;
            }
        }};

        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        }, ctx));

        Client client = Client.create(config);
        return client;
    }
    */

}

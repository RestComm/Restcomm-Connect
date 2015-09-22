package org.mobicents.servlet.restcomm.identity;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakClient.KeycloakClientException;

/**
 * All api calls to restcomm-identity proxy are defined here
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class RestcommIdentityApi {
    protected Logger logger = Logger.getLogger(RestcommIdentityApi.class);

    private String tokenString;
    private IdentityConfigurator configurator;

    public RestcommIdentityApi(final IdentityContext identityContext, final IdentityConfigurator configurator) {
        // TODO Auto-generated constructor stub
        tokenString = identityContext.getOauthTokenString();
        if (tokenString == null)
            throw new IllegalStateException("No oauth token in context.");
        this.configurator = configurator;
    }

    public boolean inviteUser(String username) {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(configurator.getIdentityProxyUrl() + "/api/instances/" + configurator.getIdentityInstanceId() + "/users/" + username + "/invite");
            request.addHeader("Authorization", "Bearer " + tokenString);
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    logger.error("Error inviting user '" + username + "' to instance '" + configurator.getIdentityInstanceId() + "' - " + response.getStatusLine().toString());
                    return false;
                } else
                    return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (KeycloakClientException e1) {
            throw new RuntimeException(e1);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private CloseableHttpClient buildHttpClient() throws KeycloakClientException {
        // TODO - use a proper certificate on identity.restcomm.com instead of a self-signed one
        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslsf;
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(),SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new KeycloakClientException(e);
        }
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        return httpclient;
    }

}

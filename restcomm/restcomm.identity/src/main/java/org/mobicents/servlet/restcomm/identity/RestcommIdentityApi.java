package org.mobicents.servlet.restcomm.identity;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;

import com.google.gson.Gson;

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
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() >= 300) {
                logger.error("Error inviting user '" + username + "' to instance '" + configurator.getIdentityInstanceId() + "' - " + response.getStatusLine().toString());
                return false;
            } else
                return true;
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Outcome createUser(UserEntity user) {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(configurator.getIdentityProxyUrl() + "/api/users");
            request.addHeader("Authorization", "Bearer " + tokenString);
            request.addHeader("Content-Type", "application/json");

            Gson gson = new Gson();
            request.setEntity(new StringEntity(gson.toJson(user)));
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 409)
                return Outcome.CONFLICT;
            if (response.getStatusLine().getStatusCode() >= 300) {
                logger.error("Error creating user '" + user.username + "' - " + response.getStatusLine().toString());
                return Outcome.FAILED;
            } else
                return Outcome.OK;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private CloseableHttpClient buildHttpClient() throws Exception {
        // TODO - use a proper certificate on identity.restcomm.com instead of a self-signed one
        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslsf;
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        sslsf = new SSLConnectionSocketFactory(builder.build(),SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        return httpclient;
    }

    public static class UserEntity {
        String username;
        String email;
        String firstname;
        String lastname;
        String password;
        //List<String> memberOf; // instanceIds of restcomm instances User is a member of

        public UserEntity(String username, String email, String firstname, String lastname, String password) {
            super();
            this.username = username;
            this.email = email;
            this.firstname = firstname;
            this.lastname = lastname;
            this.password = password;
        }
        public UserEntity() {
            super();
        }
    }

}

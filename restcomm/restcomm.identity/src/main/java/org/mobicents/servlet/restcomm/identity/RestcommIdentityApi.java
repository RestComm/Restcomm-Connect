package org.mobicents.servlet.restcomm.identity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.endpoints.Outcome;

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
    //private IdentityConfigurator configurator;
    private String authServerBaseUrl;
    private String username;
    private String identityInstanceId;
    private String realm;


    public RestcommIdentityApi(String authurl, String username, String password, String realm, String instanceId) {
        this.authServerBaseUrl = authurl;
        this.realm = realm;
        this.tokenString = retrieveTokenString(username, password);
        if (tokenString == null)
            throw new IllegalStateException("No oauth token in context. Could not retrieve token for user '" + username + "'");
        this.username = username;
        this.identityInstanceId = instanceId;
    }

    public RestcommIdentityApi(final IdentityContext identityContext, final IdentityConfigurationSet imConfig, final MutableIdentityConfigurationSet iConfig) {
        tokenString = identityContext.getOauthTokenString();
        if (tokenString == null)
            throw new IllegalStateException("No oauth token in context.");
        this.authServerBaseUrl = imConfig.getAuthServerUrl();
        this.identityInstanceId = iConfig.getInstanceId();
        this.realm = imConfig.getRealm();
        AccessToken accessToken = identityContext.getOauthToken();
        if (accessToken == null)
            throw new IllegalStateException("Missing oauth token from identity context");
        this.username = identityContext.getOauthToken().getPreferredUsername();
    }

    public void bindInstance(String instanceId) {
        this.identityInstanceId = instanceId;
    }

    public String getBoundInstanceId() {
        return identityInstanceId;
    }

    public String getTokenString() {
        return tokenString;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    public String retrieveTokenString(String username, String password) {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(IdentityConfigurationSet.getAuthServerUrl(authServerBaseUrl)).path(ServiceUrlConstants.TOKEN_PATH).build(realm));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, IdentityConfigurationSet.IDENTITY_PROXY_CLIENT_NAME));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200 || entity == null) {
                return null;
            }
            // store the new token in the cache too for future uses (in the same request)
            AccessTokenResponse token = JsonSerialization.readValue(entity.getContent(), AccessTokenResponse.class);
            return token.getToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Grants user 'username' access to this instance. Default instance-specific roles are assigned.
     * Currently logged user token is used to access restcomm-identity proxy.
     *
     * @param username
     * @return
     */
    public boolean inviteUser(String username) {
        if (this.identityInstanceId == null)
            throw new IllegalStateException("No identity instance id is set");

        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(IdentityConfigurationSet.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances/" + this.identityInstanceId + "/users/" + username + "/invite");
            request.addHeader("Authorization", "Bearer " + tokenString);
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() >= 300) {
                logger.error("Error inviting user '" + username + "' to instance '" + this.identityInstanceId + "' - " + response.getStatusLine().toString());
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
            HttpPost request = new HttpPost(IdentityConfigurationSet.getIdentityProxyUrl(authServerBaseUrl) + "/api/users");
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

    public Outcome dropUser(String username) {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpDelete request = new HttpDelete(IdentityConfigurationSet.getIdentityProxyUrl(authServerBaseUrl) + "/api/users/" + username);
            request.addHeader("Authorization", "Bearer " + tokenString);

            HttpResponse response = client.execute(request);
            return Outcome.fromHttpStatus(response.getStatusLine().getStatusCode());
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

    public CreateInstanceResponse createInstance(String[] redirectUris, String secret) throws RestcommIdentityApiException {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(IdentityConfigurationSet.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances");
            request.addHeader("Authorization", "Bearer " + tokenString);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("prefix",redirectUris[0])); // TODO - make sure all the items of the array (not only the first) are passed and also check the receiving side
            params.add(new BasicNameValuePair("secret",secret));
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                Gson gson = new Gson();
                CreateInstanceResponse createdInstance = gson.fromJson( new InputStreamReader(response.getEntity().getContent()), CreateInstanceResponse.class);
                return createdInstance;
            } else {
                //logger.error("Error creating instance for user '" + username + "'");
                throw new RestcommIdentityApiException(Outcome.fromHttpStatus(status));
            }
        } catch ( IOException e) {
            throw new RestcommIdentityApiException(Outcome.INTERNAL_ERROR);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Outcome dropInstance(String instanceId) {
        CloseableHttpClient client = null;
        try {
            client = buildHttpClient();
            HttpDelete request = new HttpDelete(IdentityConfigurationSet.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances/" + instanceId);
            request.addHeader("Authorization", "Bearer " + tokenString);

            HttpResponse response = client.execute(request);
            return Outcome.fromHttpStatus(response.getStatusLine().getStatusCode());
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

    private CloseableHttpClient buildHttpClient() {
        // TODO - use a proper certificate on identity.restcomm.com instead of a self-signed one
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

    public static class CreateInstanceResponse {
        public String instanceId;
    }

    public static class RestcommIdentityApiException extends Exception {
        Outcome outcome;

        public RestcommIdentityApiException(Outcome outcome) {
            super();
            this.outcome = outcome;
        }

        public Outcome getOutcome() {
            return outcome;
        }

    }

}

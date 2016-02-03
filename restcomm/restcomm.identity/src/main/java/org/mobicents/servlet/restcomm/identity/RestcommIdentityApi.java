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

package org.mobicents.servlet.restcomm.identity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSetImpl;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.endpoints.Outcome;

import com.google.gson.Gson;
import org.mobicents.servlet.restcomm.http.CustomHttpClientBuilder;

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

    public RestcommIdentityApi(final UserIdentityContext userIdentityContext, final IdentityConfigurationSet imConfig, final MutableIdentityConfigurationSet iConfig) {
        tokenString = userIdentityContext.getOauthTokenString();
        if (tokenString == null)
            throw new IllegalStateException("No oauth token in context.");
        this.authServerBaseUrl = imConfig.getAuthServerBaseUrl();
        this.identityInstanceId = iConfig.getInstanceId();
        this.realm = imConfig.getRealm();
        AccessToken accessToken = userIdentityContext.getOauthToken();
        if (accessToken == null)
            throw new IllegalStateException("Missing oauth token from identity context");
        this.username = userIdentityContext.getOauthToken().getPreferredUsername();
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
        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(IdentityConfigurationSetImpl.getAuthServerUrl(authServerBaseUrl)).path(ServiceUrlConstants.TOKEN_PATH).build(realm));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, IdentityConfigurationSetImpl.IDENTITY_PROXY_CLIENT_NAME));
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
        }
    }

    /**
     * Grants user access to this instance. In keycloak terms,  instance-specific roles are assigned.
     * Currently logged user token is used to access restcomm-identity proxy.
     *
     * @param username
     * @return
     */
    public Outcome inviteUser(String username) {
        if (this.identityInstanceId == null)
            throw new IllegalStateException("No identity instance id is set");

        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(IdentityConfigurationSetImpl.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances/" + this.identityInstanceId + "/users/" + username + "/invite");
            request.addHeader("Authorization", "Bearer " + tokenString);
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() >= 300) {
                logger.error("Error inviting user '" + username + "' to instance '" + this.identityInstanceId + "' - " + response.getStatusLine().toString());
                return Outcome.FAILED;
            } else
                return Outcome.OK;
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }

    public Outcome createUser(UserEntity user) {
        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(IdentityConfigurationSetImpl.getIdentityProxyUrl(authServerBaseUrl) + "/api/users");
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
        }
    }

    public Outcome dropUser(String username) {
        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpDelete request = new HttpDelete(IdentityConfigurationSetImpl.getIdentityProxyUrl(authServerBaseUrl) + "/api/users/" + username);
            request.addHeader("Authorization", "Bearer " + tokenString);

            HttpResponse response = client.execute(request);
            return Outcome.fromHttpStatus(response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CreateInstanceResponse createInstance(String[] redirectUris, String secret) throws RestcommIdentityApiException {
        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpPost request = new HttpPost(IdentityConfigurationSetImpl.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances");
            request.addHeader("Authorization", "Bearer " + tokenString);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            for ( String redirectUri : redirectUris)
                params.add(new BasicNameValuePair("prefix",redirectUri));
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
                throw new RestcommIdentityApiException("Error creating instance. Identity proxy responded with: " + status, Outcome.fromHttpStatus(status));
            }
        } catch ( IOException e) {
            throw new RestcommIdentityApiException(Outcome.INTERNAL_ERROR);
        }
    }

    public Outcome dropInstance(String instanceId) {
        HttpClient client = null;
        try {
            client = buildHttpClient();
            HttpDelete request = new HttpDelete(IdentityConfigurationSetImpl.getIdentityProxyUrl(authServerBaseUrl) + "/api/instances/" + instanceId);
            request.addHeader("Authorization", "Bearer " + tokenString);

            HttpResponse response = client.execute(request);
            return Outcome.fromHttpStatus(response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }






    private HttpClient buildHttpClient() {
        return CustomHttpClientBuilder.buildDefault();
    }

    public static class UserEntity {
        String username;
        String email;
        String firstname;
        String lastname;
        String password;
        //List<String> memberOf; // instanceIds of restcomm instances User is a member of

        // keep this constructor disabled until we figure out how to treat the email param (currently the identity-proxy does not save it)
        private UserEntity(String username, String email, String firstname, String lastname, String password) {
            super();
            this.username = username;
            this.email = email;
            this.firstname = firstname;
            this.lastname = lastname;
            this.password = password;
        }

        public UserEntity(String username, String firstname, String lastname, String password) {
            this.username = username;
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

        public RestcommIdentityApiException(String message, Outcome outcome) {
            super(message);
            this.outcome = outcome;
        }

        public Outcome getOutcome() {
            return outcome;
        }

    }

}

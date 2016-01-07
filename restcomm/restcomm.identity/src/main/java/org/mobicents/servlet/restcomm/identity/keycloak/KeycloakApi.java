package org.mobicents.servlet.restcomm.identity.keycloak;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.http.CustomHttpClientBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by otsakir on 12/24/15.
 */
public class KeycloakApi {
    private String baseUrl;
    private String token;

    public KeycloakApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean connect(String username, String password, String clientApp, String realm ) {
        this.token = KeycloakApi.getToken(username, password, clientApp, realm, baseUrl);
        if (this.token != null)
            return true;
        return false;
    }

    public Outcome createRealm(String realmJson) {
        if (this.token == null)
            throw new IllegalStateException("No token found. Make sure you succesfully run connect() before this.");
        HttpClient client = null;
        try {
            client = CustomHttpClientBuilder.buildDefault();
            HttpPost request = new HttpPost(baseUrl + "/auth/admin/realms/");
            request.addHeader("Authorization", "Bearer " + token);
            request.addHeader("Content-Type", "application/json");

            Gson gson = new Gson();
            request.setEntity(new StringEntity(realmJson));
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 409)
                return Outcome.CONFLICT;
            if (response.getStatusLine().getStatusCode() >= 300) {
                return Outcome.FAILED;
            } else
                return Outcome.OK;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Outcome dropRealm(String realm) {
        if (this.token == null)
            throw new IllegalStateException("No token found. Make sure you succesfully run connect() before this.");
        HttpClient client = null;
        try {
            client = CustomHttpClientBuilder.buildDefault();
            HttpDelete request = new HttpDelete(baseUrl + "/auth/admin/realms/" + realm);
            request.addHeader("Authorization", "Bearer " + token);
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 409)
                return Outcome.CONFLICT;
            if (response.getStatusLine().getStatusCode() >= 300) {
                return Outcome.FAILED;
            } else
                return Outcome.OK;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getToken(String username, String password, String clientApp, String realm, String baseUrl) {
        HttpClient client = null;
        try {
            client = CustomHttpClientBuilder.buildDefault();
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(baseUrl + "/auth").path(ServiceUrlConstants.TOKEN_PATH).build(realm));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientApp));
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

}

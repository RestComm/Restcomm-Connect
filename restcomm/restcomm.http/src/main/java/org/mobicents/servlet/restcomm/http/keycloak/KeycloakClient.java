package org.mobicents.servlet.restcomm.http.keycloak;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.HostUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.UriUtils;
import org.mobicents.servlet.restcomm.http.keycloak.entities.ResetPasswordEntity;

import com.google.gson.Gson;

public class KeycloakClient {
    private Logger logger = Logger.getLogger(KeycloakClient.class);

    // Some static configuration options. We should move them to a configuration file at some point.
    static final String REALM = "restcomm"; // the name of the keycloak realm to use for administrative tasks
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "password";
    static final String ADMINISTRATION_APPLICATION = "admin-client"; // the name of the oauth application that carries out administrative tasks
    static final String KEYCKLOAD_URL_ORIGIN = "https://identity.restcomm.com";

    private AccessTokenResponse cachedToken;
    private HttpServletRequest request; // Store the HTTP request for cleaner API. Remember, KeycloakClient lifecycle follows endpoint lifecycle.

    public KeycloakClient(HttpServletRequest request) {
        super();
        if (request == null)
            throw new RuntimeException("Assertion failed. request is null.");
        this.request = request;
    }

    // Retrieves an access token from keycloak and caches it
    private AccessTokenResponse getToken() throws KeycloakClientException {
        // first check the token cache
        if ( cachedToken != null )
            return cachedToken;
        // looks like we'll have to ask keycloak for a token
        HttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getBaseUrl() + "/auth")
                    .path(ServiceUrlConstants.TOKEN_PATH).build(REALM));
            List <NameValuePair> formparams = new ArrayList <NameValuePair>();
            formparams.add(new BasicNameValuePair("username", ADMIN_USERNAME));
            formparams.add(new BasicNameValuePair("password", ADMIN_PASSWORD));
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, ADMINISTRATION_APPLICATION));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                //String json = getContent(entity);
                throw new KeycloakClientOauthException();
            }
            if (entity == null) {
                throw new KeycloakClientOauthException();
            }
            String json = getContent(entity);
            // store the new token in the cache too for future uses (in the same request)
            cachedToken = JsonSerialization.readValue(json, AccessTokenResponse.class);
            return cachedToken;
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void logout() throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getBaseUrl() + "/auth")
                    .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .build(REALM));
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, res.getRefreshToken()));
            formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, ADMINISTRATION_APPLICATION));
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = client.execute(post);
            boolean status = response.getStatusLine().getStatusCode() != 204;
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return;
            }
            InputStream is = entity.getContent();
            if (is != null) is.close();
            if (status) {
                throw new RuntimeException("failed to logout");
            }
        } catch (IOException e) {
            throw new KeycloakClientException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void updateUser(String username, UserRepresentation user) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            //e.g. PUT http://login.restcomm.com:8081/auth/admin/realms/restcomm/users/otsakir
            HttpPut putRequest = new HttpPut(getBaseUrl() + "/auth/admin/realms/"+REALM+"/users/"+username);
            putRequest.addHeader("Authorization", "Bearer " + res.getToken());
            putRequest.addHeader("Content-Type","application/json");

            //UserRepresentation user = toUserRepresentation(userData);
            Gson gson = new Gson();
            String json_user = gson.toJson(user);
            StringEntity stringBody = new StringEntity(json_user,"UTF-8");
            putRequest.setEntity(stringBody);
            try {
                HttpResponse response = client.execute(putRequest);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (UnsupportedEncodingException e1) {
            throw new KeycloakClientException();
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    public void createUser(String username, UserRepresentation user) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            //e.g. PUT http://login.restcomm.com:8081/auth/admin/realms/restcomm/users/otsakir
            HttpPost postRequest = new HttpPost(getBaseUrl() + "/auth/admin/realms/"+REALM+"/users");
            postRequest.addHeader("Authorization", "Bearer " + res.getToken());
            postRequest.addHeader("Content-Type","application/json");

            Gson gson = new Gson();
            String json_user = gson.toJson(user);
            StringEntity stringBody = new StringEntity(json_user,"UTF-8");
            postRequest.setEntity(stringBody);
            try {
                HttpResponse response = client.execute(postRequest);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (UnsupportedEncodingException e1) {
            throw new KeycloakClientException();
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    public void resetUserPassword(String username, String password, boolean temporary) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            //e.g. PUT http://login.restcomm.com:8081/auth/admin/realms/restcomm/users/paparas/reset-password
            HttpPut putRequest = new HttpPut(getBaseUrl() + "/auth/admin/realms/"+REALM+"/users/"+username+"/reset-password");
            putRequest.addHeader("Authorization", "Bearer " + res.getToken());
            putRequest.addHeader("Content-Type","application/json");

            ResetPasswordEntity resetPass = new ResetPasswordEntity();
            resetPass.setType("password");
            resetPass.setValue(password);
            resetPass.setTemporary(temporary);
            Gson gson = new Gson();
            String json = gson.toJson(resetPass);
            StringEntity stringBody = new StringEntity(json,"UTF-8");
            putRequest.setEntity(stringBody);
            try {
                HttpResponse response = client.execute(putRequest);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (UnsupportedEncodingException e1) {
            throw new KeycloakClientException();
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    public List<RoleRepresentation> getRealmRoles() throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(getBaseUrl() + "/auth/admin/realms/"+REALM+"/roles");
            get.addHeader("Authorization", "Bearer " + res.getToken());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, TypedList.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public UserRepresentation getUserInfo(String username) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(getBaseUrl() + "/auth/admin/realms/" + REALM + "/users/" + username);
            get.addHeader("Authorization", "Bearer " + res.getToken());
            try {
                HttpResponse response = client.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                try {
                    return JsonSerialization.readValue(is, UserRepresentation.class);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void addUserRoles(String username, List<RoleRepresentation> keycloakRoles) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            //e.g. POST  login.restcomm.com:8081/auth/admin/realms/restcomm/users/account2%40gmail.com/role-mappings/realm
            HttpPost postRequest = new HttpPost(getBaseUrl() + "/auth/admin/realms/"+REALM+"/users/"+username+"/role-mappings/realm");
            postRequest.addHeader("Authorization", "Bearer " + res.getToken());
            postRequest.addHeader("Content-Type","application/json");

            Gson gson = new Gson();
            String json = gson.toJson(keycloakRoles);
            StringEntity stringBody = new StringEntity(json,"UTF-8");
            postRequest.setEntity(stringBody);
            try {
                HttpResponse response = client.execute(postRequest);
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new KeycloakClientException(response.getStatusLine().getStatusCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (UnsupportedEncodingException e1) {
            throw new KeycloakClientException();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void setUserRoles(String username, List<String> appliedRoles) throws KeycloakClientException {
        List<RoleRepresentation> availableKeycloakRoles = getRealmRoles();
        List<RoleRepresentation> addedKeycloakRoles = new ArrayList<RoleRepresentation>();
        for (String roleName: appliedRoles) {
            RoleRepresentation keycloakRole = KeycloakHelpers.getRoleByName(roleName, availableKeycloakRoles);
            //
            if ( keycloakRole == null )  {
                logger.warn("Cannot add role " + roleName + ". It does not exist in the realm");
            } else {
                addedKeycloakRoles.add( keycloakRole );
            }
        }
        if (addedKeycloakRoles.size() > 0) {
            addUserRoles(username, addedKeycloakRoles);
        }
    }


    // Returns the URL origin where keycloak lies. First tries from a configuration value that is explicitly defined. It falls back to geting the value from the request assuming that keycloak runs side by side with restcomm.
    public String getBaseUrl() {
        if ( KEYCKLOAD_URL_ORIGIN != null && !KEYCKLOAD_URL_ORIGIN.equals("") )
            return KEYCKLOAD_URL_ORIGIN;

        String useHostname = request.getServletContext().getInitParameter("useHostname");
        if (useHostname != null && "true".equalsIgnoreCase(useHostname)) {
            return "http://" + HostUtils.getHostName() + ":8080";
        } else {
            return UriUtils.getOrigin(request.getRequestURL().toString());
        }
    }

    public static String getContent(HttpEntity entity) throws IOException {
        if (entity == null) return null;
        InputStream is = entity.getContent();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            String data = new String(bytes);
            return data;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {

            }
        }
    }

    static class TypedList extends ArrayList<RoleRepresentation> {
    }

    public static class KeycloakClientException extends Exception {
        private Integer httpStatusCode;
        public KeycloakClientException() {}
        public KeycloakClientException(Integer status) {
            this.httpStatusCode = status;
        }
        public KeycloakClientException(Throwable cause) {
            super(cause);
        }
        public Integer getHttpStatusCode() {
            return httpStatusCode;
        }
    }

    // throw when part of the Oauth negotiation with keycloak fails
    public static class KeycloakClientOauthException extends KeycloakClientException {
    }

    public static class KeycloakUserNotFound extends Exception {

        public KeycloakUserNotFound(String message) {
            super(message);
        }
    }
}

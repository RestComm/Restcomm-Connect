package org.mobicents.servlet.restcomm.identity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
//import org.mobicents.servlet.restcomm.http.keycloak.KeycloakClient.KeycloakClientException;
//import org.mobicents.servlet.restcomm.http.keycloak.entities.ResetPasswordEntity;

import org.mobicents.servlet.restcomm.identity.entities.ResetPasswordEntity;

import com.google.gson.Gson;

/**
 * REST administration client for 'admin-client' keycloak application
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class KeycloakAdminClient extends KeycloakClient {

    static final String ADMINISTRATION_APPLICATION = "admin-client"; // the name of the oauth application that carries out administrative tasks

    public KeycloakAdminClient() {
        super();
    }

    public KeycloakAdminClient(String username, String password, String realm, String keycloakBaseUrl) {
        super(username, password, ADMINISTRATION_APPLICATION, realm, keycloakBaseUrl);
        applyConfiguration(); // !!! this should call KeycloakAdminClient::applyConfiguration()
        // TODO - validate KeycloakAdminClient specific values here
        // ...
    }

    // initializes client object from configuration taking into account existing values
    private void applyConfiguration() {
        // TODO
    }



    public void updateUser(String username, UserRepresentation user) throws KeycloakClientException {
        AccessTokenResponse res = getToken();
        HttpClient client = new DefaultHttpClient();
        try {
            //e.g. PUT http://login.restcomm.com:8081/auth/admin/realms/restcomm/users/otsakir
            HttpPut putRequest = new HttpPut(getBaseUrl() + "/auth/admin/realms/"+realm+"/users/"+username);
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
            HttpPost postRequest = new HttpPost(getBaseUrl() + "/auth/admin/realms/"+realm+"/users");
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
            HttpPut putRequest = new HttpPut(getBaseUrl() + "/auth/admin/realms/"+realm+"/users/"+username+"/reset-password");
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
            HttpGet get = new HttpGet(getBaseUrl() + "/auth/admin/realms/"+realm+"/roles");
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
            HttpGet get = new HttpGet(getBaseUrl() + "/auth/admin/realms/" + realm + "/users/" + username);
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
            HttpPost postRequest = new HttpPost(getBaseUrl() + "/auth/admin/realms/"+realm+"/users/"+username+"/role-mappings/realm");
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

    static class TypedList extends ArrayList<RoleRepresentation> {
    }
}

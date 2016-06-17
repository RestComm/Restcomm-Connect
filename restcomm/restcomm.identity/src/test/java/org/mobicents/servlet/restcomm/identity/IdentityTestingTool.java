/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class that handles setup/teardown of a keycloak instance.
 *
 * Requirements:
 *      - admin account in master realm
 *      - existence of an app/client with Direct Grants enabled
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class IdentityTestingTool {

    private String token;
    private String authServerBaseUrl;

    public IdentityTestingTool(String authServerBaseUrl, String adminUsername, String adminPassword) {
        this.authServerBaseUrl = authServerBaseUrl;
        this.token = getAdminToken(adminUsername, adminPassword);
    }

    private String getAdminToken(String adminUsername, String adminPassword) {
        if (this.token == null) {
            Client jerseyClient = Client.create();
            WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/realms/master/protocol/openid-connect/token");
            MultivaluedMap params = new MultivaluedMapImpl();
            params.add("username", adminUsername);
            params.add("password", adminPassword);
            params.add("grant_type", "password");
            params.add("client_id", "admin-cli"); // this is a default application in 'master' realm. It MUST be there
            String response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON).post(String.class, params);
            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
            String token = jsonResponse.getAsJsonPrimitive("access_token").getAsString();
            System.out.println("Using oauth Admin token: " + token);

            this.token = token;
        }
        return this.token;
    }

    public void importRealm(String resourceFileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourceFileName);
        String data = IOUtils.toString(inputStream,"UTF-8");

        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/admin/realms");
        String response = webResource.type(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + token).post(String.class, data);
    }

    public void dropRealm(String realmName) {
        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/admin/realms/" + realmName);
        webResource.header("Authorization", "Bearer " + token).delete();
    }

    /**
     * Check if a role is inside the mappings.
     *
     * @param element
     * @param role
     * @param type This should be "realm" or "client".
     * @return
     */
    public static boolean roleMappingsContainRole(JsonElement element, String role, String instanceId, String type) {
        if ("client".equals(type)) {
            JsonElement mappingsElement = element.getAsJsonObject().get("clientMappings").getAsJsonObject().get(instanceId + "-restcomm-rest").getAsJsonObject().get("mappings").getAsJsonArray();
            Assert.assertNotNull(mappingsElement);
            JsonArray mappingsArray = mappingsElement.getAsJsonArray();
            Assert.assertTrue(mappingsArray.size() > 0);
            boolean developerRoleFound = false;
            for (int i = 0; i < mappingsArray.size(); i++) {
                if (role.equals(mappingsArray.get(i).getAsJsonObject().get("name").getAsString()))
                    return true;
            }
            return false;
        }
        throw new NotImplementedException();
    }

    public String getTokenWithClientGrant(String clientId, String clientSecret, String realm) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(clientId, clientSecret));
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token");
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("grant_type", "client_credentials");
        String response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON).post(String.class, params);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        String token = jsonResponse.getAsJsonPrimitive("access_token").getAsString();
        //System.out.println("Using oauth token: " + token);

        return token;
    }

    public String getTokenWithDirectAccessGrant(String username, String password, String clientId, String clientSecret, String realm) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(clientId, clientSecret));
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token");
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("username", username);
        params.add("password", password);
        params.add("grant_type", "password");
        String response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON).post(String.class, params);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        String token = jsonResponse.getAsJsonPrimitive("access_token").getAsString();
        //System.out.println("Using oauth token: " + token);

        return token;
    }

    public String getIAT(String token, int expiration, int count, String realm) {
        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/admin/realms/" + realm + "/clients-initial-access");
        String data = "{\"expiration\":" + expiration + ",\"count\":" + count +"}";
        String response = webResource.header("Authorization", "Bearer " + token).type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).post(String.class, data);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        String iat = jsonResponse.getAsJsonPrimitive("token").getAsString();

        return iat;
    }

    public JsonObject getClient(String token, String clientId, String realm) {
        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(authServerBaseUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId);
        String response = webResource.header("Authorization", "Bearer " + token).accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonArray().get(0).getAsJsonObject();
        return jsonResponse;
    }

}

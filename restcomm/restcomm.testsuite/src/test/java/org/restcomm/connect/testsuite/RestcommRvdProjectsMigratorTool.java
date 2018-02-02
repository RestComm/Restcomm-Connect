/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.connect.testsuite;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * @author guilherme.jansen@telestax.com
 */
public class RestcommRvdProjectsMigratorTool {

    private static RestcommRvdProjectsMigratorTool instance;

    public static enum Endpoint {
        APPLICATIONS("Applications", ".json"),
        INCOMING_PHONE_NUMBERS("IncomingPhoneNumbers", ".json"),
        CLIENTS("Clients", ".json"),
        NOTIFICATIONS("Notifications", ".json"),
        ;
        private String name;
        private String extension;

        Endpoint(String name, String extension){
            this.name = name;
            this.extension = extension;
        }

        public String getName(){
            return this.name;
        }

        public String getExtension(){
            return this.extension;
        }
    }

    public static RestcommRvdProjectsMigratorTool getInstance() {
        if (instance == null) {
            instance = new RestcommRvdProjectsMigratorTool();
        }
        return instance;
    }

    public JsonArray getEntitiesList(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            Endpoint endpoint, String propertyName) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getEntitiesUrl(deploymentUrl, adminAccountSid, endpoint);
        WebTarget webResource = jerseyClient.target(url);
        String response = webResource.request(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse;
        if (propertyName == null) {
            jsonResponse = parser.parse(response).getAsJsonArray();
        } else {
            JsonObject jsonObject = parser.parse(response).getAsJsonObject();
            jsonResponse = jsonObject.get(propertyName).getAsJsonArray();
        }
        return jsonResponse;
    }

    public JsonArray getEntitiesList(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid, Endpoint endpoint) {
        return getEntitiesList(deploymentUrl, adminUsername, adminAuthToken, adminAccountSid, endpoint, null);
    }

    private String getEntitiesUrl(String deploymentUrl, String accountSid, Endpoint endpoint) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        String entitiesUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/" + endpoint.getName()
                + endpoint.getExtension();
        return entitiesUrl;
    }

    private String getEntityUrl(String deploymentUrl, String accountSid, Endpoint endpoint, String entitySid) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        String entityUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/" + endpoint.getName() + "/" + entitySid
                + endpoint.getExtension();
        return entityUrl;
    }

    public JsonObject getEntity(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String applicationSid, Endpoint endpoint) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getEntityUrl(deploymentUrl, adminAccountSid, endpoint, applicationSid);
        WebTarget webResource = jerseyClient.target(url);
        String response = null;
        JsonObject jsonResponse = null;
        try {
            response = webResource.request(MediaType.APPLICATION_JSON).get(String.class);
            JsonParser parser = new JsonParser();
            jsonResponse = parser.parse(response).getAsJsonObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return jsonResponse;
    }

}

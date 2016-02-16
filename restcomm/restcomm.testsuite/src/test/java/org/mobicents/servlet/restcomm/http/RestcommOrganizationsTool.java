/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author guilherme.jansen@telestax.com
 */
public class RestcommOrganizationsTool {

    private static RestcommOrganizationsTool instance;
    private static String organizationsUrl;

    public static RestcommOrganizationsTool getInstance() {
        if (instance == null) {
            instance = new RestcommOrganizationsTool();
        }
        return instance;
    }

    private String getOrganizationsUrl(String deploymentUrl, String accountSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        organizationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Organizations";
        if (!xml) {
            organizationsUrl += ".json";
        }
        return organizationsUrl;
    }

    private String getOrganizationUrl(String deploymentUrl, String accountSid, String organizationSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        organizationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Organizations/" + organizationSid;

        if (!xml) {
            organizationsUrl += ".json";
        }
        return organizationsUrl;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    public JsonObject createOrganization(String deploymentUrl, String adminAccountSid, String adminUsername,
            String adminAuthToken, MultivaluedMap<String, String> organizationParams) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getOrganizationsUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, organizationParams);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getOrganization(String deploymentUrl, String adminUsername, String adminAuthToken,
            String adminAccountSid, String organizationSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getOrganizationUrl(deploymentUrl, adminAccountSid, organizationSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = null;
        JsonObject jsonResponse = null;
        try {
            response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
            JsonParser parser = new JsonParser();
            jsonResponse = parser.parse(response).getAsJsonObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return jsonResponse;
    }

    public JsonArray getOrganizations(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getOrganizationsUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        return jsonResponse;
    }

    public JsonObject updateOrganization(String deploymentUrl, String adminUsername, String adminAuthToken,
            String adminAccountSid, String organizationSid, MultivaluedMap<String, String> organizationParams, boolean usePut) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getOrganizationUrl(deploymentUrl, adminAccountSid, organizationSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = "";
        if (usePut) {
            response = webResource.accept(MediaType.APPLICATION_JSON).put(String.class, organizationParams);
        } else {
            response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, organizationParams);
        }
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public void deleteOrganization(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String organizationSid) {
        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");
        String url = getOrganizationUrl("http://" + adminAccountSid + ":" + adminAuthToken + "@" + endpoint, adminAccountSid,
                organizationSid, false);
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));
        WebResource webResource = jerseyClient.resource(url);
        webResource.accept(MediaType.APPLICATION_JSON).delete();
    }
}

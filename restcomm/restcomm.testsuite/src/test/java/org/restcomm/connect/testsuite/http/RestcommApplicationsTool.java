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

package org.restcomm.connect.testsuite.http;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * @author guilherme.jansen@telestax.com
 */
public class RestcommApplicationsTool {

    private static RestcommApplicationsTool instance;
    private static String applicationsUrl;

    public static RestcommApplicationsTool getInstance() {
        if (instance == null) {
            instance = new RestcommApplicationsTool();
        }
        return instance;
    }

    private String getApplicationsUrl(String deploymentUrl, String accountSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        applicationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Applications";
        if (!xml) {
            applicationsUrl += ".json";
        }
        return applicationsUrl;
    }

    private String getApplicationUrl(String deploymentUrl, String accountSid, String applicationSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        applicationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid;

        if (!xml) {
            applicationsUrl += ".json";
        }
        return applicationsUrl;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    public JsonObject createApplication(String deploymentUrl, String adminAccountSid, String adminUsername,
            String adminAuthToken, MultivaluedMap<String, String> applicationParams) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getApplicationsUrl(deploymentUrl, adminAccountSid, false);
        WebTarget WebTarget = jerseyClient.target(url);
        String response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(applicationParams), String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getApplication(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String applicationSid) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getApplicationUrl(deploymentUrl, adminAccountSid, applicationSid, false);
        WebTarget WebTarget = jerseyClient.target(url);
        String response = null;
        JsonObject jsonResponse = null;
        try {
            response = WebTarget.request(MediaType.APPLICATION_JSON).get(String.class);
            JsonParser parser = new JsonParser();
            jsonResponse = parser.parse(response).getAsJsonObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return jsonResponse;
    }

    public JsonArray getApplications(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid) {
        return getApplications(deploymentUrl, adminUsername, adminAuthToken, adminAccountSid, false);
    }

    public JsonArray getApplications(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid, boolean includeNumbers) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getApplicationsUrl(deploymentUrl, adminAccountSid, false);
        WebTarget WebTarget = jerseyClient.target(url);
        if (includeNumbers)
            WebTarget = WebTarget.queryParam("includeNumbers", "true");
        String response = WebTarget.request(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        return jsonResponse;
    }

    public JsonObject updateApplication(String deploymentUrl, String adminUsername, String adminAuthToken,
            String adminAccountSid, String applicationSid, MultivaluedMap<String, String> applicationParams, boolean usePut) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));
        String url = getApplicationUrl(deploymentUrl, adminAccountSid, applicationSid, false);
        WebTarget WebTarget = jerseyClient.target(url);
        String response = "";
        if (usePut) {
            response = WebTarget.request(MediaType.APPLICATION_JSON).put(Entity.form(applicationParams), String.class);
        } else {
            response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(applicationParams), String.class);
        }
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public void deleteApplication(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String applicationSid) throws IOException {
        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");
        String url = getApplicationUrl("http://" + adminAccountSid + ":" + adminAuthToken + "@" + endpoint, adminAccountSid,
                applicationSid, false);
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminAccountSid, adminAuthToken));
        WebTarget WebTarget = jerseyClient.target(url);
        WebTarget.request(MediaType.APPLICATION_JSON).delete();
    }
}

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

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
public class RestcommGeolocationsTool {

    private static RestcommGeolocationsTool instance;
    private static String geolocationsUrl;

    public static RestcommGeolocationsTool getInstance() {
        if (instance == null) {
            instance = new RestcommGeolocationsTool();
        }
        return instance;
    }

    private String getGeolocationsUrl(String deploymentUrl, String accountSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        geolocationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Geolocation";
        if (!xml) {
            geolocationsUrl += ".json";
        }
        return geolocationsUrl;
    }

    private String getGeolocationUrl(String deploymentUrl, String accountSid, String geolocationSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        geolocationsUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Geolocation/" + geolocationSid;

        if (!xml) {
            geolocationsUrl += ".json";
        }
        return geolocationsUrl;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    public JsonObject createGeolocation(String deploymentUrl, String adminAccountSid, String adminUsername,
            String adminAuthToken, MultivaluedMap<String, String> geolocationParams) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getGeolocationsUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, geolocationParams);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getGeolocation(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String geolocationSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getGeolocationUrl(deploymentUrl, adminAccountSid, geolocationSid, false);
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

    public JsonArray getGeolocations(String deploymentUrl, String adminUsername, String adminAuthToken,
            String adminAccountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getGeolocationsUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        return jsonResponse;
    }

    public JsonObject updateGeolocation(String deploymentUrl, String adminUsername, String adminAuthToken,
            String adminAccountSid, String geolocationSid, MultivaluedMap<String, String> geolocationParams, boolean usePut) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getGeolocationUrl(deploymentUrl, adminAccountSid, geolocationSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = "";
        if (usePut) {
            response = webResource.accept(MediaType.APPLICATION_JSON).put(String.class, geolocationParams);
        } else {
            response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, geolocationParams);
        }
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public void deleteGeolocation(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String geolocationSid) throws IOException {
        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");
        String url = getGeolocationUrl("http://" + adminAccountSid + ":" + adminAuthToken + "@" + endpoint, adminAccountSid,
                geolocationSid, false);
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));
        WebResource webResource = jerseyClient.resource(url);
        webResource.accept(MediaType.APPLICATION_JSON).delete();
    }

}

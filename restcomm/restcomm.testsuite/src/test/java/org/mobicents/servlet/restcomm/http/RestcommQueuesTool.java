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
package org.mobicents.servlet.restcomm.http;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
public class RestcommQueuesTool {
    private static RestcommQueuesTool instance;
    private static String queuesUrl;
    private static Logger logger = Logger.getLogger(RestcommQueuesTool.class);

    public static RestcommQueuesTool getInstance() {
        if (instance == null) {
            instance = new RestcommQueuesTool();
        }
        return instance;
    }

    private String getQueuesUrl(String deploymentUrl, String accountSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        queuesUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Queues"+((xml)?"":".json");
        return queuesUrl;
    }

    private String getQueueUrl(String deploymentUrl, String accountSid, String queueSid, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        queuesUrl = deploymentUrl + "/2012-04-24/Accounts/" + accountSid + "/Queues" + ((xml)?"/":".json/")+ queueSid;
        
        return queuesUrl;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    public JsonObject createQueue(String deploymentUrl, String adminAccountSid, String adminUsername, String adminAuthToken,
            MultivaluedMap<String, String> queueParams) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getQueuesUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, queueParams);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getQueue(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String queueSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getQueueUrl(deploymentUrl, adminAccountSid, queueSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = null;
        JsonObject jsonResponse = null;
        try {
            response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
            JsonParser parser = new JsonParser();
            jsonResponse = parser.parse(response).getAsJsonObject();
        } catch (Exception e) {
            logger.info("Exception during getQueue : " + url + " exception: " + e);
        }
        return jsonResponse;
    }

    public JsonArray getQueues(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getQueuesUrl(deploymentUrl, adminAccountSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
        return jsonResponse;
    }

    public JsonObject updateQueue(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String queueSid, MultivaluedMap<String, String> queueParams) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        String url = getQueueUrl(deploymentUrl, adminAccountSid, queueSid, false);
        WebResource webResource = jerseyClient.resource(url);
        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, queueParams);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        return jsonResponse;
    }

    public void deleteQueue(String deploymentUrl, String adminUsername, String adminAuthToken, String adminAccountSid,
            String queueSid) throws IOException {
        String url = getQueueUrl(deploymentUrl, adminAccountSid,queueSid, true);
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));
        WebResource webResource = jerseyClient.resource(url);
        webResource.accept(MediaType.APPLICATION_JSON).delete();
    }

}

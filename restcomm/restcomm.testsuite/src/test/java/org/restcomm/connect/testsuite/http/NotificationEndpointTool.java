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
package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import javax.ws.rs.core.MultivaluedHashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:n.congvu@gmail.com">vunguyen</a>
 *
 */
public class NotificationEndpointTool {
    private static NotificationEndpointTool instance;
    private static String accountsUrl;
    private NotificationEndpointTool() {}

    public static NotificationEndpointTool getInstance() {
        if (instance == null)
            instance = new NotificationEndpointTool();
        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username, Boolean json) {
        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }

            accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Notifications" + ((json) ? ".json" : "");
        }

        return accountsUrl;
    }

    public JsonObject getNotificationList (String deploymentUrl, String username, String authToken) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));
        String url = getAccountsUrl(deploymentUrl, username, true);
        WebTarget WebTarget = jerseyClient.target(url);
        String response = WebTarget.request(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

    public JsonObject getNotificationList (String deploymentUrl, String username, String authToken, Integer page, Integer pageSize, Boolean json) {

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));
        String url = getAccountsUrl(deploymentUrl, username, true);
        WebTarget webTarget = jerseyClient.target(url);
        String response;

        if (page != null || pageSize != null) {
            MultivaluedMap<String, String> params = new MultivaluedHashMap();

            if (page != null) {
                params.add("Page", String.valueOf(page));
                webTarget.queryParam("Page", String.valueOf(page));
            }
            if (pageSize != null) {
                params.add("PageSize", String.valueOf(pageSize));
                webTarget.queryParam("PageSize", String.valueOf(pageSize));
            }

            response = webTarget.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                    .get(String.class);
        } else {
            response = webTarget.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

    public JsonObject getNotificationListUsingFilter(String deploymentUrl, String username, String authToken, Map<String, String> filters) {

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));
        String url = getAccountsUrl(deploymentUrl, username, true);
        WebTarget webTarget = jerseyClient.target(url);

        MultivaluedMap<String, String> params = new MultivaluedHashMap();

        for (String filterName : filters.keySet()) {
            String filterData = filters.get(filterName);
            params.add(filterName, filterData);
            webTarget = webTarget.queryParam(filterName, filterData);
        }

        String response = webTarget.request(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }
}

package org.mobicents.servlet.restcomm.http;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.mobicents.servlet.restcomm.entities.CallDetailRecordList;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommCallsTool {

    private static RestcommCallsTool instance;
    private static String accountsUrl;

    private RestcommCallsTool() {

    }

    public static RestcommCallsTool getInstance() {
        if (instance == null)
            instance = new RestcommCallsTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username, Boolean json) {
        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }

            accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Calls" + ((json) ? ".json" : "");
        }

        return accountsUrl;
    }

    public JsonObject getCalls(String deploymentUrl, String username, String authToken) {
        return (JsonObject) getCalls(deploymentUrl, username, authToken, null, null, true);
    }

    public JsonObject getCalls(String deploymentUrl, String username, String authToken, Integer page, Integer pageSize,
            Boolean json) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, json);

        WebResource webResource = jerseyClient.resource(url);

        String response = null;

        if (page != null || pageSize != null) {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();

            if (page != null)
                params.add("Page", String.valueOf(page));
            if (pageSize != null)
                params.add("PageSize", String.valueOf(pageSize));

            response = webResource.queryParams(params).accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                    .get(String.class);
        } else {
            response = webResource.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        }

        JsonParser parser = new JsonParser();

        if (json) {

            JsonObject jsonObject = parser.parse(response).getAsJsonObject();

            return jsonObject;
        } else {
            XStream xstream = new XStream();
            xstream.alias("cdrlist", CallDetailRecordList.class);
            JsonObject jsonObject = parser.parse(xstream.toXML(response)).getAsJsonObject();
            return jsonObject;
        }

    }

    public JsonObject getCallsUsingFilter(String deploymentUrl, String username, String authToken, Map<String, String> filters) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        for (String filterName : filters.keySet()) {
            String filterData = filters.get(filterName);
            params.add(filterName, filterData);
        }
        webResource = webResource.queryParams(params);
        String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }

    public JsonObject createCall(String deploymentUrl, String username, String authToken, String from, String to, String rcmlUrl) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("From", from);
        params.add("To", to);
        params.add("Url", rcmlUrl);

        // webResource = webResource.queryParams(params);
        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, params);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }

    public JsonObject modifyCall(String deploymentUrl, String username, String authToken, String callSid, String status,
            String rcmlUrl) throws Exception {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        if (status != null && rcmlUrl != null) {
            throw new Exception(
                    "You can either redirect a call using the \"url\" attribute or terminate it using the \"status\" attribute!");
        }
        if (status != null)
            params.add("Status", status);
        if (rcmlUrl != null)
            params.add("Url", rcmlUrl);

        String response = webResource.path(callSid).accept(MediaType.APPLICATION_JSON).post(String.class, params);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }

}

package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommUssdPushTool {

    private static RestcommUssdPushTool instance;
    private static String accountsUrl;

    private RestcommUssdPushTool() {

    }

    public static RestcommUssdPushTool getInstance() {
        if (instance == null)
            instance = new RestcommUssdPushTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username) {
        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }

            accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/UssdPush.json";
        }

        return accountsUrl;
    }

    public JsonObject createUssdPush(String deploymentUrl, String username, String authToken, String from, String to, String rcmlUrl) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username);

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
}

package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class OutboundProxyTool {

    private static OutboundProxyTool instance;
    private static String accountsUrl;

    private OutboundProxyTool() {

    }

    public static OutboundProxyTool getInstance() {
        if (instance == null)
            instance = new OutboundProxyTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username, Boolean json) {
        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }

            accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/OutboundProxy" + ((json) ? ".json" : "");
        }

        return accountsUrl;
    }

    public JsonObject getProxies(String deploymentUrl, String username, String authToken) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebResource webResource = jerseyClient.resource(url);

        String response = null;

        response = webResource.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }


    public JsonObject switchProxy(String deploymentUrl, String username, String authToken) {
        
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);
        
        WebResource webResource = jerseyClient.resource(url);

        String response = null;

        response = webResource.path("switchProxy").accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

    public JsonObject getActiveProxy(String deploymentUrl, String username, String authToken) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);
        
        WebResource webResource = jerseyClient.resource(url);

        String response = null;

        response = webResource.path("getActiveProxy").accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

}

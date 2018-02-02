package org.restcomm.connect.testsuite.http;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

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

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebTarget WebTarget = jerseyClient.target(url);

        String response = null;

        response = WebTarget.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }


    public JsonObject switchProxy(String deploymentUrl, String username, String authToken) {

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebTarget WebTarget = jerseyClient.target(url);

        String response = null;

        response = WebTarget.path("switchProxy").request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

    public JsonObject getActiveProxy(String deploymentUrl, String username, String authToken) {

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username, true);

        WebTarget WebTarget = jerseyClient.target(url);

        String response = null;

        response = WebTarget.path("getActiveProxy").request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();
        return jsonObject;
    }

}

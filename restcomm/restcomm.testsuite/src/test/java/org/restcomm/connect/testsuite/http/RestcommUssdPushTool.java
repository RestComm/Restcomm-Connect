package org.restcomm.connect.testsuite.http;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import javax.ws.rs.core.MultivaluedHashMap;

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

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username);

        WebTarget WebTarget = jerseyClient.target(url);

        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add("From", from);
        params.add("To", to);
        params.add("Url", rcmlUrl);

        // WebTarget = WebTarget.queryParams(params);
        String response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params),String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }
}

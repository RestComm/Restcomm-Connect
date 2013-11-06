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

public class RestcommAccountsTool {

    private static RestcommAccountsTool instance;
    private static String accountsUrl;

    private RestcommAccountsTool() {

    }

    public static RestcommAccountsTool getInstance() {
        if (instance == null)
            instance = new RestcommAccountsTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl) {
        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }
            accountsUrl = deploymentUrl + "/2012-04-24/Accounts.json";
        }

        return accountsUrl;
    }

    public JsonObject createAccount(String deploymentUrl, String adminUsername, String adminAuthToken, String emailAddress,
            String password) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getAccountsUrl(deploymentUrl);

        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("EmailAddress", emailAddress);
        params.add("Password", password);
        params.add("Role", "Administartor");

        String response = webResource.accept(MediaType.APPLICATION_JSON).post(String.class, params);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return jsonResponse;
    }

    public JsonObject getAccount(String deploymentUrl, String adminUsername, String adminAuthToken, String username) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        WebResource webResource = jerseyClient.resource(getAccountsUrl(deploymentUrl));

        String response = webResource.path(username).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return jsonResponse;
    }
}

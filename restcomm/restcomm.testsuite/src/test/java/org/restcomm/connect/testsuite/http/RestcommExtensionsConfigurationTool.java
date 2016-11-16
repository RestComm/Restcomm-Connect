package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommExtensionsConfigurationTool {
    private static Logger logger = Logger.getLogger(RestcommExtensionsConfigurationTool.class.getName());

    private static RestcommExtensionsConfigurationTool instance;
    private static String accountsUrl;

    private RestcommExtensionsConfigurationTool() {    }

    public static RestcommExtensionsConfigurationTool getInstance() {
        if (instance == null)
            instance = new RestcommExtensionsConfigurationTool();

        return instance;
    }

    private String getUrl(String deploymentUrl, Boolean xml) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }
            if(xml){
                accountsUrl = deploymentUrl + "/2012-04-24/ExtensionsConfiguration";
            } else {
                accountsUrl = deploymentUrl + "/2012-04-24/ExtensionsConfiguration.json";
            }

        return accountsUrl;
    }

    public JsonObject postConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> configurationParams) {
        return postConfiguration(deploymentUrl, adminUsername, adminAuthToken, configurationParams, false);
    }

    public JsonObject postConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> configurationParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = jerseyClient.resource(url);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, configurationParams);
        jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionName) {
        return getConfiguration(deploymentUrl, adminUsername, adminAuthToken, extensionName, false);
    }

    public JsonObject getConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionName, Boolean xml) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        WebResource webResource = jerseyClient.resource(getUrl(deploymentUrl, xml));

        String response = webResource.path(extensionName).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return jsonResponse;
    }

    public JsonObject updateConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionSid,
                                          MultivaluedMap<String, String> configurationParams) {
        return updateConfiguration(deploymentUrl, adminUsername, adminAuthToken, extensionSid, configurationParams, false);
    }

    public JsonObject updateConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionSid,
                                          MultivaluedMap<String, String> configurationParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml)+"/"+extensionSid;

        WebResource webResource = jerseyClient.resource(url);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, configurationParams);
        jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }
}

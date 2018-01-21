package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.logging.Logger;
import javax.ws.rs.client.Entity;

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

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml);

        WebTarget WebTarget = jerseyClient.target(url);

        Response clientResponse = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(configurationParams));
        jsonResponse = parser.parse(clientResponse.readEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionName) {
        return getConfiguration(deploymentUrl, adminUsername, adminAuthToken, extensionName, false);
    }

    public JsonObject getConfiguration(String deploymentUrl, String adminUsername, String adminAuthToken, String extensionName, Boolean xml) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

        WebTarget WebTarget = jerseyClient.target(getUrl(deploymentUrl, xml));

        String response = WebTarget.path(extensionName).request().get(String.class);
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

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml)+"/"+extensionSid;

        WebTarget WebTarget = jerseyClient.target(url);

        Response clientResponse = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(configurationParams));
        jsonResponse = parser.parse(clientResponse.readEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }
}

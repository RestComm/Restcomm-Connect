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

public class CreateGatewaysTool {

    private static CreateGatewaysTool instance;

    private CreateGatewaysTool() {
    }

    public static CreateGatewaysTool getInstance() {
        if (instance == null)
            instance = new CreateGatewaysTool();

        return instance;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    public JsonObject createGateway(String deploymentUrl, String friendlyName, String username, String password, String proxy, String register, String ttl) {

        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");

        String restcommUsername = "ACae6e420f425248d6a26948c17a9e2acf";
        String restcommPassword = "77f8c12cc7b8f8423e5c38b035249166";

        String url = "http://"+restcommUsername+":"+restcommPassword+"@" + endpoint
                + "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Management/Gateways.json";

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(restcommUsername, restcommPassword));

        WebTarget WebTarget = jerseyClient.target(url);

        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        params.add("FriendlyName", friendlyName);
        params.add("UserName", username);
        params.add("Password", password);
        params.add("Proxy", proxy);
        params.add("Register", register);
        params.add("TTL", ttl);

        String response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params),String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }

    public JsonObject updateGateway(String deploymentUrl, String sid, String friendlyName, String username, String password, String proxy, String register, String ttl) {

        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");

        String restcommUsername = "ACae6e420f425248d6a26948c17a9e2acf";
        String restcommPassword = "77f8c12cc7b8f8423e5c38b035249166";

        String url = "http://"+restcommUsername+":"+restcommPassword+"@" + endpoint
                + "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Management/Gateways/"+sid+".json";

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(restcommUsername, restcommPassword));

        WebTarget WebTarget = jerseyClient.target(url);

        MultivaluedMap<String, String> params = new MultivaluedHashMap();
        if (friendlyName != null) params.add("FriendlyName", friendlyName);
        if (username != null) params.add("UserName", username);
        if (password != null) params.add("Password", password);
        if (proxy != null) params.add("Proxy", proxy);
        if (register != null) params.add("Register", register);
        if (ttl != null) params.add("TTL", ttl);

        String response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params),String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

        return jsonObject;
    }

    public void deleteGateway(String deploymentUrl, String sid) {

        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");

        String restcommUsername = "ACae6e420f425248d6a26948c17a9e2acf";
        String restcommPassword = "77f8c12cc7b8f8423e5c38b035249166";

        String url = "http://"+restcommUsername+":"+restcommPassword+"@" + endpoint
                + "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Management/Gateways/"+sid+".json";

        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(restcommUsername, restcommPassword));

        WebTarget WebTarget = jerseyClient.target(url);

        WebTarget.request(MediaType.APPLICATION_JSON).delete();
    }


}

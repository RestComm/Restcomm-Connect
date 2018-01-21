package org.restcomm.connect.testsuite.http;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * @author maria farooq
 */

public class RestcommOrganizationsTool {
	private static Logger logger = Logger.getLogger(RestcommOrganizationsTool.class.getName());

	private static RestcommOrganizationsTool instance;
	private static String organizationsUrl;

	private RestcommOrganizationsTool () {

	}

	public static RestcommOrganizationsTool getInstance () {
		if (instance == null)
			instance = new RestcommOrganizationsTool();

		return instance;
	}

	private String getOrganizationsUrl (String deploymentUrl) {
		return getOrganizationsUrl(deploymentUrl, false);
	}

	private String getOrganizationsUrl (String deploymentUrl, Boolean xml) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		if (xml) {
			organizationsUrl = deploymentUrl + "/2012-04-24/Organizations";
		} else {
			organizationsUrl = deploymentUrl + "/2012-04-24/Organizations.json";
		}
		return organizationsUrl;
	}

	public JsonObject getOrganization (String deploymentUrl, String adminUsername, String adminAuthToken, String username)
			{
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		WebTarget WebTarget = jerseyClient.target(getOrganizationsUrl(deploymentUrl));

		String response = WebTarget.path(username).request().get(String.class);
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

		return jsonResponse;
	}

	public JsonArray getOrganizationList (String deploymentUrl, String adminUsername, String adminAuthToken, String status)
			 {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		WebTarget webTarget = jerseyClient.target(getOrganizationsUrl(deploymentUrl));
		String response;
		if (status != null) {
            MultivaluedMap<String, String> params = new MultivaluedHashMap();
            params.add("Status", String.valueOf(status));
            webTarget.queryParam("Status", String.valueOf(status));

            response = webTarget.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                    .get(String.class);
        } else {
            response = webTarget.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        }
		JsonParser parser = new JsonParser();
		JsonArray jsonArray = null;
		try {
            JsonElement jsonElement = parser.parse(response);
            if (jsonElement.isJsonArray()) {
                jsonArray = jsonElement.getAsJsonArray();
            } else {
                logger.info("JsonElement: " + jsonElement.toString());
            }
        } catch (Exception e) {
            logger.info("Exception during JSON response parsing, exception: "+e);
            logger.info("JSON response: "+response);
        }

		return jsonArray;
	}

	public Response getOrganizationResponse (String deploymentUrl, String username, String authtoken, String organizationSid) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(username, authtoken));
		WebTarget WebTarget = jerseyClient.target(getOrganizationsUrl(deploymentUrl));
		Response response = WebTarget.path(organizationSid).request().get();
		return response;
	}

	public Response getOrganizationsResponse (String deploymentUrl, String username, String authtoken) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(username, authtoken));
		WebTarget WebTarget = jerseyClient.target(getOrganizationsUrl(deploymentUrl));
		Response response = WebTarget.request().get();
		return response;
	}

	public JsonObject createOrganization (String deploymentUrl, String username, String adminAuthToken, String domainName) {

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			Response clientResponse = createOrganizationResponse(deploymentUrl, username, adminAuthToken, domainName);
			jsonResponse = parser.parse(clientResponse.readEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public Response createOrganizationResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String domainName) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(operatorUsername, operatorAuthtoken));

		String url = getOrganizationsUrl(deploymentUrl) + "/" + domainName;

		WebTarget WebTarget = jerseyClient.target(url);
		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		Response response = WebTarget.request(MediaType.APPLICATION_JSON).put(Entity.form(params));
		return response;
	}
}

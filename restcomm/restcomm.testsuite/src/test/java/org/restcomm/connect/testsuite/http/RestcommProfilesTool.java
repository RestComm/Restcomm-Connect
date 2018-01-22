package org.restcomm.connect.testsuite.http;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author maria farooq
 */

public class RestcommProfilesTool {
	private static Logger logger = Logger.getLogger(RestcommProfilesTool.class.getName());

	private static RestcommProfilesTool instance;
	private static String profilesUrl;

	private RestcommProfilesTool () {

	}

	public static RestcommProfilesTool getInstance () {
		if (instance == null)
			instance = new RestcommProfilesTool();

		return instance;
	}

	private String getProfilesUrl (String deploymentUrl) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		profilesUrl = deploymentUrl + "/2012-04-24/Profiles";
		return profilesUrl;
	}

	public JsonObject getProfile (String deploymentUrl, String adminUsername, String adminAuthToken, String profileSid)
			throws UniformInterfaceException {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

		WebResource webResource = jerseyClient.resource(getProfilesUrl(deploymentUrl));

		String response = webResource.path(profileSid).get(String.class);
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

		return jsonResponse;
	}

	public JsonArray getProfileListJsonResponse (String deploymentUrl, String adminUsername, String adminAuthToken)
			throws UniformInterfaceException {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

		WebResource webResource = jerseyClient.resource(getProfilesUrl(deploymentUrl));
		String response;
		response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
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

	public ClientResponse getProfileResponse (String deploymentUrl, String username, String authtoken, String profileSid) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
		WebResource webResource = jerseyClient.resource(getProfilesUrl(deploymentUrl));
		ClientResponse response = webResource.path(profileSid).get(ClientResponse.class);
		return response;
	}

	public ClientResponse getProfileListClientResponse (String deploymentUrl, String username, String authtoken) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
		WebResource webResource = jerseyClient.resource(getProfilesUrl(deploymentUrl));
		ClientResponse response = webResource.get(ClientResponse.class);
		return response;
	}

	public JsonObject createProfile (String deploymentUrl, String username, String adminAuthToken, JsonObject profileDocument) {

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			ClientResponse clientResponse = createProfileResponse(deploymentUrl, username, adminAuthToken, profileDocument);
			jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public ClientResponse createProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, JsonObject profileDocument) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesUrl(deploymentUrl);

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, profileDocument);
		return response;
	}

	public JsonObject updateProfile (String deploymentUrl, String username, String adminAuthToken, String profileSid, JsonObject profileDocument) {

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			ClientResponse clientResponse = updateProfileResponse(deploymentUrl, username, adminAuthToken, profileSid, profileDocument);
			jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public ClientResponse updateProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid, JsonObject profileDocument) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesUrl(deploymentUrl) + "/" + profileSid;

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class, profileDocument);
		return response;
	}

	public JsonObject deleteProfile (String deploymentUrl, String username, String adminAuthToken, String profileSid) {

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			ClientResponse clientResponse = deleteProfileResponse(deploymentUrl, username, adminAuthToken, profileSid);
			jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public ClientResponse deleteProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesUrl(deploymentUrl) + "/" + profileSid;

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
		return response;
	}
}

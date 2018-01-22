package org.restcomm.connect.testsuite.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.restcomm.connect.testsuite.http.util.HttpLink;
import org.restcomm.connect.testsuite.http.util.HttpUnLink;

import com.google.common.net.HttpHeaders;
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
	private static String profilesEndpointUrl;
	
	public static final String PROFILE_REL_TYPE = "related";
	
	public static final String ACCOUNT_ENPOINT_BASE = "/2012-04-24/Accounts/";
	public static final String ORGANIZATION_ENPOINT_BASE = "/2012-04-24/Organizations/";
	public static final String PROFILE_ENPOINT_BASE = "/2012-04-24/Profiles";

    public enum AssociatedResourceType {
        ACCOUNT, ORGANIZATION
    };

	public static RestcommProfilesTool getInstance () {
		if (instance == null)
			instance = new RestcommProfilesTool();

		return instance;
	}

	private String getTargetResourceUrl(String deploymentUrl, String targetResourceSid, AssociatedResourceType type){
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		switch (type) {
		case ACCOUNT:
			return deploymentUrl + ACCOUNT_ENPOINT_BASE + targetResourceSid;
		case ORGANIZATION:
			return deploymentUrl + ORGANIZATION_ENPOINT_BASE + targetResourceSid;

		default:
			return null;
		}
	}
	
	/**
	 * @param deploymentUrl
	 * @return
	 */
	private String getProfilesEndpointUrl (String deploymentUrl) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		profilesEndpointUrl = deploymentUrl + PROFILE_ENPOINT_BASE;
		return profilesEndpointUrl;
	}

	/**
	 * get a particular profile and return json response
	 * @param deploymentUrl
	 * @param adminUsername
	 * @param adminAuthToken
	 * @param profileSid
	 * @return
	 * @throws UniformInterfaceException
	 */
	public JsonObject getProfile (String deploymentUrl, String adminUsername, String adminAuthToken, String profileSid)
			throws UniformInterfaceException {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

		WebResource webResource = jerseyClient.resource(getProfilesEndpointUrl(deploymentUrl));

		String response = webResource.path(profileSid).get(String.class);
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

		return jsonResponse;
	}

	/**
	 * get a particular profile and return raw response
	 * @param deploymentUrl
	 * @param username
	 * @param authtoken
	 * @param profileSid
	 * @return
	 */
	public ClientResponse getProfileResponse (String deploymentUrl, String username, String authtoken, String profileSid) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
		WebResource webResource = jerseyClient.resource(getProfilesEndpointUrl(deploymentUrl));
		ClientResponse response = webResource.path(profileSid).get(ClientResponse.class);
		return response;
	}

	/**
	 * get profile list and return json response
	 * @param deploymentUrl
	 * @param adminUsername
	 * @param adminAuthToken
	 * @return
	 * @throws UniformInterfaceException
	 */
	public JsonArray getProfileListJsonResponse (String deploymentUrl, String adminUsername, String adminAuthToken)
			throws UniformInterfaceException {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

		WebResource webResource = jerseyClient.resource(getProfilesEndpointUrl(deploymentUrl));
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

	/**
	 * get profile list and return raw response
	 * @param deploymentUrl
	 * @param username
	 * @param authtoken
	 * @return
	 */
	public ClientResponse getProfileListClientResponse (String deploymentUrl, String username, String authtoken) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
		WebResource webResource = jerseyClient.resource(getProfilesEndpointUrl(deploymentUrl));
		ClientResponse response = webResource.get(ClientResponse.class);
		return response;
	}

	/**
	 * create a profile and return json response
	 * @param deploymentUrl
	 * @param username
	 * @param adminAuthToken
	 * @param profileDocument
	 * @return
	 */
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

	/**
	 * create a profile and return raw response
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileDocument
	 * @return
	 */
	public ClientResponse createProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, JsonObject profileDocument) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesEndpointUrl(deploymentUrl);

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, profileDocument);
		return response;
	}

	/**
	 * update a profile and return json response
	 * @param deploymentUrl
	 * @param username
	 * @param adminAuthToken
	 * @param profileSid
	 * @param profileDocument
	 * @return
	 */
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

	/**
	 * update a profile and return raw response
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileSid
	 * @param profileDocument
	 * @return
	 */
	public ClientResponse updateProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid, JsonObject profileDocument) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesEndpointUrl(deploymentUrl) + "/" + profileSid;

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class, profileDocument);
		return response;
	}

	/**
	 * delete a profile and return raw response
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileSid
	 * @return
	 */
	public ClientResponse deleteProfileResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid) {
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorAuthtoken));

		String url = getProfilesEndpointUrl(deploymentUrl) + "/" + profileSid;

		WebResource webResource = jerseyClient.resource(url);
		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
		return response;
	}

	/**
	 * link a profile to a target resource
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileSid
	 * @param targetResourceSid
	 * @param associatedResourceType
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public HttpResponse linkProfile (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid, String targetResourceSid, AssociatedResourceType type) throws ClientProtocolException, IOException {
		String url = getProfilesEndpointUrl(deploymentUrl) + "/" + profileSid;

		HttpLink request = new HttpLink(url);
		request = (HttpLink) addLinkUnlinkRequiredHeaders(request, deploymentUrl, operatorUsername, operatorAuthtoken, profileSid, targetResourceSid, type);
		final DefaultHttpClient client = new DefaultHttpClient();
	    final HttpResponse response = client.execute(request);
	    logger.info("response is here: "+response);
	    return response;
	}

	/**
	 * unlink a profile from a target resource
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileSid
	 * @param targetResourceSid
	 * @param associatedResourceType
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public HttpResponse unLinkProfile (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid, String targetResourceSid, AssociatedResourceType type) throws ClientProtocolException, IOException {
		String url = getProfilesEndpointUrl(deploymentUrl) + "/" + profileSid;

		HttpUnLink request = new HttpUnLink(url);
		request = (HttpUnLink) addLinkUnlinkRequiredHeaders(request, deploymentUrl, operatorUsername, operatorAuthtoken, profileSid, targetResourceSid, type);
		final DefaultHttpClient client = new DefaultHttpClient();
	    final HttpResponse response = client.execute(request);
	    logger.info("response is here: "+response);
	    return response;
	}
	
	/**
	 * @param request
	 * @param deploymentUrl
	 * @param operatorUsername
	 * @param operatorAuthtoken
	 * @param profileSid
	 * @param targetResourceSid
	 * @param type
	 * @return
	 */
	private HttpRequestBase addLinkUnlinkRequiredHeaders(HttpRequestBase request, String deploymentUrl, String operatorUsername, String operatorAuthtoken, String profileSid, String targetResourceSid, AssociatedResourceType type){
		request.addHeader(getAuthHeader(deploymentUrl, operatorUsername, operatorAuthtoken));
		request.addHeader(getjsonAcceptHeader());
		request.addHeader(getLinkHeaderOfTargetResource(deploymentUrl, targetResourceSid, type));
		return request;
	}
	
	private BasicHeader getLinkHeaderOfTargetResource(String deploymentUrl, String targetResourceSid, AssociatedResourceType type){
		String targetResourceLinkstr = Link.fromUri(getTargetResourceUrl(deploymentUrl, targetResourceSid, type)).rel(PROFILE_REL_TYPE).build().toString();
        return new BasicHeader("Link", targetResourceLinkstr);
	}

	private BasicHeader getAuthHeader(String deploymentUrl, String operatorUsername, String operatorAuthtoken){
		String auth = operatorUsername + ":" + operatorAuthtoken;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
		return new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
	}

	private BasicHeader getjsonAcceptHeader(){
		return new BasicHeader("Accept", "application/json");
	}
}

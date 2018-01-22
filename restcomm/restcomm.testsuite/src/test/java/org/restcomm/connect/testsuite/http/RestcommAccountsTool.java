package org.restcomm.connect.testsuite.http;

import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.restcomm.connect.testsuite.WebTargetUtil;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:lyhungthinh@gmail.com">Thinh Ly</a>
 */

public class RestcommAccountsTool {
	private static Logger logger = Logger.getLogger(RestcommAccountsTool.class.getName());

	private static RestcommAccountsTool instance;
	private static String accountsUrl;

	private RestcommAccountsTool () {

	}

	public static RestcommAccountsTool getInstance () {
		if (instance == null)
			instance = new RestcommAccountsTool();

		return instance;
	}

	private String getAccountsUrl (String deploymentUrl) {
		return getAccountsUrl(deploymentUrl, false);
	}

	private String getAccountsUrl (String deploymentUrl, Boolean xml) {
//        if (accountsUrl == null) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		if (xml) {
			accountsUrl = deploymentUrl + "/2012-04-24/Accounts";
		} else {
			accountsUrl = deploymentUrl + "/2012-04-24/Accounts.json";
		}
//        }

		return accountsUrl;
	}

	public void removeAccount (String deploymentUrl, String adminUsername, String adminAuthToken, String accountSid) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		String url = getAccountsUrl(deploymentUrl, true) + "/" + accountSid;

		WebTarget WebTarget = jerseyClient.target(url);
		WebTarget.request(MediaType.APPLICATION_JSON).delete();
	}

	public JsonObject updateAccount (String deploymentUrl, String adminUsername, String adminAuthToken, String accountSid, String friendlyName, String password, String authToken, String role, String status) {
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			Response clientResponse = updateAccountResponse(deploymentUrl, adminUsername, adminAuthToken, accountSid, friendlyName, password, authToken, role, status);
			jsonResponse = parser.parse(clientResponse.readEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public Response updateAccountResponse (String deploymentUrl, String adminUsername, String adminAuthToken, String accountSid, String friendlyName, String password, String authToken, String role, String status) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		String url = getAccountsUrl(deploymentUrl, false) + "/" + accountSid;

		WebTarget WebTarget = jerseyClient.target(url);

		// FriendlyName, status, password and auth_token are currently updated in AccountsEndpoint. Role remains to be added
		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		if (friendlyName != null)
			params.add("FriendlyName", friendlyName);
		if (password != null)
			params.add("Password", password);
		if (authToken != null)
			params.add("Auth_Token", authToken);
		if (role != null)
			params.add("Role", role);
		if (status != null)
			params.add("Status", status);

		Response response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params));
		return response;
	}

	public JsonObject migrateAccount (String deploymentUrl, String adminUsername, String adminAuthToken,
	                                  String accountSid, String newOrganizationSid) {
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;

		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		String url = getAccountsUrl(deploymentUrl, false) + "/migrate/" + accountSid;

		WebTarget WebTarget = jerseyClient.target(url);

		// FriendlyName, status, password and auth_token are currently updated in AccountsEndpoint. Role remains to be added
		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		if (newOrganizationSid != null)
			params.add("Organization", newOrganizationSid);

		Response response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params));
		if (response.getStatus() == 200) {
			jsonResponse = parser.parse(response.readEntity(String.class)).getAsJsonObject();
		}

		return jsonResponse;
	}

	public JsonObject createAccount (String deploymentUrl, String adminUsername, String adminAuthToken, String emailAddress,
									 String password) {
		return createAccount(deploymentUrl,adminUsername, adminAuthToken, emailAddress, password, null);
	}

	public JsonObject createAccount (String deploymentUrl, String adminUsername, String adminAuthToken, String emailAddress,
									 String password, String friendlyName) {

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = null;
		try {
			Response clientResponse = createAccountResponse(deploymentUrl, adminUsername, adminAuthToken, emailAddress, password, friendlyName, null);
			jsonResponse = parser.parse(clientResponse.readEntity(String.class)).getAsJsonObject();
		} catch (Exception e) {
			logger.info("Exception: " + e);
		}
		return jsonResponse;
	}

	public Response createAccountResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String emailAddress,
												 String password) {
		return createAccountResponse(deploymentUrl, operatorUsername, operatorAuthtoken, emailAddress, password, null, null);
	}

	public Response createAccountResponse (String deploymentUrl, String operatorUsername, String operatorAuthtoken, String emailAddress,
												 String password, String friendlyName, String organizationSid) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(operatorUsername, operatorAuthtoken));

		String url = getAccountsUrl(deploymentUrl);

		WebTarget WebTarget = jerseyClient.target(url);

		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		params.add("EmailAddress", emailAddress);
		params.add("Password", password);
		params.add("Role", "Administartor");
		if (friendlyName != null)
			params.add("FriendlyName", friendlyName);
		if (organizationSid != null)
			params.add("OrganizationSid", organizationSid);

		Response response = WebTarget.request(MediaType.APPLICATION_JSON).post(Entity.form(params));
		return response;
	}

	public JsonObject getAccount (String deploymentUrl, String adminUsername, String adminAuthToken, String username)
			 {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(adminUsername, adminAuthToken));

		WebTarget WebTarget = jerseyClient.target(getAccountsUrl(deploymentUrl));

		String response = WebTarget.path(username).request().get(String.class);
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

		return jsonResponse;
	}

	/*
		Returns an account response so that the invoker can make decisions on the status code etc.
	 */
	public Response getAccountResponse (String deploymentUrl, String username, String authtoken, String accountSid) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(username, authtoken));
		WebTarget WebTarget = jerseyClient.target(getAccountsUrl(deploymentUrl));
		Response response = WebTarget.path(accountSid).request().get();
		return response;
	}

	public Response getAccountsResponse (String deploymentUrl, String username, String authtoken) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(username, authtoken));
		WebTarget WebTarget = jerseyClient.target(getAccountsUrl(deploymentUrl));
		Response response = WebTarget.request().get();
		return response;
	}

	public Response removeAccountResponse (String deploymentUrl, String operatingUsername, String operatingAuthToken, String removedAccountSid) {
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(operatingUsername, operatingAuthToken));
		WebTarget WebTarget = jerseyClient.target(getAccountsUrl(deploymentUrl));
		Response response = WebTarget.path(removedAccountSid).request().delete(Response.class);
		return response;
	}

	/**
	 * @param deploymentUrl
	 * @param username
	 * @param authtoken
	 * @param organizationSid
	 * @param domainName
	 * @return
	 */
	public Response getAccountsWithFilterClientResponse (String deploymentUrl, String username, String authtoken, String organizationSid, String domainName) {
		WebTarget webTarget = prepareAccountListWebTarget(deploymentUrl, username, authtoken);
                WebTargetUtil.addQueryMap(webTarget, prepareAccountListFilter(organizationSid, domainName));
		Response  response = webTarget
				.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                                .get();
		return response;
	}

	/**
	 * @param deploymentUrl
	 * @param username
	 * @param authtoken
	 * @param organizationSid
	 * @param domainName
	 * @return JsonArray
	 */
	public JsonArray getAccountsWithFilterResponse (String deploymentUrl, String username, String authtoken, String organizationSid, String domainName) {
		WebTarget webTarget = prepareAccountListWebTarget(deploymentUrl, username, authtoken);

                WebTargetUtil.addQueryMap(webTarget, prepareAccountListFilter(organizationSid, domainName));
		String  response = webTarget
				.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .get(String.class);
		JsonElement jsonElement = new JsonParser().parse(response);
        return jsonElement.getAsJsonArray();
	}

	/**
	 * @param deploymentUrl
	 * @param username
	 * @param authtoken
	 * @return
	 */
	private WebTarget prepareAccountListWebTarget(String deploymentUrl, String username, String authtoken){
		Client jerseyClient = ClientBuilder.newClient();
		jerseyClient.register(HttpAuthenticationFeature.basic(username, authtoken));
		WebTarget WebTarget = jerseyClient.target(getAccountsUrl(deploymentUrl));
		return WebTarget;
	}

	/**
	 * @param organizationSid
	 * @param domainName
	 * @return
	 */
	private MultivaluedMap<String, String> prepareAccountListFilter(String organizationSid, String domainName){
		MultivaluedMap<String, String> params = new MultivaluedHashMap();
		if(organizationSid != null && !(organizationSid.trim().isEmpty()))
			params.add("OrganizationSid", organizationSid);
		if(domainName != null && !(domainName.trim().isEmpty()))
			params.add("DomainName", domainName);

		return params;
	}
}

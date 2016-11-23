package org.restcomm.connect.testsuite.http;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:lyhungthinh@gmail.com">Thinh Ly</a>
 */

public class RestcommAccountsTool {
    private static Logger logger = Logger.getLogger(RestcommAccountsTool.class.getName());

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
        return getAccountsUrl(deploymentUrl, false);
    }

    private String getAccountsUrl(String deploymentUrl, Boolean xml) {
//        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }
            if(xml){
                accountsUrl = deploymentUrl + "/2012-04-24/Accounts";
            } else {
                accountsUrl = deploymentUrl + "/2012-04-24/Accounts.json";
            }
//        }

        return accountsUrl;
    }

    public void removeAccount(String deploymentUrl, String adminUsername, String adminAuthToken, String accountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getAccountsUrl(deploymentUrl, true) + "/" + accountSid;

        WebResource webResource = jerseyClient.resource(url);
        webResource.accept(MediaType.APPLICATION_JSON).delete();
    }

    public JsonObject updateAccount(String deploymentUrl, String adminUsername, String adminPassword, String accountSid, String friendlyName, String password, String authToken, String role, String status) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;
        try {
            ClientResponse clientResponse = updateAccountResponse(deploymentUrl,adminUsername,adminPassword,accountSid, friendlyName, password, authToken, role, status);
            jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        } catch (Exception e) {
            logger.info("Exception: "+e);
        }
        return jsonResponse;
    }

    public ClientResponse updateAccountResponse(String deploymentUrl, String adminUsername, String adminPassword, String accountSid, String friendlyName, String password, String authToken, String role, String status) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminPassword));

        String url = getAccountsUrl(deploymentUrl,false) + "/"+accountSid;

        WebResource webResource = jerseyClient.resource(url);

        // FriendlyName, status, password and auth_token are currently updated in AccountsEndpoint. Role remains to be added
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
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

        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, params);
        return response;
    }

    public JsonObject createAccount(String deploymentUrl, String adminUsername, String adminPassword, String emailAddress,
            String password) {

        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;
        try {
            ClientResponse clientResponse = createAccountResponse(deploymentUrl,adminUsername,adminPassword,emailAddress,password);
            jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        } catch (Exception e) {
            logger.info("Exception: "+e);
        }
        return jsonResponse;
    }

    public ClientResponse createAccountResponse(String deploymentUrl, String operatorUsername, String operatorPassword, String emailAddress,
                                    String password) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(operatorUsername, operatorPassword));

        String url = getAccountsUrl(deploymentUrl);

        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("EmailAddress", emailAddress);
        params.add("Password", password);
        params.add("Role", "Administrator");

        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, params);
        return response;
    }

    public JsonObject getAccount(String deploymentUrl, String adminUsername, String adminAuthToken, String username)
            throws UniformInterfaceException {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        WebResource webResource = jerseyClient.resource(getAccountsUrl(deploymentUrl));

        String response = webResource.path(username).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return jsonResponse;
    }

    /*
        Returns an account response so that the invoker can make decisions on the status code etc.
     */
    public ClientResponse getAccountResponse(String deploymentUrl, String username, String authtoken, String accountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
        WebResource webResource = jerseyClient.resource(getAccountsUrl(deploymentUrl));
        ClientResponse response = webResource.path(accountSid).get(ClientResponse.class);
        return response;
    }

    public ClientResponse getAccountsResponse(String deploymentUrl, String username, String authtoken) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authtoken));
        WebResource webResource = jerseyClient.resource(getAccountsUrl(deploymentUrl));
        ClientResponse response = webResource.get(ClientResponse.class);
        return response;
    }

    public ClientResponse removeAccountResponse(String deploymentUrl, String operatingUsername, String operatingAuthToken, String removedAccountSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(operatingUsername, operatingAuthToken));
        WebResource webResource = jerseyClient.resource(getAccountsUrl(deploymentUrl));
        ClientResponse response = webResource.path(removedAccountSid).delete(ClientResponse.class);
        return response;
    }
}

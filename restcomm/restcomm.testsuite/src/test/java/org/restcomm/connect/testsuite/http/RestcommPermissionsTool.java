package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommPermissionsTool {
    private static Logger logger = Logger.getLogger(RestcommPermissionsTool.class.getName());

    private static RestcommPermissionsTool instance;
    private static String accountsUrl;

    private RestcommPermissionsTool() {    }

    public static RestcommPermissionsTool getInstance() {
        if (instance == null)
            instance = new RestcommPermissionsTool();

        return instance;
    }

    private String getUrl(String deploymentUrl, Boolean xml) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }
            if(xml){
                accountsUrl = deploymentUrl + "/2012-04-24/Permissions";
            } else {
                accountsUrl = deploymentUrl + "/2012-04-24/Permissions.json";
            }

        return accountsUrl;
    }

    public JsonObject addPermission(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> permissionParams) {
        return addPermission(deploymentUrl, adminUsername, adminAuthToken, permissionParams, false);
    }

    public JsonObject addPermission(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> permissionParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;
        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = getAuthenticatedJerseyClient(adminUsername, adminAuthToken).resource(url);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, permissionParams);
        String entity = clientResponse.getEntity(String.class);

        JsonElement jsonElement = parser.parse(entity);

        if(jsonElement.isJsonObject()){
            jsonResponse = jsonElement.getAsJsonObject();
        }else if(jsonElement.isJsonArray()){
            jsonResponse = (jsonElement.getAsJsonArray()).get(0).getAsJsonObject();
        }

        return jsonResponse;
    }

    public JsonElement getPermissionsList(String deploymentUrl, String adminUsername, String adminAuthToken, Boolean xml) {
        WebResource webResource = getAuthenticatedJerseyClient(adminUsername, adminAuthToken).resource(getUrl(deploymentUrl, xml));

        String response = webResource.get(String.class);

        JsonParser parser = new JsonParser();
        JsonElement jsonResponse = parser.parse(response);

        return jsonResponse;
    }

    public JsonObject getPermission(String deploymentUrl, String adminUsername, String adminAuthToken, String permissionSid) throws Exception {
        return getPermission(deploymentUrl, adminUsername, adminAuthToken, permissionSid, false);
    }

    public JsonObject getPermission(String deploymentUrl, String adminUsername, String adminAuthToken, String permissionSid, Boolean xml) throws Exception {
        WebResource webResource = getAuthenticatedJerseyClient(adminUsername, adminAuthToken).resource(getUrl(deploymentUrl, xml));
        String response = "";
        ClientResponse clientResponse = webResource.path(permissionSid).get(ClientResponse.class);
        int status = clientResponse.getStatus();
        response = clientResponse.getEntity(String.class);
        //System.out.println("status="+status+" response="+response+" "+clientResponse.toString()+" "+clientResponse.getClientResponseStatus());
        //TODO: handle other Statuses?
        if(clientResponse.getClientResponseStatus().equals(Status.NOT_FOUND) ){
            throw new NotFoundException();
        }
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;
        JsonElement jsonElement = parser.parse(response);
        if(jsonElement.isJsonArray()){
            jsonResponse = ((JsonArray)jsonElement).get(0).getAsJsonObject();
        }else if(jsonElement.isJsonObject()){
            jsonResponse = (JsonObject)jsonElement;
        }

        return jsonResponse;
    }

    public JsonObject updatePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid,
                                          MultivaluedMap<String, String> configurationParams) {
        return updatePermission(deploymentUrl, adminUsername, adminAuthToken, objectSid, configurationParams, false);
    }

    public JsonObject updatePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid,
                                          MultivaluedMap<String, String> configurationParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;

        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = getAuthenticatedJerseyClient(adminUsername, adminAuthToken).resource(url).path(objectSid);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, configurationParams);
        if(clientResponse.getClientResponseStatus().equals(Status.NOT_FOUND) ){
            throw new NotFoundException();
        }
        jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }

    public JsonElement removePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid) {
        return removePermission(deploymentUrl, adminUsername, adminAuthToken, objectSid, false);
    }

    public JsonElement removePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid, boolean xml) {
        JsonParser parser = new JsonParser();
        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = getAuthenticatedJerseyClient(adminUsername, adminAuthToken).resource(url);
        ClientResponse response = webResource.path(objectSid).accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
        //TODO:handle other status'
        if(response.getClientResponseStatus().equals(Status.NOT_FOUND) ){
            throw new NotFoundException();
        }

        return parser.parse(response.getEntity(String.class));
    }

    private Client getAuthenticatedJerseyClient(String adminUsername, String adminAuthToken){
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
        return jerseyClient;
    }
}

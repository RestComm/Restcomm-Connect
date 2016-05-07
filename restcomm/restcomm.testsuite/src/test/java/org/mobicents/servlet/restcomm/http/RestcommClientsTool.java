package org.mobicents.servlet.restcomm.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author guilherme.jansen@telestax.com
 */

public class RestcommClientsTool {
	
	private static RestcommClientsTool instance;
	private static String clientsUrl;
	
	private RestcommClientsTool(){
		
	}
	
	public static RestcommClientsTool getInstance(){
		if(instance == null)
			instance = new RestcommClientsTool();
		return instance;
	}
	
	private String getClientsUrl(String deploymentUrl, String adminAccountSid){
		return getClientsUrl(deploymentUrl, adminAccountSid, false);
	}
	
	private String getClientUrl(String deploymentUrl, String adminAccountSid, String clientSid, Boolean presenceOnly){
		return getClientUrl(deploymentUrl, adminAccountSid, clientSid.trim(), false, presenceOnly);
	}
	
	private String getClientsUrl(String deploymentUrl, String adminAccountSid, Boolean xml){
		if(deploymentUrl.endsWith("/")){
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		if(xml){
			clientsUrl = deploymentUrl + "/2012-04-24/Accounts/" + adminAccountSid + "/Clients";
		} else {
			clientsUrl = deploymentUrl + "/2012-04-24/Accounts/" + adminAccountSid + "/Clients.json";
		}
		return clientsUrl;
	}
	
	private String getClientUrl(String deploymentUrl, String adminAccountSid, String clientSid, Boolean xml, Boolean presenceOnly){
		if(deploymentUrl.endsWith("/")){
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		
		clientsUrl = deploymentUrl + "/2012-04-24/Accounts/" + adminAccountSid + "/Clients/" + clientSid;
		
		if(presenceOnly){
			clientsUrl += "/presence";
		}
		
		if(!xml){
			clientsUrl += ".json";
		}
		
		return clientsUrl;
	}
	
	public JsonArray getClients(String deploymentUrl, String adminUsername, String adminAccountSid, String adminAuthToken){
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
		
		WebResource webResource = jerseyClient.resource(getClientsUrl(deploymentUrl, adminAccountSid));
		
		String response = webResource.get(String.class);
		JsonParser parser = new JsonParser();
		JsonArray jsonResponse = parser.parse(response).getAsJsonArray();
		
		return jsonResponse;
	}
	
	public JsonObject getClient(String deploymentUrl, String adminUsername, String adminAccountSid, String adminAuthToken, String clientSid, Boolean presenceOnly){
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));
		
		WebResource webResource = jerseyClient.resource(getClientUrl(deploymentUrl, adminAccountSid, clientSid, presenceOnly));
		
		String response = webResource.get(String.class);
		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
		
		return jsonResponse;
	}
	
}

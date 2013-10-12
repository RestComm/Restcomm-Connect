package org.mobicents.servlet.restcomm.http;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommCallsTool {

	private static RestcommCallsTool instance;
	private static String accountsUrl;
	
	private RestcommCallsTool(){
		
	}
	
	public static RestcommCallsTool getInstance(){
		if (instance == null)
			instance = new RestcommCallsTool();
		
		return instance;
	}
	
	private String getAccountsUrl(String deploymentUrl, String username) {
		if(accountsUrl == null){
			if (deploymentUrl.endsWith("/")) {
				deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
			}
			accountsUrl = deploymentUrl + "/2012-04-24/Accounts/"+username+"/Calls.json";
		}
		
		return accountsUrl;
	}
	
	public JsonObject getCalls(String deploymentUrl, String username, String authToken){	
		return getCalls(deploymentUrl, username, authToken, null, null);
	}
	
	public JsonObject getCalls(String deploymentUrl, String username, String authToken, Integer page, Integer pageSize){	
		
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));
		
		String url = getAccountsUrl(deploymentUrl, username);
		
		WebResource webResource = jerseyClient.resource(url);
	    
		String response = null;
		
		if(page != null || pageSize != null){
			MultivaluedMap<String, String> params = new MultivaluedMapImpl();
			
			if (page != null)
				params.add("Page", String.valueOf(page));
			if (pageSize != null)
				params.add("PageSize", String.valueOf(pageSize));
			
			response = webResource.queryParams(params).accept(MediaType.APPLICATION_JSON).get(String.class);
		} else {
			response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
		}
	
	    JsonParser parser = new JsonParser();
	    JsonObject jsonObject = parser.parse(response).getAsJsonObject();
	    
//	    JsonArray jsonArray = parser.parse(response).getAsJsonArray();
	    
		return jsonObject;
	}

	public JsonObject getCallsUsingFilter(String deploymentUrl, String username, String authToken, Map<String, String> filters){	
		
		Client jerseyClient = Client.create();
		jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));
		
		String url = getAccountsUrl(deploymentUrl, username);
		
		WebResource webResource = jerseyClient.resource(url);
		
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		
		for (String filterName : filters.keySet()) {
			String filterData = filters.get(filterName);
			params.add(filterName, filterData);
		}
	    webResource = webResource.queryParams(params);
	    String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);  
	    JsonParser parser = new JsonParser();
	    JsonObject jsonObject = parser.parse(response).getAsJsonObject();
	    
		return jsonObject;
	}
}

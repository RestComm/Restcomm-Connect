package org.mobicents.servlet.restcomm.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class CreateClientsTool {
	
	private static CreateClientsTool instance;
	
	private CreateClientsTool(){}
	
	public static CreateClientsTool getInstance(){
		if(instance == null)
			instance = new CreateClientsTool();
		
		return instance;
	}
	
	private String getEndpoint(String deploymentUrl) {
		if (deploymentUrl.endsWith("/")) {
			deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
		return deploymentUrl;
	}
	
	public String createClient(String deploymentUrl, String username, String password, String voiceUrl) throws ClientProtocolException, IOException{
		
		String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");
		
		String url = "http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@"+endpoint+
				"/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Clients.json";
		
		String clientSid = null;
		
		HttpClient httpclient = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(url);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("Login", username));
		nvps.add(new BasicNameValuePair("Password", password));
		
		if(voiceUrl != null)
			nvps.add(new BasicNameValuePair("VoiceUrl", voiceUrl));
		
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		HttpResponse response = httpclient.execute(httpPost);

		if(response.getStatusLine().getStatusCode()==200){
			HttpEntity entity = response.getEntity();
			String res = EntityUtils.toString(entity);
			System.out.println("Entity: "+res);
			
			res = res.replaceAll("\\{", "").replaceAll("\\}", "");
			String[] components = res.split(",");
			clientSid = (components[0].split(":")[1]).replaceAll("\"", "");
		}
		
		httpPost.releaseConnection();
		
		return clientSid;
	}
	
	
}

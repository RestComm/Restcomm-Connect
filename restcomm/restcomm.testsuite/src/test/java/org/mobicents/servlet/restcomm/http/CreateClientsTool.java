package org.mobicents.servlet.restcomm.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:lyhungthinh@gmail.com">Thinh Ly</a>
 */

public class CreateClientsTool {

    private static CreateClientsTool instance;
    private static String clientUrl;

    private CreateClientsTool() {
    }

    public static CreateClientsTool getInstance() {
        if (instance == null)
            instance = new CreateClientsTool();

        return instance;
    }

    private String getEndpoint(String deploymentUrl) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        return deploymentUrl;
    }

    private String getAuthorizationToken(String username, String password) {
        byte[] usernamePassBytes = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
        String authenticationToken = Base64.encodeBase64String(usernamePassBytes);
        return authenticationToken;
    }

    private String getClientUrl(String deploymentUrl, JsonObject account) {
        return getClientUrl(deploymentUrl, account, false);
    }

    private String getClientUrl(String deploymentUrl, JsonObject account, Boolean xml) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }
        StringBuffer curlCommand = new StringBuffer("http://");
        curlCommand.append(account.get("sid"));
        curlCommand.append(":");
        curlCommand.append(account.get("auth_token"));
        curlCommand.append(deploymentUrl.replace("http://", "@"));
        curlCommand.append("/2012-04-24/Accounts/").append(account.get("sid"));

        if (xml) {
            curlCommand.append("/Clients");
        } else {
            curlCommand.append("/Clients.json");
        }

        return curlCommand.toString().replace("\"", "");
    }

    public JsonObject getClientOfAccount(String deploymentUrl, JsonObject account, String credentialUsername,
            String credentialPassword) {
        String url = getClientUrl(deploymentUrl, account);
        JsonObject jsonResponse = null;
        String authToken = getAuthorizationToken(credentialUsername, credentialPassword);

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Authorization", "Basic " + authToken);
            HttpResponse response = httpclient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                String res = EntityUtils.toString(entity);
                JsonParser parser = new JsonParser();
                JsonArray jArray = parser.parse(res).getAsJsonArray();
                jsonResponse = jArray.get(0).getAsJsonObject();
            }

            httpGet.releaseConnection();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    public void updateClientVoiceUrl(String deploymentUrl, JsonObject account, String clientSid, String voiceUrl,
            String credentialUsername, String credentialPassword) throws ClientProtocolException, IOException {
        String url = getClientUrl(deploymentUrl, account);
        String clientUrl = url.replace("Clients.json", "Clients/" + clientSid);

        String authToken = getAuthorizationToken(credentialUsername, credentialPassword);
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(clientUrl);
        httpPost.addHeader("Authorization", "Basic " + authToken);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        if (voiceUrl != null)
            nvps.add(new BasicNameValuePair("VoiceUrl", voiceUrl));

        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpclient.execute(httpPost);
        httpPost.releaseConnection();
    }

    public String createClient(String deploymentUrl, String username, String password, String voiceUrl)
            throws ClientProtocolException, IOException {

        String endpoint = getEndpoint(deploymentUrl).replaceAll("http://", "");

        String url = "http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@" + endpoint
                + "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Clients.json";

        String clientSid = null;

        HttpClient httpclient = new DefaultHttpClient();

        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("Login", username));
        nvps.add(new BasicNameValuePair("Password", password));

        if (voiceUrl != null)
            nvps.add(new BasicNameValuePair("VoiceUrl", voiceUrl));

        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpclient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entity = response.getEntity();
            String res = EntityUtils.toString(entity);
            System.out.println("Entity: " + res);

            res = res.replaceAll("\\{", "").replaceAll("\\}", "");
            String[] components = res.split(",");
            clientSid = (components[0].split(":")[1]).replaceAll("\"", "");
        }

        httpPost.releaseConnection();

        return clientSid;
    }
}

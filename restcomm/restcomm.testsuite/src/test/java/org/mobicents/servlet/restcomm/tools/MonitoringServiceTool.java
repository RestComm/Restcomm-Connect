package org.mobicents.servlet.restcomm.tools;

import com.amazonaws.util.json.JSONArray;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.util.Iterator;

/**
 * Created by gvagenas on 11/25/15.
 */
public class MonitoringServiceTool {
    private static MonitoringServiceTool instance;
    private static String accountsUrl;
    private static Logger logger = Logger.getLogger(MonitoringServiceTool.class);

    private MonitoringServiceTool() {}

    public static MonitoringServiceTool getInstance() {
        if (instance == null)
            instance = new MonitoringServiceTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username) {
        int registeredUsers;

        if (accountsUrl == null) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }

            accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Supervisor.json";
        }

        return accountsUrl;
    }

    public int getRegisteredUsers(String deploymentUrl, String username, String authToken) {
        int registeredUsers = 0;
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));

        String url = getAccountsUrl(deploymentUrl, username);

        WebResource webResource = jerseyClient.resource(url);

        String response = null;

        response = webResource.path("/metrics").accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);

        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(response).getAsJsonObject();

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");

        JsonElement elem = metrics.get("RegisteredUsers");

        registeredUsers = elem.getAsInt();

        return registeredUsers;

    }

}

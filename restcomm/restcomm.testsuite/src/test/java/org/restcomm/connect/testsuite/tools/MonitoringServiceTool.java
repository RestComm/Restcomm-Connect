package org.restcomm.connect.testsuite.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.client.Client;import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gvagenas on 11/25/15.
 */
public class MonitoringServiceTool {
    private static MonitoringServiceTool instance;
    private static Logger logger = Logger.getLogger(MonitoringServiceTool.class);

    private MonitoringServiceTool() {
    }

    public static MonitoringServiceTool getInstance() {
        if (instance == null)
            instance = new MonitoringServiceTool();

        return instance;
    }

    public String getAccountsUrl(String deploymentUrl, String username) {

        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }

        String accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Supervisor.json";

        return accountsUrl;
    }

    public JsonObject getLiveCalls(String deploymentUrl, String username, String authToken) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));
        String url = getAccountsUrl(deploymentUrl, username);
        WebTarget webResource = jerseyClient.target(url).path("/livecalls");

        String response = null;
        response = webResource.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        JsonParser parser = new JsonParser();
        return parser.parse(response).getAsJsonObject();
    }

    public Map<String, Integer> getMgcpResources(JsonObject metrics) {
        Integer mgcpEndpoints = metrics.getAsJsonObject("Metrics").get("MgcpEndpoints").getAsInt();
        Integer mgcpConnections = metrics.getAsJsonObject("Metrics").get("MgcpConnections").getAsInt();

        Map<String, Integer> mgcpResources = new ConcurrentHashMap<String, Integer>();

        mgcpResources.put("MgcpEndpoints", mgcpEndpoints);
        mgcpResources.put("MgcpConnections", mgcpConnections);


        return mgcpResources;
    }

    public JsonObject getMetrics(String deploymentUrl, String username, String authToken) {
        return getMetrics(deploymentUrl, username, authToken, true, true);
    }

    public JsonObject getMetrics(String deploymentUrl, String username, String authToken, boolean callDetails, boolean mgcpStats) {
        Client jerseyClient = ClientBuilder.newClient();
        jerseyClient.register(HttpAuthenticationFeature.basic(username, authToken));
        String url = getAccountsUrl(deploymentUrl, username);
        WebTarget webResource = jerseyClient.target(url).path("/metrics");

        if (callDetails) {
            webResource = webResource.queryParam("LiveCallDetails","true");
        }
        if (mgcpStats) {
            webResource = webResource.queryParam("MgcpStats", "true");
        }

        String response = null;
        response = webResource.request(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        JsonParser parser = new JsonParser();
        return parser.parse(response).getAsJsonObject();
    }

    public int getRegisteredUsers(String deploymentUrl, String username, String authToken) {
        int registeredUsers = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");

        JsonElement elem = metrics.get("RegisteredUsers");

        registeredUsers = elem.getAsInt();

        return registeredUsers;

    }

    public int getStatistics (String deploymentUrl, String username, String authToken) {
        int liveCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");

        JsonElement elem = metrics.get("LiveCalls");

        liveCalls = elem.getAsInt();

        return liveCalls;
    }

    public int getLiveIncomingCallStatistics (String deploymentUrl, String username, String authToken) {
        int liveIncomingCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");

        JsonElement elem = metrics.get("LiveIncomingCalls");

        liveIncomingCalls = elem.getAsInt();

        return liveIncomingCalls;
    }

    public int getLiveOutgoingCallStatistics (String deploymentUrl, String username, String authToken) {
        int liveOutgoingCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");

        JsonElement elem = metrics.get("LiveOutgoingCalls");

        liveOutgoingCalls = elem.getAsInt();

        return liveOutgoingCalls;
    }

    public int getLiveCallsArraySize(String deploymentUrl, String username, String authToken) {
        int liveCallsArraySize = 0;
        JsonObject jsonObject = getLiveCalls(deploymentUrl, username, authToken);

//        {"InstanceId":"IDbe78ada9dc864b558774ce4432cac866","Version":"7.5.0-SNAPSHOT","Revision":"r35cf44c0589a0aeabd6ae8d1bfab0a9edcd24c1a","Metrics":{"TotalCallsSinceUptime":0,"NoAnswerCalls":0,"LiveOutgoingCalls":0,"OutgoingCallsSinceUptime":0,"IncomingCallsSinceUptime":0,"RegisteredUsers":1,"CompletedCalls":0,"TextMessageOutbound":0,"NotFoundCalls":0,"CanceledCalls":0,"FailedCalls":0,"TextMessageNotFound":0,"TextMessageInboundToApp":0,"LiveCalls":0,"BusyCalls":0,"LiveIncomingCalls":0,"TextMessageInboundToProxyOut":0,"TextMessageInboundToClient":0},"LiveCallDetails":[]}

        JsonArray liveCallDetails = jsonObject.getAsJsonArray("LiveCallDetails");

        liveCallsArraySize = liveCallDetails.size();

//        JsonElement elem = metrics.get("LiveCalls");
//
//        liveCallsArraySize = elem.getAsInt();

        return liveCallsArraySize;
    }

    public int getMaxConcurrentCalls(String deploymentUrl, String username, String authToken) {
        int maxConcurrentCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);
        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");
        JsonElement elem = metrics.get("MaximumConcurrentCalls");
        maxConcurrentCalls = elem.getAsInt();
        return maxConcurrentCalls;
    }

    public int getMaxConcurrentIncomingCalls(String deploymentUrl, String username, String authToken) {
        int maxConcurrentIncomingCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);
        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");
        JsonElement elem = metrics.get("MaximumConcurrentIncomingCalls");
        maxConcurrentIncomingCalls = elem.getAsInt();
        return maxConcurrentIncomingCalls;
    }

    public int getMaxConcurrentOutgoingCalls(String deploymentUrl, String username, String authToken) {
        int maxConcurrentOutgoingCalls = 0;
        JsonObject jsonObject = getMetrics(deploymentUrl, username, authToken);
        JsonObject metrics = jsonObject.getAsJsonObject("Metrics");
        JsonElement elem = metrics.get("MaximumConcurrentIncomingCalls");
        maxConcurrentOutgoingCalls = elem.getAsInt();
        return maxConcurrentOutgoingCalls;
    }

}

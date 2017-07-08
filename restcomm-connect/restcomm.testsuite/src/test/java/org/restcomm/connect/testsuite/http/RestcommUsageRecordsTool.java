/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.testsuite.http;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.restcomm.connect.dao.entities.CallDetailRecordList;
import org.restcomm.connect.dao.entities.UsageList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */

public class RestcommUsageRecordsTool {

    private static RestcommUsageRecordsTool instance;
    private static String accountsUrl;
    private static Logger logger = Logger.getLogger(RestcommUsageRecordsTool.class);

    private RestcommUsageRecordsTool() {
    }

    public static RestcommUsageRecordsTool getInstance() {
        if (instance == null)
            instance = new RestcommUsageRecordsTool();

        return instance;
    }

    public JsonElement getUsageRecordsDaily(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Daily", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsMonthly(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Monthly", categoryStr, "", "", null, null, json).getAsJsonArray();
    }

    public JsonElement getUsageRecordsYearly(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Yearly", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsAllTime(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "AllTime", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsToday(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Today", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsYesterday(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Yesterday", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsThisMonth(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "ThisMonth", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsLastMonth(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "LastMonth", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecordsWeekly(String deploymentUrl, String username, String authToken, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, "Weekly", categoryStr, "", "", null, null, json);
    }

    public JsonElement getUsageRecords(String deploymentUrl, String username, String authToken, String subresource, String categoryStr, Boolean json) {
        return getUsageRecords(deploymentUrl, username, authToken, subresource, categoryStr, "", "", null, null, json);
    }

    private String getUsageRecordsUrl(String deploymentUrl, String username, String subresource, Boolean json) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }

        if (!subresource.isEmpty()) {
            subresource = "/" + subresource;
        }

        accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Usage/Records" + subresource
                + ((json) ? ".json" : "");

        return accountsUrl;
    }

    public JsonElement getUsageRecords(String deploymentUrl, String username, String authToken, String subresource,
            String categoryStr, String startDate, String endDate, Integer page, Integer pageSize, Boolean json) {

        Map<String, String> map = new HashedMap();
        if (!categoryStr.isEmpty())
            map.put("Category", categoryStr);
        if (!startDate.isEmpty())
            map.put("StartDate", startDate);
        if (!endDate.isEmpty())
            map.put("EndDate", endDate);
        if (page != null)
            map.put("Page", String.valueOf(page));
        if (pageSize != null)
            map.put("PageSize", String.valueOf(pageSize));

        return getUsageRecordsUsingFilter(deploymentUrl, username, authToken, subresource, map, json);
    }

    public JsonElement getUsageRecordsUsingFilter(String deploymentUrl, String username, String authToken, String subresource,
            Map<String, String> filters, Boolean json) {

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, authToken));
        String url = getUsageRecordsUrl(deploymentUrl, username, subresource, json);
        WebResource webResource = jerseyClient.resource(url);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (String filterName : filters.keySet()) {
            String filterData = filters.get(filterName);
            params.add(filterName, filterData);
        }

        if (!params.isEmpty()) {
            webResource = webResource.queryParams(params);
        }

        String response = webResource.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML).get(String.class);
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = null;
        if (json) {
            try {
                jsonElement = parser.parse(response);
            } catch (Exception e) {
                logger.info("Exception during JSON response parsing, exception: " + e);
                logger.info("JSON response: " + response);
            }
        } else {
            //TODO: return XML and cast as Json
        }

        return jsonElement;
    }
}

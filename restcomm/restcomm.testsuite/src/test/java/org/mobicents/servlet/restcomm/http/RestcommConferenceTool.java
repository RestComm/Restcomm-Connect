/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.http;

import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

/**
 * @author Maria
 */

public class RestcommConferenceTool {

    private static RestcommConferenceTool instance;
    private static String accountsUrl;
    private static Logger logger = Logger.getLogger(RestcommConferenceTool.class);
    
    private RestcommConferenceTool() {}

    public static RestcommConferenceTool getInstance() {
        if (instance == null)
            instance = new RestcommConferenceTool();

        return instance;
    }

    private String getAccountsUrl(String deploymentUrl, String username, Boolean json) {
        if (deploymentUrl.endsWith("/")) {
            deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
        }

        accountsUrl = deploymentUrl + "/2012-04-24/Accounts/" + username + "/Conferences" + ((json) ? ".json" : "");

        return accountsUrl;
    }

    public JsonObject getConferences(String deploymentUrl, String username, String authToken) {
        return (JsonObject) getConferences(deploymentUrl, username, authToken, null, null, true);
    }

    public JsonObject getConferences(String deploymentUrl, String username, String authToken, Integer page, Integer pageSize,
            Boolean json) {
        return null;
    }

    public JsonObject getConference(String deploymentUrl, String username, String authToken, String sid){
    	return null;
    }

    public JsonObject getConferencesUsingFilter(String deploymentUrl, String username, String authToken, Map<String, String> filters) {
    	return null;
    }
}

/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.servlet.ServletContext;

import org.mobicents.servlet.restcomm.rvd.exceptions.AccessApiException;
import org.mobicents.servlet.restcomm.rvd.exceptions.ApplicationAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.exceptions.ApplicationsApiSyncException;
import org.mobicents.servlet.restcomm.rvd.model.ApiServerConfig;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommApplicationResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommClient;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommClient.RestcommClientException;
import org.mobicents.servlet.restcomm.rvd.security.TicketRepository;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

/**
 * @author guilherme.jansen@telestax.com
 */
public class ProjectApplicationsApi {

    private ServletContext servletContext;
    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;

    private enum AccessApiAction {
        CREATE, DELETE, RENAME
    }

    public ProjectApplicationsApi(ServletContext servletContext, WorkspaceStorage workspaceStorage, ModelMarshaler marshaler) {
        this.servletContext = servletContext;
        this.workspaceStorage = workspaceStorage;
        this.marshaler = marshaler;
    }

    public void createApplication(final String ticketId, final String friendlyName, final String projectKind)
            throws ApplicationsApiSyncException, UnsupportedEncodingException {
        HashMap<String, String> params = new HashMap<String, String>();
        String rcmlUrl = "/restcomm-rvd/services/apps/" + RvdUtils.myUrlEncode(friendlyName) + "/controller";
        params.put("FriendlyName", friendlyName);
        params.put("Kind", projectKind);
        params.put("RcmlUrl", rcmlUrl);
        accessApi(ticketId, params, AccessApiAction.CREATE);

    }

    public void rollbackCreateApplication(final String ticketId, final String friendlyName) throws ApplicationsApiSyncException {
        removeApplication(ticketId, friendlyName);
    }

    public void renameApplication(final String ticketId, final String name, final String newName)
            throws ApplicationsApiSyncException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("FriendlyName", name);
        params.put("NewFriendlyName", newName);
        params.put("RcmlUrl", "/restcomm-rvd/services/apps/" + RvdUtils.myUrlEncode(newName) + "/controller");
        accessApi(ticketId, params, AccessApiAction.RENAME);
    }

    public void removeApplication(final String ticketId, final String friendlyName) throws ApplicationsApiSyncException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FriendlyName", friendlyName);
        accessApi(ticketId, params, AccessApiAction.DELETE);
    }

    private void accessApi(final String ticketId, final HashMap<String, String> params, final AccessApiAction action)
            throws ApplicationsApiSyncException {
        try {
            // Load configuration from Restcomm
            ApiServerConfig apiServerConfig = RestcommConfiguration.getApiServerConfig(servletContext
                    .getRealPath(File.separator));

            // Load rvd settings
            SettingsModel settingsModel = SettingsModel.createDefault();
            if (workspaceStorage.entityExists(".settings", ""))
                settingsModel = workspaceStorage.loadEntity(".settings", "", SettingsModel.class);

            // Setup required values depending on existing setup
            String apiHost = settingsModel.getApiServerHost();
            if (RvdUtils.isEmpty(apiHost))
                apiHost = apiServerConfig.getHost();

            Integer apiPort = settingsModel.getApiServerRestPort();
            if (apiPort == null)
                apiPort = apiServerConfig.getPort();

            String authenticationToken = TicketRepository.getInstance().findTicket(ticketId).getAuthenticationToken();
            String username = TicketRepository.getInstance().findTicket(ticketId).getUserId();

            if (RvdUtils.isEmpty(apiHost) || apiPort == null)
                throw new ApplicationsApiSyncException("Could not determine restcomm host/port.");

            if (RvdUtils.isEmpty(authenticationToken))
                throw new ApplicationsApiSyncException("Could not determine credentials to access API.");

            if (RvdUtils.isEmpty(username))
                throw new ApplicationsApiSyncException("Could not determine account to create new Application.");

            RestcommClient client = new RestcommClient(apiHost, apiPort, username, authenticationToken);
            client.setAuthenticationTokenAsPassword(true);

            // Find the account sid for the apiUsername
            RestcommAccountInfoResponse accountResponse = client.get("/restcomm/2012-04-24/Accounts.json/" + username).done(
                    marshaler.getGson(), RestcommAccountInfoResponse.class);
            String accountSid = accountResponse.getSid();
            RestcommApplicationResponse applicationResponse = null;
            String friendlyName;

            switch (action) {
                case DELETE:
                    // Get application sid
                    friendlyName = params.get("FriendlyName");
                    friendlyName = RvdUtils.myUrlEncode(friendlyName);
                    try {
                        applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + friendlyName + ".json")
                                .done(marshaler.getGson(), RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() == 404) {
                            return; // No reference found via API, cancel delete action
                        } else {
                            throw e;
                        }
                    }
                    String applicationSid = applicationResponse.getSid();
                    if (applicationSid == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");
                    // Delete application
                    client.delete("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                            .done(marshaler.getGson(), RestcommApplicationResponse.class);
                    break;
                case CREATE:
                    applicationResponse = client.post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications.json")
                            .addParams(params).done(marshaler.getGson(), RestcommApplicationResponse.class);
                    if (applicationResponse.getSid() == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");
                    break;
                case RENAME:
                    // Check if new name is already in use
                    String newFriendlyName = String.valueOf(params.get("NewFriendlyName"));
                    String newNameTemp = RvdUtils.myUrlEncode(newFriendlyName);
                    try {
                        applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + newNameTemp + ".json").done(
                                marshaler.getGson(), RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() != 404) {
                            throw e;
                        }
                    }

                    if (applicationResponse != null)
                        throw new ApplicationAlreadyExists();

                    // Get application sid
                    friendlyName = params.get("FriendlyName");
                    friendlyName = RvdUtils.myUrlEncode(friendlyName);

                    applicationResponse = client.get(
                            "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + friendlyName + ".json").done(
                            marshaler.getGson(), RestcommApplicationResponse.class);

                    applicationSid = applicationResponse.getSid();
                    if (applicationSid == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");

                    // Rename application
                    String rcmlUrl = String.valueOf(params.get("RcmlUrl"));
                    applicationResponse = client
                            .post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                            .addParam("FriendlyName", newFriendlyName).addParam("RcmlUrl", rcmlUrl)
                            .done(marshaler.getGson(), RestcommApplicationResponse.class);
                    friendlyName = applicationResponse.getFriendly_name();
                    if (!friendlyName.equalsIgnoreCase(newFriendlyName))
                        throw new ApplicationsApiSyncException("Fail to change the name through the API.");
                default:
                    break;
            }
        } catch (AccessApiException e) {
            if (e.getStatusCode() == 409) {
                throw new ApplicationAlreadyExists();
            } else {
                throw new ApplicationsApiSyncException(e.getMessage(), e).setStatusCode(e.getStatusCode());
            }
        } catch (Exception e) {
            throw new ApplicationsApiSyncException(e.getMessage(), e);
        }
    }
}

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

package org.restcomm.connect.rvd;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;


import com.google.gson.Gson;
import org.restcomm.connect.rvd.exceptions.AccessApiException;
import org.restcomm.connect.rvd.exceptions.ApplicationAlreadyExists;
import org.restcomm.connect.rvd.exceptions.ApplicationApiNotSynchedException;
import org.restcomm.connect.rvd.exceptions.ApplicationsApiSyncException;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.restcomm.RestcommApplicationResponse;
import org.restcomm.connect.rvd.restcomm.RestcommClient;
import org.restcomm.connect.rvd.restcomm.RestcommClient.RestcommClientException;
import org.restcomm.connect.rvd.utils.RvdUtils;

/**
 * @author guilherme.jansen@telestax.com
 */
public class ProjectApplicationsApi {

    private UserIdentityContext identity;
    private ApplicationContext appContext;
    private Gson gson;

    private enum AccessApiAction {
        CREATE, DELETE, RENAME, UPDATE
    }

    public ProjectApplicationsApi(UserIdentityContext identity, ApplicationContext appContext) {
        this.identity = identity;
        this.appContext = appContext;
        this.gson = new Gson();
    }

    public String createApplication(final String friendlyName,
            final String projectKind)
            throws ApplicationsApiSyncException, UnsupportedEncodingException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("FriendlyName", friendlyName);
        params.put("Kind", projectKind);
        String applicationSid = accessApi(params, AccessApiAction.CREATE);
        String rcmlUrl = "/restcomm-rvd/services/apps/" + applicationSid + "/controller";
        params.clear();
        params.put("Sid", applicationSid);
        params.put("RcmlUrl", rcmlUrl);
        updateApplication(applicationSid, null, rcmlUrl, null);
        return applicationSid;
    }

    public void rollbackCreateApplication(final String applicationSid) throws ApplicationsApiSyncException {
        removeApplication(applicationSid);
    }

    public void renameApplication(final String applicationSid, final String newName)
            throws ApplicationsApiSyncException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("Sid", applicationSid);
        params.put("NewFriendlyName", newName);
        accessApi(params, AccessApiAction.RENAME);
    }

    public void removeApplication( final String applicationSid) throws ApplicationsApiSyncException {
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("Sid", applicationSid);
        accessApi(params, AccessApiAction.DELETE);
    }

    public void updateApplication(final String applicationSid, final String friendlyName,
            final String rcmlUrl, final String kind) throws ApplicationsApiSyncException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("Sid", applicationSid);
        if (friendlyName != null && !friendlyName.isEmpty()) {
            params.put("FriendlyName", friendlyName);
        }
        if (kind != null && !kind.isEmpty()) {
            params.put("Kind", kind);
        }
        if (rcmlUrl != null && !rcmlUrl.isEmpty()) {
            params.put("RcmlUrl", rcmlUrl);
        }
        accessApi(params, AccessApiAction.UPDATE);
    }

    private String accessApi(final HashMap<String, String> params, final AccessApiAction action)
            throws ApplicationsApiSyncException {
        try {
            URI restcommBaseUri = appContext.getConfiguration().getRestcommBaseUri();
            if (RvdUtils.isEmpty(identity.getEffectiveAuthorizationHeader()))
                throw new ApplicationsApiSyncException("Could not determine credentials to access API.");
            // create the client
            RestcommClient client = new RestcommClient(restcommBaseUri,identity.getEffectiveAuthorizationHeader(),appContext.getHttpClientBuilder());
            String accountSid = identity.getAccountInfo().getSid();
            RestcommApplicationResponse applicationResponse = null;
            String applicationSid;

            switch (action) {
                case DELETE:
                    // Check the existence of the application
                    applicationSid = params.get("Sid");
                    try {
                        applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                                .done(gson, RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() == 404) {
                            return null; // No reference found via API, cancel delete action
                        } else {
                            throw e;
                        }
                    }
                    applicationSid = applicationResponse.getSid();
                    if (applicationSid == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");
                    // Delete application
                    client.delete("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                            .done(gson, RestcommApplicationResponse.class);
                    return null;
                case CREATE:
                    applicationResponse = client.post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications.json")
                            .addParams(params).done(gson, RestcommApplicationResponse.class);
                    if (applicationResponse.getSid() == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");
                    return applicationResponse.getSid();
                case RENAME:
                    // Check if new name is already in use
                    String newFriendlyName = String.valueOf(params.get("NewFriendlyName"));
                    String newNameTemp = RvdUtils.myUrlEncode(newFriendlyName);
                    try {
                        applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + newNameTemp + ".json").done(
                                gson, RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() != 404) {
                            throw e;
                        }
                    }

                    if (applicationResponse != null)
                        throw new ApplicationAlreadyExists();

                    // Get application sid to check its existence
                    applicationSid = params.get("Sid");

                    try {
                    applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                                .done(gson, RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() == 404)
                            throw new ApplicationApiNotSynchedException("Cannot rename project '" + applicationSid
                                    + "'. The project was not found as restcomm Application.", e);
                        else
                            throw e; // else re-throw
                    }

                    applicationSid = applicationResponse.getSid();
                    if (applicationSid == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");

                    // Rename application
                    applicationResponse = client
                            .post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                            .addParam("FriendlyName", newFriendlyName)
                            .done(gson, RestcommApplicationResponse.class);
                    if (!applicationResponse.getFriendly_name().equalsIgnoreCase(newFriendlyName))
                        throw new ApplicationsApiSyncException("Fail to change the name through the API.");
                case UPDATE:
                    // Check the existence of the application
                    applicationSid = params.get("Sid");
                    try {
                        applicationResponse = client.get(
                                "/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                                .done(gson, RestcommApplicationResponse.class);
                    } catch (RestcommClientException e) {
                        if (e.getStatusCode() == 404) {
                            return null; // No reference found via API, cancel delete action
                        } else {
                            throw e;
                        }
                    }
                    applicationSid = applicationResponse.getSid();
                    if (applicationSid == null)
                        throw new ApplicationsApiSyncException("Invalid Application sid obtained from API response.");
                    // Update application
                    client.post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Applications/" + applicationSid + ".json")
                            .addParams(params).done(gson, RestcommApplicationResponse.class);
                    return null;
                default:
                    return null;
            }
        } catch (AccessApiException e) {
            if (e.getStatusCode() != null && e.getStatusCode() == 409) {
                throw new ApplicationAlreadyExists();
            } else {
                throw new ApplicationsApiSyncException(e.getMessage(), e).setStatusCode(e.getStatusCode());
            }
        } catch (ApplicationsApiSyncException e) {
            throw e; // throw ApplicationsApiSyncException* exceptions as is and not wrapped in another ApplicationsApiSyncException as done below
        } catch (Exception e) {
            throw new ApplicationsApiSyncException(e.getMessage(), e);
        }
    }
}

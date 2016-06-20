/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.identity;

import java.util.Arrays;
import java.util.UUID;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.identity.entities.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityClientRegistrationError;

import javax.ws.rs.core.MediaType;

/**
 * @author Oretis Tsakiridis
 */
public class IdentityRegistrationTool {

    static final Logger logger = Logger.getLogger(IdentityRegistrationTool.class.getName());

    private String keycloakBaseUrl;
    private String realm;
    public static String RESTCOMM_REST_CLIENT_SUFFIX = "restcomm-rest";
    public static String RESTCOMM_UI_CLIENT_SUFFIX = "restcomm-ui";
    public static String RVD_REST_CLIENT_SUFFIX = "rvd-rest";
    public static String RVD_UI_CLIENT_SUFFIX = "rvd-ui";

    public String getClientRegistrationRelativeUrl() {
        return "/realms/" + realm + "/clients-registrations/default";
    }

    public IdentityRegistrationTool(String keycloakBaseUrl) {
        this(keycloakBaseUrl, "restcomm"); // default realm
    }

    public IdentityRegistrationTool(String keycloakBaseUrl, String realm) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.realm = realm;
    }

    /**
     * Registers keycloak Clients for an identity instance using an Initial Access Token (iat). Returns a
     * new IdentityInstance object on success or null on failure.
     *
     * @param iat
     * @param redirectUrls
     * @param restcommClientSecret
     * @return the new IdentityInstance or null
     * @throws AuthServerAuthorizationError
     */
    public IdentityInstance registerInstanceWithIAT(String iat, String redirectUrls, String restcommClientSecret) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        if (redirectUrls != null && redirectUrls.endsWith("/"))
            redirectUrls = redirectUrls.substring(0, redirectUrls.length()-1); //trim trailing '/' character if present
        String instanceName = generateName();
        KeycloakClient restcommRestClient;
        KeycloakClient restcommUiClient;
        KeycloakClient rvdRestClient;
        KeycloakClient rvdUiClient;
        // create client application at keycloak side
        restcommRestClient = registerRestcommRestClient(instanceName,iat,null,restcommClientSecret,true,false);
        restcommUiClient = registerRestcommUiClient(instanceName,iat,redirectUrls,restcommClientSecret,false,true);
        rvdRestClient = registerRvdRestClient(instanceName,iat,null,restcommClientSecret,false,false);
        rvdUiClient = registerRvdUiClient(instanceName,iat,redirectUrls,restcommClientSecret,false,true);
        // for each client created, there is a Registration Access token that allows further modifying that client in the future. We keep that.
        IdentityInstance identityInstance = new IdentityInstance();
        identityInstance.setName(instanceName);
        identityInstance.setRestcommRestRAT(restcommRestClient.getRegistrationAccessToken());
        identityInstance.setRestcommUiRAT(restcommUiClient.getRegistrationAccessToken());
        identityInstance.setRvdRestRAT(rvdRestClient.getRegistrationAccessToken());
        identityInstance.setRvdUiRAT(rvdUiClient.getRegistrationAccessToken());
        return identityInstance;
    }

    public void unregisterInstanceWithRAT(IdentityInstance identityInstance) {
        unregisterClient(identityInstance.getName() + "-" + RESTCOMM_REST_CLIENT_SUFFIX, identityInstance.getRestcommRestRAT());
        unregisterClient(identityInstance.getName() + "-" + RESTCOMM_UI_CLIENT_SUFFIX, identityInstance.getRestcommUiRAT());
        unregisterClient(identityInstance.getName() + "-" + RVD_REST_CLIENT_SUFFIX, identityInstance.getRvdRestRAT());
        unregisterClient(identityInstance.getName() + "-" + RVD_UI_CLIENT_SUFFIX, identityInstance.getRvdUiRAT());
    }

    /**
     * Builds a "unique" identifier for the identity instance soon to be registered
     *
     * @return the newly generated instance name
     */
    private String generateName() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    KeycloakClient registerRestcommRestClient(String instanceName, String iat, String rootUrl, String restcommClientSecret, Boolean bearerOnly, Boolean publicClient ) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        KeycloakClient repr = new KeycloakClient();
        repr.setClientId(instanceName + "-" + RESTCOMM_REST_CLIENT_SUFFIX);
        repr.setProtocol("openid-connect");
        repr.setBearerOnly(bearerOnly);
        repr.setPublicClient(publicClient);
        return registerClient(iat,repr);    }

    KeycloakClient registerRestcommUiClient(String instanceName, String iat, String rootUrl, String restcommClientSecret, Boolean bearerOnly, Boolean publicClient ) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        KeycloakClient repr = new KeycloakClient();
        repr.setClientId(instanceName + "-" + RESTCOMM_UI_CLIENT_SUFFIX);
        repr.setProtocol("openid-connect");
        repr.setBearerOnly(bearerOnly);
        repr.setPublicClient(publicClient);
        repr.setRedirectUris(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl+"/*"}));
        repr.setWebOrigins(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl}));
        repr.setBaseUrl(rootUrl);
        return registerClient(iat,repr);
    }

    KeycloakClient registerRvdRestClient(String instanceName, String iat, String rootUrl, String restcommClientSecret, Boolean bearerOnly, Boolean publicClient ) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        KeycloakClient repr = new KeycloakClient();
        repr.setClientId(instanceName + "-" + RVD_REST_CLIENT_SUFFIX);
        repr.setProtocol("openid-connect");
        repr.setBearerOnly(bearerOnly);
        repr.setPublicClient(publicClient);
        return registerClient(iat,repr);
    }

    KeycloakClient registerRvdUiClient(String instanceName, String iat, String rootUrl, String restcommClientSecret, Boolean bearerOnly, Boolean publicClient ) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        KeycloakClient repr = new KeycloakClient();
        repr.setClientId(instanceName + "-" + RVD_UI_CLIENT_SUFFIX);
        repr.setProtocol("openid-connect");
        repr.setBearerOnly(bearerOnly);
        repr.setPublicClient(publicClient);
        repr.setRedirectUris(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl+"/restcomm-rvd/*"}));
        repr.setWebOrigins(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl}));
        repr.setBaseUrl(rootUrl+"/restcomm-rvd");
        return registerClient(iat,repr);
    }

    KeycloakClient registerClient(String iat, KeycloakClient repr) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        // create the Keycloak Client entity (not related to the jersey client class)
        try {
            Client jersey = Client.create();
            WebResource resource = jersey.resource(keycloakBaseUrl + getClientRegistrationRelativeUrl());
            // build a client representation as a JSON object

            Gson gson = new Gson();
            String json = gson.toJson(repr);
            // do create the client
            ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + iat).post(ClientResponse.class, json);
            if (response.getStatus() == 201) {
                String data = response.getEntity(String.class);
                KeycloakClient createdClient = gson.fromJson(data, KeycloakClient.class);
                return createdClient;
            } else if (response.getStatus() == 403 || response.getStatus() == 401) {
                throw new AuthServerAuthorizationError("Cannot create keycloak Client " + repr.getClientId());
            } else {
                throw new IdentityClientRegistrationError("Client registration for client '" + repr.getClientId() + "' failed with status " + response.getStatus());
            }
        } catch ( IdentityClientRegistrationError | AuthServerAuthorizationError e) {
            throw e;
        } catch (Exception e) {
            throw new IdentityClientRegistrationError("Error creating client " + repr.getClientId(),e);
        }
    }

    Integer unregisterClient(String clientId, String registrationAccessToken) {
        Client jersey = Client.create();
        WebResource resource = jersey.resource(keycloakBaseUrl + getClientRegistrationRelativeUrl() + "/" + clientId);
        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + registrationAccessToken).delete(ClientResponse.class);
        return response.getStatus();
    }
}

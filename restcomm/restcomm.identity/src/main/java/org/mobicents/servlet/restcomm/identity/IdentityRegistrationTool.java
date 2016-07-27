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
import org.keycloak.representations.idm.ClientRepresentation;
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
    public static String RESTCOMM_CLIENT_SUFFIX = "restcomm_connect";

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
    public IdentityInstance registerInstanceWithIAT(String organizationIdentityName, String iat, String redirectUrls, String restcommClientSecret) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        if (redirectUrls != null && redirectUrls.endsWith("/"))
            redirectUrls = redirectUrls.substring(0, redirectUrls.length()-1); //trim trailing '/' character if present
        //String instanceName = generateName(); //
        KeycloakClient restcommClient;
        // create client application at keycloak side
        restcommClient = registerRestcommUiClient(organizationIdentityName,iat,redirectUrls,restcommClientSecret,false,true);
        // for each client created, there is a Registration Access token that allows further modifying that client in the future. We keep that.
        IdentityInstance identityInstance = new IdentityInstance();
        identityInstance.setName(organizationIdentityName);
        identityInstance.setRestcommRAT(restcommClient.getRegistrationAccessToken());
        return identityInstance;
    }

    public void unregisterInstanceWithRAT(IdentityInstance identityInstance) {
        unregisterClient(identityInstance.getName() + "-" + RESTCOMM_CLIENT_SUFFIX, identityInstance.getRestcommRAT());
    }

    /**
     * Updates a client specified in repr using a Registration Access Token (RAT). The refreshed RAT is returned.
     * It also updated the IdentityInstance appropriately.
     *
     * Note that IdentityInstance object should be stored for future reference.
     *
     * @param repr
     * @param identityInstance
     * @param clientSuffix
     * @return
     * @throws IdentityClientRegistrationError
     * @throws AuthServerAuthorizationError
     */
    public KeycloakClient updateRegisteredClientWithRAT(KeycloakClient repr, IdentityInstance identityInstance,  String clientSuffix) throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        repr.setClientId(identityInstance.getName() + "-" + clientSuffix);
        String RAT = getRATForClientSuffix(identityInstance, clientSuffix);
        try {
            Client jersey = Client.create();
            WebResource resource = jersey.resource(keycloakBaseUrl + getClientRegistrationRelativeUrl() + "/" + repr.getClientId());
            // build a client representation as a JSON object. Only non-null fields will be effective/present
            Gson gson = new Gson();
            String json = gson.toJson(repr);
            // do create the client
            ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + RAT).put(ClientResponse.class, json);
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                String data = response.getEntity(String.class);
                KeycloakClient updatedClient = gson.fromJson(data, KeycloakClient.class);
                setRATForClientSuffix(identityInstance, clientSuffix, updatedClient.getRegistrationAccessToken());
                return updatedClient;
            } else if (response.getStatus() == 403 || response.getStatus() == 401) {
                throw new AuthServerAuthorizationError("Cannot update keycloak Client " + repr.getClientId());
            } else {
                throw new IdentityClientRegistrationError("Registered Client update failed for client '" + repr.getClientId() + "' failed with status " + response.getStatus());
            }
        } catch ( IdentityClientRegistrationError | AuthServerAuthorizationError e) {
            throw e;
        } catch (Exception e) {
            throw new IdentityClientRegistrationError("Error creating client " + repr.getClientId(),e);
        }
    }

    public static String getRATForClientSuffix(IdentityInstance identityInstance, String clientSuffix) {
        String RAT;
        if (clientSuffix.equals(RESTCOMM_CLIENT_SUFFIX)) {
            RAT = identityInstance.getRestcommRAT();
        } else
            throw new IllegalArgumentException("While trying to update client invalid client suffix was specified: " + clientSuffix );
        return RAT;
    }

    public static void setRATForClientSuffix(IdentityInstance identityInstance, String clientSuffix, String RAT) {
        if (clientSuffix.equals(RESTCOMM_CLIENT_SUFFIX)) {
            identityInstance.setRestcommRAT(RAT);
        } else
            throw new IllegalArgumentException("While trying to update client invalid client suffix was specified: " + clientSuffix );
    }

    /**
     * Builds a "unique" identifier for the identity instance soon to be registered
     *
     * @return the newly generated instance name
     */
    private String generateName() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    KeycloakClient registerRestcommUiClient(String instanceName, String iat, String rootUrl, String restcommClientSecret, Boolean bearerOnly, Boolean publicClient ) throws AuthServerAuthorizationError, IdentityClientRegistrationError {
        KeycloakClient repr = new KeycloakClient();
        repr.setClientId(buildKeycloakClientName(instanceName));
        repr.setProtocol("openid-connect");
        repr.setBearerOnly(bearerOnly);
        repr.setPublicClient(publicClient);
        repr.setRedirectUris(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl+"/*"}));
        repr.setWebOrigins(rootUrl == null ? null : Arrays.asList(new String[] {rootUrl}));
        repr.setBaseUrl(rootUrl);
        repr.setDefaultRoles(Arrays.asList(new String[] {buildKeycloakClientRole(instanceName)}));
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
            } else
            if (response.getStatus() == 400) {
                throw new IdentityClientRegistrationError("Client registration for client '" + repr.getClientId() + "' failed with status " + response.getStatus() + ". Reason: " + IdentityClientRegistrationError.Reason.CLIENT_ALREADY_THERE, IdentityClientRegistrationError.Reason.CLIENT_ALREADY_THERE);
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
        if (logger.isInfoEnabled())
            logger.info("Unregistering client '" + clientId + " - ' status: " + response.getStatus() );
        return response.getStatus();
    }

    void updateClient(String clientId, String registrationAccessToken, ClientRepresentation repr) throws IdentityClientRegistrationError, AuthServerAuthorizationError {
        try {
            Client jersey = Client.create();
            WebResource resource = jersey.resource(keycloakBaseUrl + getClientRegistrationRelativeUrl() + "/" + clientId);
            // build a client representation as a JSON object

            Gson gson = new Gson();
            String json = gson.toJson(repr);
            // do create the client
            ClientResponse response = resource.path(clientId).type(MediaType.APPLICATION_JSON_TYPE).header("Authorization", "Bearer " + registrationAccessToken).put(ClientResponse.class, json);
            if (response.getStatus() == 201) {
                String data = response.getEntity(String.class);
                if (logger.isInfoEnabled())
                    logger.info(data);
                //KeycloakClient createdClient = gson.fromJson(data, KeycloakClient.class);
                //return createdClient;
            } else if (response.getStatus() == 403 || response.getStatus() == 401) {
                throw new AuthServerAuthorizationError("Cannot update keycloak Client " + repr.getClientId());
            } else {
                throw new IdentityClientRegistrationError("Client registration for client '" + repr.getClientId() + "' failed with status " + response.getStatus());
            }
        } catch ( IdentityClientRegistrationError | AuthServerAuthorizationError e) {
            throw e;
        } catch (Exception e) {
            throw new IdentityClientRegistrationError("Error updating client " + repr.getClientId(),e);
        }
    }

    /**
     * Builds the name of the role needed to access an Organization Identity.
     *
     * Example
     *
     *  telestax-access
     *
     * @param orgIdentityName
     * @return the name of the role
     */
    public static String buildKeycloakClientRole(String orgIdentityName) {
        return orgIdentityName + "-access";
    }

    /**
     * Builds the name of the Keycloak Client that corresponds to the specified
     * Organization Identity name passed.
     *
     * Example
     *  telestax-restcomm
     *
     * @param orgIdentityName
     * @return the nanme of the Keycloak Client
     */
    public static String buildKeycloakClientName(String orgIdentityName) {
        return orgIdentityName + "-" + RESTCOMM_CLIENT_SUFFIX;
    }
}

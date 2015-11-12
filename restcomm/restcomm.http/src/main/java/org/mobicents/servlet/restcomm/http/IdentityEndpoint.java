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

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.CreateInstanceResponse;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi.RestcommIdentityApiException;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;

import com.google.gson.Gson;

/**
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
@Path("/instance")
public class IdentityEndpoint extends AccountsCommonEndpoint {

    private IdentityConfigurator identityConfigurator;
    private AccountsDao accountsDao;

    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    private void init() {
        identityConfigurator = (IdentityConfigurator) context.getAttribute(IdentityConfigurator.class.getName());
        DaoManager daoManager = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.accountsDao = daoManager.getAccountsDao();
    }

    /**
     * Registers this restcomm instance to the authorization server. username/password credentials are used for authentication.
     * The registering user is granted Administrator access to the instance and is linked to the (legacy) administrator account.
     * @param baseUrl
     * @param username
     * @param password
     * @param instanceSecret
     * @return
     */
    @POST
    @Path("/register")
    public Response registerInstance(@FormParam("restcommBaseUrl") String baseUrl, @FormParam("username") String username, @FormParam("password") String password, @FormParam("instanceSecret") String instanceSecret )  {
        if (StringUtils.isEmpty(instanceSecret))
            instanceSecret = generateInstanceSecret();

        // fail if no auth server url is set
        String authUrlBase = identityConfigurator.getAuthServerUrlBase();
        if (StringUtils.isEmpty(authUrlBase)) {
            logger.error("Missing identity.auth-server-url-base configuration setting.");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Invalid configuration").build();
        }
        // authenticate and retrieve a token
        RestcommIdentityApi api = new RestcommIdentityApi(authUrlBase, username, password);
        // update configuration
        String tokenString = api.getTokenString();
        AccessToken accessToken = IdentityUtils.verifyToken(tokenString, identityConfigurator.getUnregisteredDeployment());
        if ( accessToken == null )
            return toResponse(Outcome.NOT_ALLOWED);
        // create the instance (clients, roles, grant user roles) in keycloak
        CreateInstanceResponse response;
        try {
            response = api.createInstance(new String[] {baseUrl}, instanceSecret);
        } catch (RestcommIdentityApiException e) {
            return toResponse(e.getOutcome());
        }
        String instanceName = response.instanceId;

        // We're now registered. Update configuration.
        //identityConfigurator.setAuthServerUrlBase(authUrl);
        identityConfigurator.setMode(IdentityMode.cloud); // TODO maybe turn this to 'standalone'. Is there a difference after all between 'cloud' and 'standalone'
        identityConfigurator.setRestcommClientSecret(instanceSecret);
        identityConfigurator.setInstanceId(response.instanceId);
        identityConfigurator.save();
        logger.info( "User '" + username + "' registed instance '" + instanceName + "' to authorization server " + authUrlBase);

        // Link to existing legacy administrator account. It has to be there.
        // TODO this account is queried using the default SID. Will this assumption hold? If now another way to retrieve this account should be found.
        Account existingAccount = accountsDao.getAccount(new Sid(identityConfigurator.getLegacyAdministratorSid()));
        if ( existingAccount == null ) {
            // if the account is missing a new one is created and linked to the user
            Account newAccount = accountFromAccessToken(accessToken);
            accountsDao.addAccount(newAccount);
            logger.warn("Legacy administrator account (" + identityConfigurator.getLegacyAdministratorSid() + ") was not found. New administrator account (" + newAccount.getSid().toString() + ") was created for user '" + username + "'. Default resources may not be available.");
        } else {
            existingAccount = existingAccount.setEmailAddress(username);
            accountsDao.updateAccount(existingAccount);
            logger.info("User '" + username + "' was granted administrator access to instance '" + response.instanceId + "'");
        }

        IdentityInstanceEntity instanceEntity = new IdentityInstanceEntity();
        instanceEntity.setInstanceName(instanceName);
        Gson gson = new Gson();

        return Response.ok().entity(gson.toJson(instanceEntity)).build();
    }

    // generate a random secret for the instance/restcomm-rest client if none specified in the request
    protected String generateInstanceSecret() {
        return UUID.randomUUID().toString();
    }

    public class IdentityInstanceEntity {
        private String instanceName;

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

}

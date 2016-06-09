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

package org.mobicents.servlet.restcomm.http;

import com.google.gson.Gson;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.responseentities.IdentityInstanceEntity;
import org.mobicents.servlet.restcomm.identity.IdentityRegistrationTool;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityInstancesEndpoint extends SecuredEndpoint {

    @Context
    protected ServletContext context;
    MainConfigurationSet mainConfig;
    IdentityInstancesDao identityInstancesDao;

    @PostConstruct
    private void init() {
        // this is needed by AbstractEndpoint
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final DaoManager daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.identityInstancesDao = daos.getIdentityInstancesDao();
        mainConfig = RestcommConfiguration.getInstance().getMain();
    }


    protected Response registerIdentityInstanceWithIAT(String initialAccessToken, String redirectUrl, String keycloakBaseUrlParam) {
        String clientSecret = generateClientSecret();
        // determine keycloakBaseUrl based on configuration and defaults
        String keycloakBaseUrl = keycloakBaseUrlParam;
        if (StringUtils.isEmpty(keycloakBaseUrl))
            keycloakBaseUrl = mainConfig.getIdentityAuthServerUrl()
        // is there an IdentityInstance already for this organization ?
        IdentityInstance existingInstance = getActiveIdentityInstance();
        if (existingInstance == null) {
            IdentityRegistrationTool tool = new IdentityRegistrationTool(keycloakBaseUrl, mainConfig.getIdentityRealm());
            IdentityInstance storedInstance;
            try {
                IdentityInstance instance = tool.registerInstanceWithIAT(initialAccessToken, redirectUrl, clientSecret);
                instance.setOrganizationSid(getCurrentOrganizationSid());
                identityInstancesDao.addIdentityInstance(instance);
                storedInstance = identityInstancesDao.getIdentityInstanceByName(instance.getName());
            } catch (AuthServerAuthorizationError authServerAuthorizationError) {
                String errorResponse = "{\"error\":\"KEYCLOAK_ACCESS_ERROR\"}";
                return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponse).header("Content-Type", "application/json").build();
            }
            logger.info("registered NEW identity instance named '" + storedInstance.getName() + "' with sid: '" + storedInstance.getSid().toString() + "'");
            // TODO use a proper converter here
            Gson gson = new Gson();
            String json = gson.toJson(new IdentityInstanceEntity(storedInstance));
            return Response.ok(json).header("Content-Type", "application/json").build();
        } else
            return Response.status(Response.Status.CONFLICT).build();
    }

    protected Response getCurrentIdentityInstance() {
        IdentityInstance instance = getActiveIdentityInstance();
        // TODO use a proper converter here
        Gson gson = new Gson();
        if (instance == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else {
            String json = gson.toJson(new IdentityInstanceEntity(instance));
            return Response.ok(json).header("Content-Type", "application/json").build();
        }
    }

    protected Response unregisterIdentityInstance(String sid) {
        Sid instanceSid = new Sid(sid);
        IdentityInstance instance = identityInstancesDao.getIdentityInstance(instanceSid);
        if (instance != null) {
            IdentityRegistrationTool tool = new IdentityRegistrationTool(mainConfig.getIdentityAuthServerUrl(), mainConfig.getIdentityRealm());
            tool.unregisterInstanceWithRAT(instance);
            identityInstancesDao.removeIdentityInstance(instanceSid);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private String generateClientSecret() {
        return UUID.randomUUID().toString();
    }
}

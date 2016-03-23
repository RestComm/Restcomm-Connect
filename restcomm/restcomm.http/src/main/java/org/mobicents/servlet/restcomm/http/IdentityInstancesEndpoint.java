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
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.identity.IdentityRegistrationTool;
import org.mobicents.servlet.restcomm.identity.exceptions.InitialAccessTokenExpired;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityInstancesEndpoint extends SecuredEndpoint {

    private static String KEYCLOAK_REALM_DEFAULT = "restcomm";
    private static String KEYCLOAK_BASE_URL_DEFAULT = "https://identity.restcomm.com/auth";

    @Context
    protected ServletContext context;
    IdentityInstancesDao identityInstancesDao;

    @PostConstruct
    private void init() {
        // this is needed by AbstractEndpoint
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
    }


    protected Response registerIdentityInstanceWithIAT(String initialAccessToken, String redirectUrl, String keycloakBaseUrlParam) throws InitialAccessTokenExpired {
        String clientSecret = generateClientSecret();
        // determine keycloakBaseUrl based on configuration and defaults
        String keycloakBaseUrl = keycloakBaseUrlParam;
        if (StringUtils.isEmpty(keycloakBaseUrl))
            keycloakBaseUrl = getKeycloakBaseUrl();
        // is there an IdentityInstance already for this organization ?
        IdentityInstance existingInstance = getIdentityInstance();
        if (existingInstance == null) {
            IdentityRegistrationTool tool = new IdentityRegistrationTool(keycloakBaseUrl, getRealm());
            IdentityInstance instance = tool.registerInstanceWithIAT(initialAccessToken, new String [] {redirectUrl}, clientSecret);
            instance.setOrganizationSid(getCurrentOrganizationSid());
            identityInstancesDao.addIdentityInstance(instance);
            IdentityInstance storedInstance = identityInstancesDao.getIdentityInstanceByName(instance.getName());

            // TODO use a proper converter here
            Gson gson = new Gson();
            String json = gson.toJson(storedInstance);
            return Response.ok(json).header("Content-Type", "application/json").build();
        } else
            return Response.status(Response.Status.CONFLICT).build();
    }

    private String generateClientSecret() {
        return UUID.randomUUID().toString();
    }

    private String getRealm() {
        // TODO try first to load from configuration
        return KEYCLOAK_REALM_DEFAULT;
    }

    private String getKeycloakBaseUrl() {
        // TODO first try to load from configuration
        return KEYCLOAK_BASE_URL_DEFAULT;
    }




}

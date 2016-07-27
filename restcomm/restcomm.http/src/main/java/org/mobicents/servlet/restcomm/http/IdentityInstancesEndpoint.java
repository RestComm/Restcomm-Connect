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
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.IdentityInstanceConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.http.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.identity.IdentityRegistrationTool;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;
import org.mobicents.servlet.restcomm.identity.exceptions.IdentityClientRegistrationError;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * TODO support XML responses ? This endpoint is supposed to be used internally from AdminUI.
 *
 * @author Orestis Tsakiridis
 */
public class IdentityInstancesEndpoint extends SecuredEndpoint {

    @Context
    protected ServletContext context;
    MainConfigurationSet mainConfig;
    IdentityInstancesDao identityInstancesDao;
    protected Gson gson;
    protected XStream xstream;

    @PostConstruct
    private void init() {
        // this is needed by AbstractEndpoint
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final DaoManager daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.identityInstancesDao = daos.getIdentityInstancesDao();
        mainConfig = RestcommConfiguration.getInstance().getMain();
        // converters
        final IdentityInstanceConverter converter = new IdentityInstanceConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(IdentityInstance.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new RestCommResponseConverter(configuration));

    }


    protected Response registerIdentityInstanceWithIAT(String initialAccessToken, String redirectUrl, String keycloakBaseUrlParam) {
        String clientSecret = generateClientSecret();
        // determine keycloakBaseUrl based on configuration and defaults
        String keycloakBaseUrl = keycloakBaseUrlParam;
        if (StringUtils.isEmpty(keycloakBaseUrl))
            keycloakBaseUrl = mainConfig.getIdentityAuthServerUrl();
        // is there an IdentityInstance already for this organization ?
        if (getIdentityInstance() == null) {
            IdentityRegistrationTool tool = new IdentityRegistrationTool(keycloakBaseUrl, mainConfig.getIdentityRealm());
            IdentityInstance storedInstance;
            String orgIdentityName = null;
            try {
                orgIdentityName = pickOrganizationIdentityName();
                IdentityInstance instance = tool.registerInstanceWithIAT(orgIdentityName, initialAccessToken, redirectUrl, clientSecret);
                instance.setOrganizationSid(getCurrentOrganizationSid());
                identityInstancesDao.addIdentityInstance(instance);
                storedInstance = instance;
            } catch (AuthServerAuthorizationError e) {
                logger.error(e);
                String errorResponse = "{\"error\":\"KEYCLOAK_ACCESS_ERROR\"}";
                return Response.status(Response.Status.FORBIDDEN).entity(errorResponse).header("Content-Type", "application/json").build();
            } catch (IdentityClientRegistrationError e) {
                logger.error(e);
                if (IdentityClientRegistrationError.Reason.CLIENT_ALREADY_THERE.equals(e.getReason())) {
                    String errorResponse = "{\"error\":\""+e.getReason()+"\",\"occupiedName\":\""+orgIdentityName+"\"}";
                    return Response.status(Response.Status.CONFLICT).entity(errorResponse).type(APPLICATION_JSON_TYPE).build();
                } else
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            if (logger.isInfoEnabled())
                logger.info("registered NEW identity instance named '" + storedInstance.getName() + "' with sid: '" + storedInstance.getSid().toString() + "'");
            return Response.ok(gson.toJson(storedInstance), APPLICATION_JSON).build();
        } else
            return Response.status(Response.Status.CONFLICT).build();
    }

    protected Response getCurrentIdentityInstance() {
        // TODO use a proper converter here
        if (getIdentityInstance() == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else {
            return Response.ok(gson.toJson(getIdentityInstance()), APPLICATION_JSON).build();
        }
    }

    protected Response unregisterIdentityInstance(String sid) {
        if ( ! hasAccountRole(getAdministratorRole()) )
            throw new AuthorizationException();
        Sid instanceSid;
        try {
            instanceSid = new Sid(sid);
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        IdentityInstance instance = identityInstancesDao.getIdentityInstance(instanceSid);
        if (instance != null) {
            IdentityRegistrationTool tool = new IdentityRegistrationTool(mainConfig.getIdentityAuthServerUrl(), mainConfig.getIdentityRealm());
            tool.unregisterInstanceWithRAT(instance);
            identityInstancesDao.removeIdentityInstance(instanceSid);
            if (logger.isInfoEnabled())
                logger.info("Removed identity instance " + instanceSid);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    protected Response updateIdentityInstanceRAT(String sid, String clientSuffix, String registrationToken) {
        if (StringUtils.isEmpty(clientSuffix) || StringUtils.isEmpty(registrationToken) || StringUtils.isEmpty(sid))
            return Response.status(Response.Status.BAD_REQUEST).build();
        if (! (clientSuffix.equals(IdentityRegistrationTool.RESTCOMM_CLIENT_SUFFIX) ) )
            return Response.status(Response.Status.BAD_REQUEST).build();
        Sid instanceSid = new Sid(sid);
        IdentityInstance ii = identityInstancesDao.getIdentityInstance(instanceSid);
        if (ii != null) {
            IdentityRegistrationTool.setRATForClientSuffix(ii, clientSuffix, registrationToken);
            identityInstancesDao.updateIdentityInstance(ii);
            return Response.ok(gson.toJson(ii), APPLICATION_JSON).build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    private String generateClientSecret() {
        return UUID.randomUUID().toString();
    }

    /**
     * Implements logic for picking a name for the OrganizationIdentity. This may vary according to whether this is
     * a cloud or standalone installation. Usually the organization name itself is used or a random value to avoid
     * conflicts.
     *
     * NOTE: The algorithm is still under construction
     *
     * @return
     */
    private String pickOrganizationIdentityName() {
        return getCurrentOrganizationName();
    }
}

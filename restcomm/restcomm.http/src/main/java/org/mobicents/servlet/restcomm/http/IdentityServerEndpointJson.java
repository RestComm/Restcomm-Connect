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
import org.mobicents.servlet.restcomm.http.responseentities.IdentityServerEntity;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Orestis Tsakiridis
 */
@Path("/Identity/Server")
public class IdentityServerEndpointJson extends AbstractEndpoint {

    @Context
    protected ServletContext context;
    MainConfigurationSet config;

    @PostConstruct
    private void init() {
        // this is needed by AbstractEndpoint
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        config = RestcommConfiguration.getInstance().getMain();
    }

    @GET
    public Response getServerConfig() {
        // if no auth server has been configured return a 404
        if (StringUtils.isEmpty(config.getIdentityAuthServerUrl()))
            return Response.status(Response.Status.NOT_FOUND).build();
        // else
        IdentityServerEntity server = new IdentityServerEntity(config.getIdentityRealm(), config.getIdentityRealmPublicKey(), config.getIdentityAuthServerUrl());
        Gson gson = new Gson();
        String json = gson.toJson(server);
        return Response.ok(json).header("Content-Type", "application/json").build();
    }

}

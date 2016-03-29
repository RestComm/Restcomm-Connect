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
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.http.responseentities.IdentityServerEntity;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author Orestis Tsakiridis
 */
@Path("/Identity/Server")
public class IdentityServerEndpointJson extends AbstractEndpoint {

    IdentityConfigurationSet config;

    @PostConstruct
    private void init() {
        // this is needed by AbstractEndpoint
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        config = newConfiguration.getIdentity();
    }

    @GET
    public Response getServerConfig() {
        IdentityServerEntity server = new IdentityServerEntity(config.getRealm(), config.getRealmkey(), config.getAuthServerUrl());
        Gson gson = new Gson();
        String json = gson.toJson(server);
        return Response.ok(json).header("Content-Type", "application/json").build();
    }

}

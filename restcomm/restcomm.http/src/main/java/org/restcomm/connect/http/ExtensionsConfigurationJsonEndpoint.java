/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.http;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * Created by gvagenas on 12/10/2016.
 */
@Path("/ExtensionsConfiguration.json")
public class ExtensionsConfigurationJsonEndpoint extends ExtensionsConfigurationEndpoint {

    @Path("/{extensionId}")
    @GET
    public Response getConfigurationAsJson(@PathParam("extensionId") final String extension) {
        return getConfiguration(extension, APPLICATION_JSON_TYPE);
    }

    @POST
    public Response postConfigurationAsJson(final MultivaluedMap<String, String> data) {
        return postConfiguration(data, APPLICATION_JSON_TYPE);
    }

    @Path("/{extensionSid}")
    @POST
    public Response updateConfigurationAsJson(@PathParam("extensionSid") final String extensionSid,
                                            final MultivaluedMap<String, String> data) {
        return updateConfiguration(extensionSid, data, APPLICATION_JSON_TYPE);
    }
}

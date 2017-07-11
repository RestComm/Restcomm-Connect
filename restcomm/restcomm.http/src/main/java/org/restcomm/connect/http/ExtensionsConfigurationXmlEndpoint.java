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

/**
 * Created by gvagenas on 12/10/2016.
 */

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.restcomm.connect.commons.dao.Sid;

import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

@Path("/ExtensionsConfiguration")
public class ExtensionsConfigurationXmlEndpoint extends ExtensionsConfigurationEndpoint {

    @Path("/{extensionId}")
    @GET
    public Response getConfigurationAsXml(@PathParam("extensionId") final String extension,
            @QueryParam("AccountSid") Sid accountSid) {
        return getConfiguration(extension, accountSid, APPLICATION_XML_TYPE);
    }

    @POST
    public Response postConfigurationAsXml(final MultivaluedMap<String, String> data) {
        return postConfiguration(data, APPLICATION_XML_TYPE);
    }

    @Path("/{extensionSid}")
    @POST
    public Response updateConfigurationAsXml(@PathParam("extensionSid") final String extensionSid,
            final MultivaluedMap<String, String> data) {
        return updateConfiguration(extensionSid, data, APPLICATION_XML_TYPE);
    }
}

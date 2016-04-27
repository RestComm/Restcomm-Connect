/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author guilherme.jansen@telestax.com
 */
@Path("/Organizations")
@ThreadSafe
public class OrganizationsXmlEndpoint extends OrganizationsEndpoint {
    public OrganizationsXmlEndpoint() {
        super();
    }

    @Path("/{sid}.json")
    @DELETE
    public Response deleteOrganizationAsJson(@PathParam("sid") final String sid) {
        return deleteOrganization(sid);
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteOrganizationAsXml(@PathParam("sid") final String sid) {
        return deleteOrganization(sid);
    }

    @Path("/{sid}.json")
    @GET
    public Response getOrganizationAsJson(@PathParam("sid") final String sid) {
        return getOrganization(sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getOrganizationAsXml(@PathParam("sid") final String sid) {
        return getOrganization(sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getOrganizations() {
        return getOrganizations(APPLICATION_XML_TYPE);
    }

    @POST
    public Response putOrganization(final MultivaluedMap<String, String> data) {
        return putOrganization(data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @POST
    public Response updateOrganizationAsJsonPost(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateOrganization(sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}.json")
    @PUT
    public Response updateOrganizationAsJsonPut(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateOrganization(sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @POST
    public Response updateOrganizationAsXmlPost(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateOrganization(sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}")
    @PUT
    public Response updateClientAsXmlPut(@PathParam("sid") final String sid,
            final MultivaluedMap<String, String> data) {
        return updateOrganization(sid, data, APPLICATION_XML_TYPE);
    }

}

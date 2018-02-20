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
package org.restcomm.connect.http;

import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import static org.restcomm.connect.http.security.AccountPrincipal.ADMIN_ROLE;

/**
 * @author maria farooq
 */
@Path("/Organizations")
@ThreadSafe
@RolesAllowed(SUPER_ADMIN_ROLE)
public final class OrganizationsXmlEndpoint extends OrganizationsEndpoint {
    public OrganizationsXmlEndpoint() {
        super();
    }

    @Path("/{organizationSid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @RolesAllowed({SUPER_ADMIN_ROLE, ADMIN_ROLE})
    public Response getOrganizationAsXml(@PathParam("organizationSid") final String organizationSid,
            @Context UriInfo info) {
        return getOrganization(organizationSid, retrieveMediaType(), info);
    }

    @GET
    @RolesAllowed(SUPER_ADMIN_ROLE)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getOrganizations(@Context UriInfo info) {
        return getOrganizations(info, retrieveMediaType());
    }

    @Path("/{domainName}")
    @PUT
    @RolesAllowed(SUPER_ADMIN_ROLE)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putOrganizationPut(@PathParam("domainName") final String domainName, @Context UriInfo info) {
        return putOrganization(domainName, info, retrieveMediaType());
    }
}

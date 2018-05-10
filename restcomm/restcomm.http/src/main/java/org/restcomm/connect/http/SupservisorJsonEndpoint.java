/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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
package org.restcomm.connect.http;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@Path("/Accounts/{accountSid}/Supervisor")
@ThreadSafe
@RolesAllowed(SUPER_ADMIN_ROLE)
public class SupservisorJsonEndpoint extends SupervisorEndpoint{

    public SupservisorJsonEndpoint() {
        super();
    }

    //Simple PING/PONG message
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response ping(@PathParam("accountSid") final String accountSid) {
        return pong(accountSid, retrieveMediaType());
    }

    //Get statistics
    @Path("/metrics")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getMetrics(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
        return getMetrics(accountSid, info, retrieveMediaType());
    }

    //Get live calls
    @Path("/livecalls")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getLiveCalls(@PathParam("accountSid") final String accountSid) {
        return getLiveCalls(accountSid, retrieveMediaType());
    }

    //Register a remote location where Restcomm will send monitoring updates
    @Path("/remote")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response registerForMetricsUpdates(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
        return registerForUpdates(accountSid, info, retrieveMediaType());
    }

    //Register a remote location where Restcomm will send monitoring updates for a specific Call
    @Path("/remote/{sid}")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response registerForCallMetricsUpdates(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return registerForCallUpdates(accountSid, sid, data, retrieveMediaType());
    }
}

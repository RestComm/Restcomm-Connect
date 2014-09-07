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
package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Calls.json")
@ThreadSafe
public final class CallsJsonEndpoint extends CallsEndpoint {
    public CallsJsonEndpoint() {
        super();
    }

    // Issue 153: https://bitbucket.org/telestax/telscale-restcomm/issue/153
    // Issue 110: https://bitbucket.org/telestax/telscale-restcomm/issue/110
    @GET
    public Response getCalls(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
        return getCalls(accountSid, info, APPLICATION_JSON_TYPE);
    }

    @POST
    public Response putCall(@PathParam("accountSid") final String accountSid, final MultivaluedMap<String, String> data) {
        return putCall(accountSid, data, APPLICATION_JSON_TYPE);
    }

    // Issue 139: https://bitbucket.org/telestax/telscale-restcomm/issue/139
    @Path("/{sid}")
    @POST
    public Response modifyCall(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid,
            final MultivaluedMap<String, String> data) {
        return updateCall(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }
}

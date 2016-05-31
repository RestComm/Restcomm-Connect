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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

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
 * @author muhammad.bilal@gmail.com (Muhammad Bilal)
 */

@Path("/Accounts/{accountSid}/Queues/{queueSid}/Members.json")
@ThreadSafe
public class MembersJsonEndpoint extends MembersEndpoint {

    public MembersJsonEndpoint() {
        super();
        // TODO Auto-generated constructor stub
    }

    @GET
    public Response getQueueMembers(@PathParam("accountSid") final String accountSid,
            @PathParam("queueSid") final String queueSid, @Context UriInfo info) {
        return getQueueMembers(accountSid, queueSid, info, APPLICATION_JSON_TYPE);
    }

    @Path("/{callSid}")
    @GET
    public Response getQueueMember(@PathParam("accountSid") final String accountSid,
            @PathParam("queueSid") final String queueSid, @PathParam("callSid") final String callSid, @Context UriInfo info) {
       if(callSid.equals("Front")){
           return getFrontQueueMember(accountSid, queueSid, info, APPLICATION_JSON_TYPE);
       }else{
        return getQueueMember(accountSid, queueSid, callSid, info, APPLICATION_JSON_TYPE);
       }
    }

    @Path("/{callSid}")
    @POST
    public Response enqueue(@PathParam("accountSid") final String accountSid,
            @PathParam("queueSid") final String queueSid, @PathParam("callSid") final String callSid,final MultivaluedMap<String, String> data) {
        return enqueue(accountSid, queueSid,callSid, data, APPLICATION_JSON_TYPE);
    }

}

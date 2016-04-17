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
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@Path("/Accounts/{accountSid}/Conferences")
@ThreadSafe
public final class ConferencesXmlEndpoint extends ConferencesEndpoint {
    public ConferencesXmlEndpoint() {
        super();
    }

    @Path("/{sid}.json")
    @GET
    public Response getConferenceAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getConference(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getConferenceAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getConference(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getConferences(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
        return getConferences(accountSid, info, APPLICATION_XML_TYPE);
    }

}

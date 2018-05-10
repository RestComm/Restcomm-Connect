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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Recordings")
@ThreadSafe
public final class RecordingsXmlEndpoint extends RecordingsEndpoint {
    public RecordingsXmlEndpoint() {
        super();
    }

    @Path("/{sid}.wav")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRecordingAsWav(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getRecordingFile(accountSid, sid);
    }

    @Path("/{sid}.mp4")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRecordingAsMp4(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getRecordingFile(accountSid, sid);
    }

    @Path("/{sid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRecordingAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getRecording(accountSid, sid, retrieveMediaType());
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRecordings(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
        return getRecordings(accountSid, info, retrieveMediaType());
    }
}

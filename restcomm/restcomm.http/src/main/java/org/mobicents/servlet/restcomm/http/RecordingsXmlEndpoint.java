/*
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

import java.io.File;

import javax.ws.rs.GET;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Recordings")
@ThreadSafe
public final class RecordingsXmlEndpoint extends RecordingsEndpoint {
    public RecordingsXmlEndpoint() {
        super();
    }

    @Path("/{sid}.json")
    @GET
    public Response getRecordingAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getRecording(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}.wav")
    @GET
    public Response getRecordingAsWav(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(baseRecordingsPath).append(sid).append(".wav");
        final File file = new File(buffer.toString());
        if (!file.exists()) {
            return status(NOT_FOUND).build();
        } else {
            return ok(file, "audio/wav").build();
        }
    }

    @Path("/{sid}")
    @GET
    public Response getRecordingAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getRecording(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getRecordings(@PathParam("accountSid") final String accountSid) {
        return getRecordings(accountSid, APPLICATION_XML_TYPE);
    }
}

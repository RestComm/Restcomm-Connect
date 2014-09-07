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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Transcriptions")
public final class TranscriptionsXmlEndpoint extends TranscriptionsEndpoint {
    public TranscriptionsXmlEndpoint() {
        super();
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteTranscription(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
        try {
            secure(super.accountsDao.getAccount(accountSid), "RestComm:Delete:Transcriptions");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        dao.removeTranscription(new Sid(sid));
        return ok().build();
    }

    @Path("/{sid}.json")
    @GET
    public Response getTranscriptionAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getTranscription(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getTranscriptionAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getTranscription(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getTranscriptions(@PathParam("accountSid") final String accountSid) {
        return getTranscriptions(accountSid, APPLICATION_XML_TYPE);
    }
}

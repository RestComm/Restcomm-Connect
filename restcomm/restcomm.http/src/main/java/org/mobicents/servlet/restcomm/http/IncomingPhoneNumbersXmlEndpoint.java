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

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/IncomingPhoneNumbers")
@ThreadSafe
public final class IncomingPhoneNumbersXmlEndpoint extends IncomingPhoneNumbersEndpoint {
    public IncomingPhoneNumbersXmlEndpoint() {
        super();
    }

    private Response deleteIncomingPhoneNumber(final String accountSid, final String sid) {
        try {
            secure(super.accountsDao.getAccount(accountSid), "RestComm:Delete:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        dao.removeIncomingPhoneNumber(new Sid(sid));
        return ok().build();
    }

    @Path("/{sid}.json")
    @DELETE
    public Response deleteIncomingPhoneNumberAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteIncomingPhoneNumber(accountSid, sid);
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteIncomingPhoneNumber(accountSid, sid);
    }

    @Path("/{sid}.json")
    @GET
    public Response getIncomingPhoneNumberAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getIncomingPhoneNumber(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getIncomingPhoneNumber(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getIncomingPhoneNumbers(@PathParam("accountSid") final String accountSid) {
        return getIncomingPhoneNumbers(accountSid, APPLICATION_XML_TYPE);
    }

    @POST
    public Response putIncomingPhoneNumber(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @PUT
    public Response updateIncomingPhoneNumberAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateIncomingPhoneNumber(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}.json")
    @POST
    public Response updateIncomingPhoneNumberAsJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateIncomingPhoneNumber(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @PUT
    public Response updateIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateIncomingPhoneNumber(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}")
    @POST
    public Response updateIncomingPhoneNumberAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateIncomingPhoneNumber(accountSid, sid, data, APPLICATION_XML_TYPE);
    }
}

/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.dao.entities.Sid;

/**
 * @author guilherme.jansen@telestax.com
 */
@Path("/Accounts/{accountSid}/Applications")
@ThreadSafe
public class ApplicationsXmlEndpoint extends ApplicationsEndpoint {
    public ApplicationsXmlEndpoint() {
        super();
    }

    private Response deleteApplication(final String accountSid, final String sid) {
        Account operatedAccount = accountsDao.getAccount(new Sid(accountSid));
        secure(operatedAccount, "RestComm:Modify:Applications", SecuredType.SECURED_APP);
        Application application = dao.getApplication(new Sid(sid));
        if (application != null) {
            secure(operatedAccount, application.getAccountSid(), SecuredType.SECURED_APP);
        }
        dao.removeApplication(new Sid(sid));
        return ok().build();
    }

    @GET
    public Response getApplications(@PathParam("accountSid") final String accountSid) {
        return getApplications(accountSid, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getApplicationAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getApplication(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @GET
    public Response getApplicationAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getApplication(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @POST
    public Response putApplication(@PathParam("accountSid") String accountSid, final MultivaluedMap<String, String> data) {
        return putApplication(accountSid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @POST
    public Response updateApplicationAsJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateApplication(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @POST
    public Response updateApplicationAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateApplication(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @PUT
    public Response updateApplicationAsJsonPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateApplication(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @PUT
    public Response updateApplicationAsXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateApplication(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @DELETE
    public Response deleteApplicationAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return deleteApplication(accountSid, sid);
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteApplicationAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return deleteApplication(accountSid, sid);
    }

}

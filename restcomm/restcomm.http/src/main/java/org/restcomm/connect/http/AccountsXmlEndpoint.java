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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts")
@ThreadSafe
public final class AccountsXmlEndpoint extends AccountsEndpoint {
    public AccountsXmlEndpoint() {
        super();
    }

    /*
    @Path("/{sid}")
    @DELETE
    public Response deleteAccountAsXml(@PathParam("sid") final String sid) {
        return deleteAccount(sid);
    }
    */

    @Path("/{accountSid}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getAccountAsXml(@PathParam("accountSid") final String accountSid,
            @Context UriInfo info) {
        return getAccount(accountSid, retrieveMediaType(), info);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getAccounts(@Context UriInfo info) {
        return getAccounts(info, retrieveMediaType());
    }

    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putAccount(final MultivaluedMap<String, String> data) {
        return putAccount(data, retrieveMediaType());
    }

    //The {accountSid} could be the email address of the account we need to update. Later we check if this is SID or EMAIL
    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateAccountAsXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, retrieveMediaType());
    }

    //The {accountSid} could be the email address of the account we need to update. Later we check if this is SID or EMAIL
    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateAccountAsXmlPut(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, retrieveMediaType());
    }

    @Path("/migrate/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    @RolesAllowed(SUPER_ADMIN_ROLE)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response migrateAccount(@PathParam("accountSid") final String accountSid, final MultivaluedMap<String, String> data) {
        return migrateAccountOrganization(accountSid, data, retrieveMediaType());
    }
}

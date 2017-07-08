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

import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts.json")
@ThreadSafe
public final class AccountsJsonEndpoint extends AccountsEndpoint {
    public AccountsJsonEndpoint() {
        super();
    }

    @Path("/{accountSid}")
    @GET
    public Response getAccountAsJson(@PathParam("accountSid") final String accountSid) {
        return getAccount(accountSid, APPLICATION_JSON_TYPE);
    }

    @Path("/{accountSid}")
    @OPTIONS
    public Response optionsAccount(@PathParam("accountSid") final String accountSid) {
        // no authentication here since this is a pre-flight request
        return Response.ok().build();
    }

    @GET
    public Response getAccounts() {
        return getAccounts(APPLICATION_JSON_TYPE);
    }

    /* disabled as #1270
    @Path("/{sid}.json")
    @DELETE
    public Response deleteAccountAsJson(@PathParam("sid") final String sid) {
        return deleteAccount(sid);
    }
    */

    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response putAccount(final MultivaluedMap<String, String> data) {
        return putAccount(data, APPLICATION_JSON_TYPE);
    }

    //The {accountSid} could be the email address of the account we need to update. Later we check if this is SID or EMAIL
    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response updateAccountAsJsonPost(@PathParam("accountSid") final String accountSid,
                                            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
    }

    //The {accountSid} could be the email address of the account we need to update. Later we check if this is SID or EMAIL
    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PUT
    public Response updateAccountAsJsonPut(@PathParam("accountSid") final String accountSid,
                                           final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
    }


}

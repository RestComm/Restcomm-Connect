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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

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

    @Path("/import")
    @GET
    public Response importCurrentAccount() {
        return importKeycloakAccount();
    }

    @GET
    public Response getAccounts() {
        return getAccounts(APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}.json")
    @DELETE
    public Response deleteAccountAsJson(@PathParam("sid") final String sid) {
        return deleteAccount(sid);
    }

    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response putAccount(final MultivaluedMap<String, String> data) {
        return putAccount(data, APPLICATION_JSON_TYPE);
    }
}

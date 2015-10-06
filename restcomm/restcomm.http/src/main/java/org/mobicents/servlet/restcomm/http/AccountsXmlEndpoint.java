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
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.endpoints.Outcome;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts")
@ThreadSafe
public final class AccountsXmlEndpoint extends AccountsEndpoint {
    public AccountsXmlEndpoint() {
        super();
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteAccountAsXml(@PathParam("sid") final String sid) {
        return deleteAccount(sid);
    }

    @Path("/{accountSid}")
    @GET
    public Response getAccountAsXml(@PathParam("accountSid") final String accountSid) {
        return getAccount(accountSid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getAccounts() {
        return getAccounts(APPLICATION_XML_TYPE);
    }

    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response putAccount(final MultivaluedMap<String, String> data) {
        return putAccount(data, APPLICATION_XML_TYPE);
    }

    @Path("/{accountSid}.json")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response updateAccountAsJsonPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{accountSid}.json")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PUT
    public Response updateAccountAsJsonPut(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @POST
    public Response updateAccountAsXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{accountSid}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @PUT
    public Response updateAccountAsXmlPut(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return updateAccount(accountSid, data, APPLICATION_XML_TYPE);
    }

    @POST
    @Path("/{accountSid}/operations/link")
    public Response linkAccount(@PathParam("accountSid") String accountSid, @FormParam("username") String username, @FormParam("create") String create, @FormParam("friendly_name") String friendly_name, @FormParam("password") String password) {
        // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        if ( "true".equals(create) && !StringUtils.isEmpty(username) ) {
            Outcome create_outcome = createUser(username, friendly_name, password);
            if ( create_outcome == Outcome.OK )
                return toResponse(linkAccountToUser(account, username));
            else
                return toResponse(create_outcome);
        } else {
            return toResponse(linkAccountToUser(account, username));
        }
    }

    @DELETE
    @Path("/{accountSid}/operations/link")
    public Response unlinkAccount(@PathParam("accountSid") String accountSid) {
     // TODO - access control
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return unlinkAccountFromUser(account);
    }

    @DELETE
    @Path("/{accountSid}/operations/key")
    public Response removeApikey(@PathParam("accountSid") String accountSid) {
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return clearAccountKey(account);
    }

    @GET
    @Path("/{accountSid}/operations/key/assign")
    public Response assignApikey(@PathParam("accountSid") String accountSid) {
        Sid sid = new Sid(accountSid);
        Account account = accountsDao.getAccount(sid);
        return assignApikey(account);
    }

}

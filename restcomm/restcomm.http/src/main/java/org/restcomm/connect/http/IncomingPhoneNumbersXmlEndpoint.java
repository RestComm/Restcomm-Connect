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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.http.filters.ExtensionFilter;
import org.restcomm.connect.provisioning.number.api.PhoneNumberType;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@Path("/Accounts/{accountSid}/IncomingPhoneNumbers")
@ThreadSafe
public final class IncomingPhoneNumbersXmlEndpoint extends IncomingPhoneNumbersEndpoint {
    public IncomingPhoneNumbersXmlEndpoint() {
        super();
    }


    @Path("/{sid}")
    @DELETE
    public Response deleteIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return super.deleteIncomingPhoneNumber(accountSid, sid);
    }

    @Path("/{sid}")
    @GET
    public Response getIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getIncomingPhoneNumber(accountSid, sid, acceptType);
    }


    @Path("/AvailableCountries")
    @GET
    public Response getAvailableCountriesAsXml(@PathParam("accountSid") final String accountSid,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getAvailableCountries(accountSid, acceptType);
    }

    @GET
    public Response getIncomingPhoneNumbers(@PathParam("accountSid") final String accountSid,@Context UriInfo info,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getIncomingPhoneNumbers(accountSid, PhoneNumberType.Global,info, acceptType);
    }

    @POST
    @ResourceFilters({ ExtensionFilter.class })
    public Response putIncomingPhoneNumber(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Global, acceptType);
    }

    @Path("/{sid}")
    @PUT
    public Response updateIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return updateIncomingPhoneNumber(accountSid, sid, data, acceptType);
    }

    @Path("/{sid}")
    @POST
    public Response updateIncomingPhoneNumberAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return updateIncomingPhoneNumber(accountSid, sid, data, acceptType);
    }

    // Local Numbers

    @Path("/Local")
    @GET
    public Response getIncomingLocalPhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.Local,info, acceptType);
    }

    @Path("/Local")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    public Response putIncomingLocalPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Local, acceptType);
    }

    // Toll Free Numbers

    @Path("/TollFree")
    @GET
    public Response getIncomingTollFreePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.TollFree,info, acceptType);
    }

    @Path("/TollFree")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    public Response putIncomingTollFreePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.TollFree, acceptType);
    }

    // Mobile Numbers

    @Path("/Mobile")
    @GET
    public Response getIncomingMobilePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.Mobile,info, acceptType);
    }

    @Path("/Mobile")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    public Response putIncomingMobilePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Mobile, acceptType);
    }


    @Path("/migrate")
    @POST
    @RolesAllowed(SUPER_ADMIN_ROLE)
    public Response migrateIncomingPhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return migrateIncomingPhoneNumbers(accountSid, data, acceptType);
    }

    @Path("/migrate.json")
    @POST
    @RolesAllowed(SUPER_ADMIN_ROLE)
    public Response migrateIncomingPhoneNumbersAsJson(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data,
            @HeaderParam("Accept") String accept) {
        MediaType acceptType = MediaType.valueOf(accept);
        return migrateIncomingPhoneNumbers(accountSid, data, acceptType);
    }
}

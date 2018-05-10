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

import com.sun.jersey.spi.container.ResourceFilters;
import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        MediaType acceptType = retrieveMediaType();
        return getIncomingPhoneNumber(accountSid, sid, acceptType);
    }


    @Path("/AvailableCountries")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getAvailableCountriesAsXml(@PathParam("accountSid") final String accountSid) {
        MediaType acceptType = retrieveMediaType();
        return getAvailableCountries(accountSid, acceptType);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getIncomingPhoneNumbers(@PathParam("accountSid") final String accountSid,@Context UriInfo info) {
        MediaType acceptType = retrieveMediaType();
        return getIncomingPhoneNumbers(accountSid, PhoneNumberType.Global,info, acceptType);
    }

    @POST
    @ResourceFilters({ ExtensionFilter.class })
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putIncomingPhoneNumber(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Global, acceptType);
    }

    @Path("/{sid}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return updateIncomingPhoneNumber(accountSid, sid, data, acceptType);
    }

    @Path("/{sid}")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateIncomingPhoneNumberAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return updateIncomingPhoneNumber(accountSid, sid, data, acceptType);
    }

    // Local Numbers

    @Path("/Local")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getIncomingLocalPhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info) {
        MediaType acceptType = retrieveMediaType();
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.Local,info, acceptType);
    }

    @Path("/Local")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putIncomingLocalPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Local, acceptType);
    }

    // Toll Free Numbers

    @Path("/TollFree")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getIncomingTollFreePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info) {
        MediaType acceptType = retrieveMediaType();
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.TollFree,info, acceptType);
    }

    @Path("/TollFree")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putIncomingTollFreePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.TollFree, acceptType);
    }

    // Mobile Numbers

    @Path("/Mobile")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getIncomingMobilePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,@Context UriInfo info) {
        MediaType acceptType = retrieveMediaType();
        return getIncomingPhoneNumbers(accountSid,PhoneNumberType.Mobile,info, acceptType);
    }

    @Path("/Mobile")
    @POST
    @ResourceFilters({ ExtensionFilter.class })
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response putIncomingMobilePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Mobile, acceptType);
    }


    @Path("/migrate")
    @POST
    @RolesAllowed(SUPER_ADMIN_ROLE)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response migrateIncomingPhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        MediaType acceptType = retrieveMediaType();
        return migrateIncomingPhoneNumbers(accountSid, data, acceptType);
    }
}

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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberType;

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

    @Path("/{sid}.json")
    @DELETE
    public Response deleteIncomingPhoneNumberAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return super.deleteIncomingPhoneNumber(accountSid, sid);
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteIncomingPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return super.deleteIncomingPhoneNumber(accountSid, sid);
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

    @Path("/AvailableCountries.json")
    @GET
    public Response getAvailableCountriesAsJson(@PathParam("accountSid") final String accountSid) {
        return getAvailableCountries(accountSid, APPLICATION_JSON_TYPE);
    }

    @Path("/AvailableCountries")
    @GET
    public Response getAvailableCountriesAsXml(@PathParam("accountSid") final String accountSid) {
        return getAvailableCountries(accountSid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getIncomingPhoneNumbers(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.Global, APPLICATION_XML_TYPE);
    }

    @POST
    public Response putIncomingPhoneNumber(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Global, APPLICATION_XML_TYPE);
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

    // Local Numbers

    @Path("/Local")
    @GET
    public Response getIncomingLocalPhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.Local, APPLICATION_XML_TYPE);
    }

    @Path("/Local")
    @POST
    public Response putIncomingLocalPhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Local, APPLICATION_XML_TYPE);
    }

    @Path("/Local.json")
    @GET
    public Response getIncomingLocalPhoneNumbersAsJSon(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.Local, APPLICATION_JSON_TYPE);
    }

    @Path("/Local.json")
    @POST
    public Response putIncomingLocalPhoneNumberAsJSon(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Local, APPLICATION_JSON_TYPE);
    }

    // Toll Free Numbers

    @Path("/TollFree")
    @GET
    public Response getIncomingTollFreePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.TollFree, APPLICATION_XML_TYPE);
    }

    @Path("/TollFree")
    @POST
    public Response putIncomingTollFreePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.TollFree, APPLICATION_XML_TYPE);
    }

    @Path("/TollFree.json")
    @GET
    public Response getIncomingTollFreePhoneNumbersAsJSon(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.TollFree, APPLICATION_JSON_TYPE);
    }

    @Path("/TollFree.json")
    @POST
    public Response putIncomingTollFreePhoneNumberAsJSon(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.TollFree, APPLICATION_JSON_TYPE);
    }

    // Mobile Numbers

    @Path("/Mobile")
    @GET
    public Response getIncomingMobilePhoneNumbersAsXml(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.Mobile, APPLICATION_XML_TYPE);
    }

    @Path("/Mobile")
    @POST
    public Response putIncomingMobilePhoneNumberAsXml(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Mobile, APPLICATION_XML_TYPE);
    }

    @Path("/Mobile.json")
    @GET
    public Response getIncomingMobilePhoneNumbersAsJSon(@PathParam("accountSid") final String accountSid,
            @QueryParam("PhoneNumber") final String phoneNumber, @QueryParam("FriendlyName") final String friendlyName) {
        return getIncomingPhoneNumbers(accountSid, phoneNumber, friendlyName, PhoneNumberType.Mobile, APPLICATION_JSON_TYPE);
    }

    @Path("/Mobile.json")
    @POST
    public Response putIncomingMobilePhoneNumberAsJSon(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putIncomingPhoneNumber(accountSid, data, PhoneNumberType.Mobile, APPLICATION_JSON_TYPE);
    }
}

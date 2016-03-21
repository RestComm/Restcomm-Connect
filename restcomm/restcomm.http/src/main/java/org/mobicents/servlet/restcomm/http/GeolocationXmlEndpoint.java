/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Geolocation;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
@Path("/Accounts/{accountSid}/Geolocation")
@ThreadSafe
public final class GeolocationXmlEndpoint extends GeolocationEndpoint {

    public GeolocationXmlEndpoint() {
        super();
    }

    // *** Immediate type of Geolocation *** //

    @Path("/Immediate.json")
    @DELETE
    public Response deleteImmediateGeolocationJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Immediate/{sid}.json")
    @DELETE
    public Response deleteImmediateGeolocationAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Immediate")
    @DELETE
    public Response deleteImmediateGeolocationXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Immediate/{sid}")
    @DELETE
    public Response deleteImmediateGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Immediate.json")
    @GET
    public Response getImmediateGeolocationJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/Immediate/{sid}.json")
    @GET
    public Response getImmediateGeolocationAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/Immediate")
    @GET
    public Response getImmediateGeolocationXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @Path("/Immediate/{sid}")
    @GET
    public Response getImmediateGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @Path("/Immediate.json")
    @POST
    public Response updateImmediateGeolocationJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Immediate, APPLICATION_JSON_TYPE);
    }

    @Path("/Immediate/{sid}.json")
    @POST
    public Response updateImmediateGeolocationAsJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/Immediate/{sid}.json")
    @PUT
    public Response updateImmediateGeolocationAsJsonPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/Immediate")
    @POST
    public Response putImmediateGeolocationXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Immediate, APPLICATION_XML_TYPE);
    }

    @Path("/Immediate/{sid}")
    @POST
    public Response putImmediateGeolocationAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/Immediate")
    @PUT
    public Response updateImmediateGeolocationXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/Immediate/{sid}")
    @PUT
    public Response updateImmediateGeolocationAsXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    // *** Notification type of Geolocation *** //

    @Path("/Notification.json")
    @DELETE
    public Response deleteNotificationGeolocationJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Notification/{sid}.json")
    @DELETE
    public Response deleteNotificationGeolocationAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Notification")
    @DELETE
    public Response deleteNotificationGeolocationXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Notification/{sid}")
    @DELETE
    public Response deleteNotificationGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return deleteGeolocation(accountSid, sid);
    }

    @Path("/Notification.json")
    @GET
    public Response getNotificationGeolocationJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/Notification/{sid}.json")
    @GET
    public Response getNotificationGeolocationAsJson(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/Notification")
    @GET
    public Response getNotificationGeolocationXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @Path("/Notification/{sid}")
    @GET
    public Response getNotificationGeolocationAsXml(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid) {
        return getGeolocation(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @Path("/Notification.json")
    @POST
    public Response updateNotificationGeolocationJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Notification, APPLICATION_JSON_TYPE);
    }

    @Path("/Notification/{sid}.json")
    @POST
    public Response updateNotificationGeolocationAsJsonPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/Notification/{sid}.json")
    @PUT
    public Response updateNotificationGeolocationAsJsonPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/Notification")
    @POST
    public Response putNotificationGeolocationXmlPost(@PathParam("accountSid") final String accountSid,
            final MultivaluedMap<String, String> data) {
        return putGeolocation(accountSid, data, Geolocation.GeolocationType.Notification, APPLICATION_XML_TYPE);
    }

    @Path("/Notification/{sid}")
    @POST
    public Response putNotificationGeolocationAsXmlPost(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/Notification")
    @PUT
    public Response updateNotificationGeolocationXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/Notification/{sid}")
    @PUT
    public Response updateNotificationGeolocationAsXmlPut(@PathParam("accountSid") final String accountSid,
            @PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGeolocation(accountSid, sid, data, APPLICATION_XML_TYPE);
    }

    //

    @GET
    public Response getGeolocationsAsXml(@PathParam("accountSid") final String accountSid) {
        return getGeolocations(accountSid, APPLICATION_XML_TYPE);
    }

    // @POST public Response putGeolocation(@PathParam("accountSid") final String accountSid, final
    // MultivaluedMap<String,String> data) { return putGeolocation(accountSid, data, APPLICATION_XML_TYPE); }

}
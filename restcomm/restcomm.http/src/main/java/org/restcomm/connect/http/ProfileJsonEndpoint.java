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

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

@Path("/Profiles")
@ThreadSafe
public class ProfileJsonEndpoint extends ProfileEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    public Response getProfilesAsJson(@Context UriInfo info) {
        return getProfiles(info);
    }

    @POST
    @Consumes(PROFILE_CONTENT_TYPE)
    @Produces(PROFILE_CONTENT_TYPE)
    public Response createProfileAsJson(InputStream body) {
        return createProfile(body);
    }

    @Path("/{profileSid}")
    @GET
    @Produces(PROFILE_CONTENT_TYPE)
    public Response getProfileAsJson(@PathParam("profileSid") final String profileSid) {
        return getProfile(profileSid);
    }

    @Path("/{profileSid}")
    @PUT
    @Consumes(PROFILE_CONTENT_TYPE)
    public Response updateProfileAsJson(@PathParam("profileSid") final String profileSid,
            InputStream body) {
        return updateProfile(profileSid, body);
    }

    @Path("/{profileSid}")
    @DELETE
    public Response deleteProfileAsJson(@PathParam("profileSid") final String profileSid) {
        return deleteProfile(profileSid);
    }

    @Path("/{profileSid}")
    @LINK
    public Response linkProfileAsJson(@PathParam("profileSid") final String profileSid,
            @Context HttpHeaders headers
    ) {
        return linkProfile(profileSid, headers);
    }

    @Path("/{profileSid}")
    @UNLINK
    public Response unlinkProfileAsJson(@PathParam("profileSid") final String profileSid,
            @Context HttpHeaders headers) {
        return unlinkProfile(profileSid, headers);
    }

    @Path("/rc-profile-schema")
    @GET
    @Produces(PROFILE_SCHEMA_CONTENT_TYPE)
    public Response getProfileSchemaAsJson() {
        return getProfileSchema();
    }

}

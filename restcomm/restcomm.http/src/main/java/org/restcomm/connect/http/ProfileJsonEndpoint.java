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

import com.sun.jersey.spi.resource.Singleton;
import java.io.InputStream;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import static org.restcomm.connect.http.ProfileEndpoint.PROFILE_CONTENT_TYPE;
import static org.restcomm.connect.http.ProfileEndpoint.PROFILE_SCHEMA_CONTENT_TYPE;
import static org.restcomm.connect.http.security.AccountPrincipal.SUPER_ADMIN_ROLE;

@Path("/Profiles")
@ThreadSafe
@RolesAllowed(SUPER_ADMIN_ROLE)
@Singleton
public class ProfileJsonEndpoint extends ProfileEndpoint {

    private static final String OVERRIDE_HDR = "X-HTTP-Method-Override";

    @GET
    @Produces(APPLICATION_JSON)
    public Response getProfilesAsJson(@Context UriInfo info) {
        return getProfiles(info);
    }

    @POST
    @Consumes({PROFILE_CONTENT_TYPE, APPLICATION_JSON})
    @Produces({PROFILE_CONTENT_TYPE, APPLICATION_JSON})
    public Response createProfileAsJson(InputStream body, @Context UriInfo info) {
        return createProfile(body, info);
    }

    @Path("/{profileSid}")
    @GET
    @Produces({PROFILE_CONTENT_TYPE, MediaType.APPLICATION_JSON})
    @PermitAll
    public Response getProfileAsJson(@PathParam("profileSid") final String profileSid,
            @Context UriInfo info, @Context SecurityContext secCtx) {
        return getProfile(profileSid, info, secCtx);
    }

    @Path("/{profileSid}")
    @PUT
    @Consumes({PROFILE_CONTENT_TYPE, MediaType.APPLICATION_JSON})
    @Produces({PROFILE_CONTENT_TYPE, MediaType.APPLICATION_JSON})
    public Response updateProfileAsJson(@PathParam("profileSid") final String profileSid,
            InputStream body, @Context UriInfo info,
            @Context HttpHeaders headers) {
        if (headers.getRequestHeader(OVERRIDE_HDR) != null
                && headers.getRequestHeader(OVERRIDE_HDR).size() > 0) {
            String overrideHdr = headers.getRequestHeader(OVERRIDE_HDR).get(0);
            switch (overrideHdr) {
                case "LINK":
                    return linkProfile(profileSid, headers, info);
                case "UNLINK":
                    return unlinkProfile(profileSid, headers);

            }
        }
        return updateProfile(profileSid, body, info);
    }

    @Path("/{profileSid}")
    @DELETE
    public Response deleteProfileAsJson(@PathParam("profileSid") final String profileSid) {
        return deleteProfile(profileSid);
    }

    @Path("/{profileSid}")
    @LINK
    @Produces(APPLICATION_JSON)
    public Response linkProfileAsJson(@PathParam("profileSid") final String profileSid,
            @Context HttpHeaders headers, @Context UriInfo info
    ) {
        return linkProfile(profileSid, headers, info);
    }

    @Path("/{profileSid}")
    @UNLINK
    @Produces(APPLICATION_JSON)
    public Response unlinkProfileAsJson(@PathParam("profileSid") final String profileSid,
            @Context HttpHeaders headers) {
        return unlinkProfile(profileSid, headers);
    }

    @Path("/schemas/{schemaId}")
    @GET
    @Produces({PROFILE_SCHEMA_CONTENT_TYPE, MediaType.APPLICATION_JSON})
    @PermitAll
    public Response getProfileSchemaAsJson(@PathParam("schemaId") final String schemaId) {
        return getSchema(schemaId);
    }

}

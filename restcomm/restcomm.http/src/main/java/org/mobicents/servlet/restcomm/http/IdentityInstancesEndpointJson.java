/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Orestis Tsakiridis.
 */
@Path("/IdentityInstances")
public class IdentityInstancesEndpointJson extends SecuredEndpoint {

    /**
     *
     * @param initialAccessToken
     * @param redirectUrl
     * @return
     */
    @POST
    public Response registerIdentityInstance(@FormParam("InitialAccessToken") String initialAccessToken, @FormParam("RedirectUrl") String redirectUrl) {
        return Response.ok().build();
    }

    /**
     * Returns all Identity Instances. Only (super) administrators can access this.
     *
     * @return
     */
    @GET
    public Response getIdentityInstances() {
        return Response.ok().build();
    }

    /**
     * Returns a single Identity Instance. Only (super) administrators can access this.
     * @return
     */
    @Path("/{identityInstanceSid}")
    @GET
    public Response getIdentityInstance() {
        return Response.ok().build();
    }

    /**
     * Returns the current instance based the active organization specified in url (domain name). If no identity
     * instances have been created 404 is returned. Otherwise a small subset of IdentityInstance entity is returned:
     *
     * {sid:II123123, organizationSid, name:abq1231231, dateCreated, dateUpdated}
     *
     * Access to this function will be public if the instance (or the organizatio) is not registered. Otherwise a bearer
     * token will be required.
     *
     * @return 404 or an IdentityInstance subset
     */
    @GET
    @Path("/current")
    public Response getCurrentIdentityInstance() {
        return Response.ok().build();
    }

    /**
     * Unregisters an instance. Only (super) administrators can access this.
     * @param identityInstanceSid
     *
     * @return nothing
     */
    @DELETE
    @Path("/{identityInstanceSid}")
    public Response unregisterIdentityInstance(@PathParam("sid") String identityInstanceSid) {
        return Response.ok().build();
    }

    /**
     * Register a restcomm instance to an identity server using an Initial Access Token (iat).
     *
     * @param iat
     */
    private void registerIdentityInstanceWithIAT(String iat, String redirectUrl, String restcommClientSecret) {

    }


}

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

import org.apache.commons.lang.NotImplementedException;
import org.mobicents.servlet.restcomm.identity.exceptions.AuthServerAuthorizationError;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * @author Orestis Tsakiridis.
 */
@Path("/IdentityInstances")
public class IdentityInstancesEndpointJson extends IdentityInstancesEndpoint {

    /**
     *
     * @param initialAccessToken
     * @param redirectUrl
     * @return
     */
    @POST
    public Response registerIdentityInstance(@FormParam("InitialAccessToken") String initialAccessToken, @FormParam("RedirectUrl") String redirectUrl, @FormParam("KeycloakBaseUrl") String keycloakBaseUrl) throws AuthServerAuthorizationError {
        return registerIdentityInstanceWithIAT(initialAccessToken, redirectUrl, keycloakBaseUrl);
    }

    /**
     * Returns all Identity Instances. Only (super) administrators can access this.
     *
     * @return
     */
    @GET
    public Response getIdentityInstances() {
        throw new NotImplementedException();
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
        return super.getCurrentIdentityInstance();
    }
    /**
     * Returns a single Identity Instance. Only (super) administrators can access this.
     * @return
     */
    @Path("/{identityInstanceSid}")
    @GET
    public Response getIdentityInstanceBySid() {
        throw new NotImplementedException();
    }

    /**
     * Unregisters an instance. Only (super) administrators can access this.
     * @param identityInstanceSid
     *
     * @return nothing
     */
    @DELETE
    @Path("/{identityInstanceSid}")
    public Response unregisterIdentityInstance(@PathParam("identityInstanceSid") String identityInstanceSid) {
        return super.unregisterIdentityInstance(identityInstanceSid);
    }
}

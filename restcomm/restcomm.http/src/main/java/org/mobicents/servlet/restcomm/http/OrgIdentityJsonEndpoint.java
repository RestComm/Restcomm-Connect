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

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Orestis Tsakiridis.
 */
@Path("/Identity/Instances")
public class OrgIdentityJsonEndpoint extends OrgIdentityEndpoint {

    /**
     * Returns all Identity Instances. Only (super) administrators can access this.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrgIdentities() {
        throw new NotImplementedException();
    }

    /**
     * Returns a single Identity Instance. Only (super) administrators can access this.
     * @return
     */
    @Path("/{identityInstanceSid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrgIdentityBySid() {
        throw new NotImplementedException();
    }

    /**
     * Returns the current instance based the active organization specified in url (domain name). If no identity
     * instances have been created 404 is returned. Only a small subset of the whole OrgIdentity entity is returned:
     *
     * {sid:II123123, organizationSid, name:abq1231231}
     *
     * Access to this function will be public if the instance (or the organizatio) is not registered. Otherwise a bearer
     * token will be required.
     *
     * @return 404 or an OrgIdentity subset
     */
    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentOrgIdentity() {
        return super.getCurrentOrgIdentity();
    }

    @POST
    public Response createOrgIdentity(
            @FormParam("Name") String name,
            @FormParam("Organization") String organizationDomain) {
        return super.createOrgIdentity(name, organizationDomain);
    }

    @Path("/{OrgIdentitySid}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrgIdentityBySid(@PathParam("OrgIdentitySid") String orgIdentitySid, @FormParam("Name") String name) {
        return updateOrgIdentity(orgIdentitySid,name);
    }

    /**
     * Unregisters an instance. Only (super) administrators can access this.
     * @param orgIdentitySid
     *
     * @return nothing
     */
    @DELETE
    @Path("/{OrgIdentitySid}")
    public Response removeOrgIdentity(@PathParam("OrgIdentitySid") String orgIdentitySid) {
        //if (mainConfig.getIdentityAuthServerUrl() != null)
            return super.removeOrgIdentity(orgIdentitySid);
        //else
        //    return Response.status(Response.Status.NOT_FOUND).build();
    }
}

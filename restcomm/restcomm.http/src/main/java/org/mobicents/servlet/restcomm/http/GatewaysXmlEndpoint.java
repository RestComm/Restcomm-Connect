/*
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

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Management/Gateways")
@ThreadSafe
public final class GatewaysXmlEndpoint extends GatewaysEndpoint {
    public GatewaysXmlEndpoint() {
        super();
    }

    private Response deleteGateway(final String sid) {
        final Sid accountSid = Sid.generate(Sid.Type.INVALID);
        try {
            secure(accountSid, "RestComm:Modify:Gateways");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        dao.removeGateway(new Sid(sid));
        return ok().build();
    }

    @Path("/{sid}.json")
    @DELETE
    public Response deleteGatewayAsJson(@PathParam("sid") final String sid) {
        return deleteGateway(sid);
    }

    @Path("/{sid}")
    @DELETE
    public Response deleteGatewayAsXml(@PathParam("sid") final String sid) {
        return deleteGateway(sid);
    }

    @Path("/{sid}.json")
    @GET
    public Response getGatewayAsJson(@PathParam("sid") final String sid) {
        return getGateway(sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getGatewayAsXml(@PathParam("sid") final String sid) {
        return getGateway(sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getGateways() {
        return getGateways(APPLICATION_XML_TYPE);
    }

    @POST
    public Response putGateway(final MultivaluedMap<String, String> data) {
        return putGateway(data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}.json")
    @POST
    public Response updateGatewayAsJsonPost(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGateway(sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}.json")
    @PUT
    public Response updateGatewayAsJsonPut(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGateway(sid, data, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @POST
    public Response updateGatewayAsXmlPost(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGateway(sid, data, APPLICATION_XML_TYPE);
    }

    @Path("/{sid}")
    @PUT
    public Response updateGatewayAsXmlPut(@PathParam("sid") final String sid, final MultivaluedMap<String, String> data) {
        return updateGateway(sid, data, APPLICATION_XML_TYPE);
    }
}

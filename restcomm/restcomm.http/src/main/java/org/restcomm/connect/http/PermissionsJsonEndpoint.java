package org.restcomm.connect.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import javax.ws.rs.core.MultivaluedMap;

import org.restcomm.connect.commons.dao.Sid;

@Path("/Permissions.json")
public class PermissionsJsonEndpoint extends PermissionsEndpoint {

    @GET
    public Response getPermissionsList() {
        return getPermissionsList(APPLICATION_JSON_TYPE);
    }

    @Path("/{permissionSid}")
    @GET
    public Response getPermission(@PathParam("permissionSid") final String permissionSid) {
        return getPermission(new Sid(permissionSid), MediaType.APPLICATION_JSON_TYPE);
    }

    @POST
    public Response addPermission(final MultivaluedMap<String, String> data) {
        return addPermission(data, APPLICATION_JSON_TYPE);
    }

    @Path("/{permissionSid}")
    @DELETE
    public Response deletePermission(@PathParam("permissionSid") final String permissionSid) {
        return deletePermission(new Sid(permissionSid), MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("/{permissionSid}")
    @POST
    public Response updatePermission(@PathParam("permissionSid") final String permissionSid, final MultivaluedMap<String, String> data){
        return updatePermission(new Sid(permissionSid), data, MediaType.APPLICATION_JSON_TYPE);
    }
}

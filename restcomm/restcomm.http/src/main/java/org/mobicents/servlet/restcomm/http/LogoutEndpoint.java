package org.mobicents.servlet.restcomm.http;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@Path("/Logout")
public class LogoutEndpoint extends AbstractEndpoint {

    @GET
    public Response logout(@Context HttpServletRequest request) {
        SecurityUtils.getSubject().logout();
        return Response.ok().build();
    }

}

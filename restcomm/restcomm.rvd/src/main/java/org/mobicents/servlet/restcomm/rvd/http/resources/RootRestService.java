package org.mobicents.servlet.restcomm.rvd.http.resources;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.security.RvdUser;

@Path("/")
public class RootRestService extends RestService {
    static final Logger logger = Logger.getLogger(RootRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    @Context
    HttpServletRequest request;

    public RootRestService() {
        // TODO Auto-generated constructor stub
    }

    @GET
    @Path("abc")
    public Response abc() {
        return Response.ok().build();
    }

    @GET
    @Path("currentUser")
    public Response getCurrentUser() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if ( session.getToken() != null ) {
            return buildOkResponse(new RvdUser( session.getToken().getPreferredUsername() ));
        } else {
            return buildOkResponse(null);
        }
    }

}

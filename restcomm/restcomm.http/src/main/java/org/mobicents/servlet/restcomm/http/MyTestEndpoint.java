package org.mobicents.servlet.restcomm.http;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.keycloak.KeycloakSecurityContext;

@Path("/testing")
@ThreadSafe
public class MyTestEndpoint extends AbstractEndpoint {

    private Logger logger = Logger.getLogger(MyTestEndpoint.class);
    private String loggedUsername;

    @Context
    HttpServletRequest request;

    public MyTestEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    void init() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if (session.getToken() != null) {
            this.loggedUsername = session.getToken().getPreferredUsername();
            logger.info("logged username: " + this.loggedUsername);
        }
    }

    @GET
    public Response runTest() {
        logger.info("IN runTest 444");
        return Response.ok().build();
    }

}

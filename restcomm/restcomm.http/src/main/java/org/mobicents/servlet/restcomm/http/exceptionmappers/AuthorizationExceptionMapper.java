package org.mobicents.servlet.restcomm.http.exceptionmappers;

import org.apache.shiro.authz.AuthorizationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Generates 401 for AuthorizationException instead of a 500 (and an exception to the log). It helps to handle
 * authorization errors when they occur in *Endpoint.init() method where there is no explicit way of returning
 * a response (feature was  needed after shiro removal).
 *
 * @author Orestis Tsakiridis
 */
@Provider
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {
    @Override
    public Response toResponse(AuthorizationException e) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}

package org.restcomm.connect.rvd.http;

import org.restcomm.connect.rvd.exceptions.AuthorizationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * @author Orestis Tsakiridis
 */
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {
    @Override
    public Response toResponse(AuthorizationException e) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}

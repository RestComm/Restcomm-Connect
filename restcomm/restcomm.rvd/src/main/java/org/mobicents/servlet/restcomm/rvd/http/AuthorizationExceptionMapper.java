package org.mobicents.servlet.restcomm.rvd.http;

import org.mobicents.servlet.restcomm.rvd.exceptions.AuthorizationException;

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

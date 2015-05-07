package org.mobicents.servlet.restcomm.rvd.http;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.UnauthorizedException;

public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    static final Logger logger = Logger.getLogger(UnauthorizedExceptionMapper.class.getName());

    public UnauthorizedExceptionMapper() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Response toResponse(UnauthorizedException exception) {
        logger.warn("Unauthorized access prevented: " +  exception.getMessage());
        return Response.status(Status.UNAUTHORIZED).build();
    }

}

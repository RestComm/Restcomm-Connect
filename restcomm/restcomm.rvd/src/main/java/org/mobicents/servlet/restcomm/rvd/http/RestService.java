package org.mobicents.servlet.restcomm.rvd.http;

import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;

public class RestService {
    Response buildErrorResponse(Response.Status httpStatus, RvdResponse.Status rvdStatus, RvdException exception) {
        RvdResponse rvdResponse = new RvdResponse(rvdStatus).setException(exception);
        return Response.status(httpStatus).entity(rvdResponse.asJson()).build();
    }
}

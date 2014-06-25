package org.mobicents.servlet.restcomm.rvd.http;


import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;

//@Provider - needed only if package scanning is enabled
public class ProjectDoesNotExistMapper implements ExceptionMapper<ProjectDoesNotExist> {
    @Override
    public Response toResponse(ProjectDoesNotExist e) {
        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        return Response.status(Status.NOT_FOUND).entity(rvdResponse.asJson()).build();
    }

}


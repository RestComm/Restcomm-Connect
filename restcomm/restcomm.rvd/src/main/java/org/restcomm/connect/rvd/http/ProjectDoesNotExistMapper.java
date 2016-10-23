package org.restcomm.connect.rvd.http;


import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;

//@Provider - needed only if package scanning is enabled
public class ProjectDoesNotExistMapper implements ExceptionMapper<ProjectDoesNotExist> {
    static final Logger logger = Logger.getLogger(ProjectDoesNotExistMapper.class.getName());

    @Override
    public Response toResponse(ProjectDoesNotExist e) {
        logger.error(e);
        if(logger.isDebugEnabled()) {
            logger.debug(e,e);
        }

        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        return Response.status(Status.NOT_FOUND).entity(rvdResponse.asJson()).build();
    }

}


package org.restcomm.connect.rvd.http;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

//@Provider - needed only if package scanning is enabled
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {
    static final Logger logger = Logger.getLogger(StorageExceptionMapper.class.getName());

    @Override
    public Response toResponse(StorageException e) {
        logger.error(e);
        if(logger.isDebugEnabled()) {
            logger.debug(e,e);
        }

        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(rvdResponse.asJson()).build();
    }

}

package org.mobicents.servlet.restcomm.rvd.http;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

@Provider
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {
    static final Logger logger = Logger.getLogger(StorageExceptionMapper.class.getName());

    @Override
    public Response toResponse(StorageException e) {
        logger.error(e);
        logger.debug(e,e);

        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(rvdResponse.asJson()).build();
    }

}

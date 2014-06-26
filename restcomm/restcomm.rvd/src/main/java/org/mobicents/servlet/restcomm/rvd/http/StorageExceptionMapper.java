package org.mobicents.servlet.restcomm.rvd.http;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

//@Provider - needed only if package scanning is enabled
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {
    @Override
    public Response toResponse(StorageException e) {
        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(rvdResponse.asJson()).build();
    }

}

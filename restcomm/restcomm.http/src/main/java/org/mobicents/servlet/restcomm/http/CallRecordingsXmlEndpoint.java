package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/Accounts/{accountSid}/Calls/{callSid}/Recordings")
public class CallRecordingsXmlEndpoint extends RecordingsEndpoint {

    public CallRecordingsXmlEndpoint() {
        super();
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getRecordingsByCallXmln(@PathParam("accountSid") String accountSid, @PathParam("callSid") String callSid) {
        return getRecordingsByCall(accountSid, callSid, MediaType.APPLICATION_XML_TYPE);
    }

}

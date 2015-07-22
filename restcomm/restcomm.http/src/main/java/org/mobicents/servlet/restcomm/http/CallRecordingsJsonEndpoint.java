package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/Accounts/{accountSid}/Calls/{callSid}/Recordings.json")
public class CallRecordingsJsonEndpoint extends RecordingsEndpoint {

    public CallRecordingsJsonEndpoint() {
        super();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecordingsByCallJson(@PathParam("accountSid") String accountSid, @PathParam("callSid") String callSid) {
        return getRecordingsByCall(accountSid, callSid, MediaType.APPLICATION_JSON_TYPE);
    }

    /*
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/Recordings")
    public Response getRecordingsByCallXmln(@PathParam("accountSid") String accountSid, @PathParam("callSid") String callSid) {
        return getRecordingsByCall(accountSid, callSid, MediaType.APPLICATION_XML_TYPE);
    }
    */

}

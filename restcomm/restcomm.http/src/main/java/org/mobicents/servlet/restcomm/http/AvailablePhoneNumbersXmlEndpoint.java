package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

@Path("/Accounts/{accountSid}/AvailablePhoneNumbers/US/Local")
@ThreadSafe
public final class AvailablePhoneNumbersXmlEndpoint extends AvailablePhoneNumbersEndpoint {
    public AvailablePhoneNumbersXmlEndpoint() {
        super();
    }

    @GET
    public Response getAvailablePhoneNumber(@PathParam("accountSid") final String accountSid,
            @QueryParam("AreaCode") String areaCode) {
        if (areaCode != null && !areaCode.isEmpty()) {
            return getAvailablePhoneNumbersByAreaCode(accountSid, areaCode, APPLICATION_XML_TYPE);
        } else {
            return status(BAD_REQUEST).build();
        }
    }
}

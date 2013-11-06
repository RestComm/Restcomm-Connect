package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@Path("/Accounts/{accountSid}/Announcements.json")
@ThreadSafe
public final class AnnouncementsJsonEndpoint extends AnnouncementsEndpoint {
    public AnnouncementsJsonEndpoint() {
        super();
    }

    @POST
    public Response putAnnouncement(@PathParam("accountSid") final String accountSid, final MultivaluedMap<String, String> data) {
        return putAnnouncement(accountSid, data, MediaType.APPLICATION_JSON_TYPE);
    }
}

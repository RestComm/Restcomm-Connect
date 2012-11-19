package org.mobicents.servlet.sip.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@Path("/Accounts/{accountSid}/Announcements")
public final class AnnouncementsXmlEndpoint extends AnnouncementsEndpoint {

	public AnnouncementsXmlEndpoint() {
		super();
	}
	
	@GET
	public Response getAnnouncements(@PathParam("accountSid") final String accountSid) {
		return getAnnouncements(accountSid, MediaType.APPLICATION_XML_TYPE);
	}
	
	@POST
	public Response putAnnouncement(@PathParam("accountSid") final String accountSid,
		      final MultivaluedMap<String, String> data) {
		return putAnnouncement(accountSid, data, MediaType.APPLICATION_XML_TYPE);
	}
}

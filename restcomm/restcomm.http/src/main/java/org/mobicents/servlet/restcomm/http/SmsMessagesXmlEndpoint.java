/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/SMS/Messages")
@ThreadSafe
public final class SmsMessagesXmlEndpoint extends SmsMessagesEndpoint {
    public SmsMessagesXmlEndpoint() {
        super();
    }

    @Path("/{sid}.json")
    @GET
    public Response getSmsMessageAsJson(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getSmsMessage(accountSid, sid, APPLICATION_JSON_TYPE);
    }

    @Path("/{sid}")
    @GET
    public Response getSmsMessageAsXml(@PathParam("accountSid") final String accountSid, @PathParam("sid") final String sid) {
        return getSmsMessage(accountSid, sid, APPLICATION_XML_TYPE);
    }

    @GET
    public Response getSmsMessages(@PathParam("accountSid") final String accountSid) {
        return getSmsMessages(accountSid, APPLICATION_XML_TYPE);
    }

    @POST
    public Response putSmsMessage(@PathParam("accountSid") final String accountSid, final MultivaluedMap<String, String> data) {
        return putSmsMessage(accountSid, data, APPLICATION_XML_TYPE);
    }
}

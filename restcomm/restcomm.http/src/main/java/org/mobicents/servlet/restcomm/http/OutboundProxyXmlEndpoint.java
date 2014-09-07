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

import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@Path("/Accounts/{accountSid}/OutboundProxy")
@ThreadSafe
public class OutboundProxyXmlEndpoint extends OutboundProxyEndpoint {
    public OutboundProxyXmlEndpoint() {
        super();
    }

    @GET
    public Response getProxies(@PathParam("accountSid") final String accountSid) {
        return getProxies(accountSid, APPLICATION_XML_TYPE);
    }

    @GET
    @Path("/switchProxy")
    public Response switchProxy(@PathParam("accountSid") final String accountSid) {
        return switchProxy(accountSid, APPLICATION_XML_TYPE);
    }

    @GET
    @Path("/getActiveProxy")
    public Response getActiveProxy(@PathParam("accountSid") final String accountSid) {
        return getActiveProxy(accountSid, APPLICATION_XML_TYPE);
    }
}

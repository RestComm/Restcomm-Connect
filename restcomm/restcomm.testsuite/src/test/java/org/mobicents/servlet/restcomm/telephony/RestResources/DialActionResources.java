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
	
package org.mobicents.servlet.restcomm.telephony.RestResources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@Path("/DialAction")
public class DialActionResources {

    private static Logger logger = Logger.getLogger(DialActionResources.class);
    private static MultivaluedMap<String, String> postRequestData = null;
    private static UriInfo getRequestData = null;
    
    @GET
    public Response getRequest(@Context UriInfo info){
        logger.info("Received GET request for Dial Action");
        getRequestData = info; 
        return Response.ok().build();
    }
    
    @POST
    public Response postRequest(final MultivaluedMap<String, String> data){
        logger.info("Received POST request for Dial Action");
        postRequestData = data;
        return Response.ok().build();
    }

    public static MultivaluedMap<String, String> getPostRequestData() {
        return postRequestData;
    }

    public static UriInfo getGetRequestData() {
        return getRequestData;
    }
    
    public static void resetData(){
        postRequestData = null;
        getRequestData = null;
    }
}

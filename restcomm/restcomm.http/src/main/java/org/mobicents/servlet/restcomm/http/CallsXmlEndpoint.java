/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Calls")
@ThreadSafe public final class CallsXmlEndpoint extends CallsEndpoint {
  public CallsXmlEndpoint() {
    super();
  }
  
  @Path("/{sid}.json")
  @GET public Response getCallAsJson(@PathParam("accountSid") final String accountSid,
      @PathParam("sid") final String sid) {
    return getCall(accountSid, sid, APPLICATION_JSON_TYPE);
  }
  
  @Path("/{sid}")
  @GET public Response getCallAsXml(@PathParam("accountSid") final String accountSid,
      @PathParam("sid") final String sid) {
    return getCall(accountSid, sid, APPLICATION_XML_TYPE);
  }
  
  @GET public Response getCalls(@PathParam("accountSid") final String accountSid, @Context UriInfo info) {
    return getCalls(accountSid, info, APPLICATION_XML_TYPE);
  }
  
  @POST public Response putCall(@PathParam("accountSid") final String accountSid,
      final MultivaluedMap<String, String> data) {
    return putCall(accountSid, data, APPLICATION_XML_TYPE);
  }
    
//  //Issue 153: https://bitbucket.org/telestax/telscale-restcomm/issue/153
//  //Example:
//  //curl -G http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@127.0.0.1:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Calls/filters?status=completed&recipient=15126002188&startTime=2013-09-06
//  @Path("/filters")
//  @GET
//  public Response getCallsByUsingFilters(@PathParam("accountSid") String accountSid, @Context UriInfo info){
//	  
//	  return getCallsByFilters(accountSid, info, APPLICATION_XML_TYPE);
//  }
  
}

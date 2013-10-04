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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts")
@ThreadSafe public final class AccountsXmlEndpoint extends AccountsEndpoint {
  public AccountsXmlEndpoint() {
    super();
  }
  
  @Path("/{sid}")
  @DELETE public Response deleteAccountAsXml(@PathParam("sid") final String sid) {
    return deleteAccount(sid);
  }
  
  @Path("/{accountSid}.json")
  @GET public Response getAccountAsJson(@PathParam("accountSid") final String accountSid) {
    return getAccount(accountSid, APPLICATION_JSON_TYPE);
  }
  
  @Path("/{accountSid}")
  @GET public Response getAccountAsXml(@PathParam("accountSid") final String accountSid) {
    return getAccount(accountSid, APPLICATION_XML_TYPE);
  } 
  
  @GET public Response getAccounts() {
    return getAccounts(APPLICATION_XML_TYPE);
  }
  
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST public Response putAccount(final MultivaluedMap<String, String> data) {
    return putAccount(data, APPLICATION_XML_TYPE);
  }
  
  @Path("/{accountSid}.json")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST public Response updateAccountAsJsonPost(@PathParam("accountSid") final String accountSid,
      final MultivaluedMap<String, String> data) {
    return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
  }
  
  @Path("/{accountSid}.json")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @PUT public Response updateAccountAsJsonPut(@PathParam("accountSid") final String accountSid,
      final MultivaluedMap<String, String> data) {
    return updateAccount(accountSid, data, APPLICATION_JSON_TYPE);
  }
  
  @Path("/{accountSid}")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @POST public Response updateAccountAsXmlPost(@PathParam("accountSid") final String accountSid,
      final MultivaluedMap<String, String> data) {
    return updateAccount(accountSid, data, APPLICATION_XML_TYPE);
  }
  
  @Path("/{accountSid}")
  @Consumes(APPLICATION_FORM_URLENCODED)
  @PUT public Response updateAccountAsXmlPut(@PathParam("accountSid") final String accountSid,
      final MultivaluedMap<String, String> data) {
    return updateAccount(accountSid, data, APPLICATION_XML_TYPE);
  }
}

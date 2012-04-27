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
package org.mobicents.servlet.sip.restcomm.http;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import java.net.URI;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Client;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ClientsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.http.converter.ClientConverter;

import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Clients")
@ThreadSafe public final class ClientsEndpoint extends AbstractEndpoint {
  private final ClientsDao dao;
  private final XStream xstream;

  public ClientsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getClientsDao();
    xstream = new XStream();
    xstream.alias("Clients", List.class);
    xstream.alias("Client", Client.class);
    xstream.registerConverter(new ClientConverter());
  }
  
  private Client createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
    final Sid sid = Sid.generate(Sid.Type.CLIENT);
    final DateTime now = DateTime.now();
    final String apiVersion = getApiVersion(data);
    final String login = data.getFirst("Login");
    final String password = data.getFirst("Password");
    final String friendlyName = getFriendlyName(login, data);
    final int status = getStatus(data);
    final StringBuilder buffer = new StringBuilder();
    buffer.append("/").append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/Clients/").append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    return new Client(sid, now, now, accountSid, apiVersion, friendlyName, login, password, status, uri);
  }
  
  @Path("/{sid}")
  @DELETE Response deleteClient(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Delete:Clients"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    dao.removeClient(new Sid(sid));
    return ok().build();
  }
  
  @Path("/{sid}")
  @GET Response getClient(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Client client = dao.getClient(new Sid(sid));
    if(client == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(client), APPLICATION_XML).build();
    }
  }
  
  @GET Response getClients(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Client> clients = dao.getClients(new Sid(accountSid));
    return ok(xstream.toXML(clients), APPLICATION_XML).build();
  }
  
  private String getFriendlyName(final String login, final MultivaluedMap<String, String> data) {
    String friendlyName = login;
    if(data.containsKey("FriendlyName")) {
      friendlyName = data.getFirst("FriendlyName");
    }
    return friendlyName;
  }
  
  private int getStatus(final MultivaluedMap<String, String> data) {
    int status = Client.ENABLED;
    if(data.containsKey("Status")) {
      try { status = Integer.parseInt(data.getFirst("Status")); }
      catch(final NumberFormatException ignored) { }
      if(status != Client.DISABLED && status != Client.ENABLED) {
        status = Client.ENABLED;
      }
    }
    return status;
  }
  
  @POST public Response putClient(@PathParam("accountSid") String accountSid, final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Create:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); } catch(final NullPointerException exception) { 
    	return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    return status(CREATED).type(APPLICATION_XML).entity(xstream.toXML(createFrom(new Sid(accountSid), data))).build();
  }
  
  @Path("/{sid}")
  @PUT public Response updateClient(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid,
      final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Modify:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Client client = dao.getClient(new Sid(sid));
    dao.updateClient(update(client, data));
    return ok().build();
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("Login")) {
      throw new NullPointerException("Login can not be null.");
    } else if(!data.containsKey("Password")) {
      throw new NullPointerException("Password can not be null.");
    }
  }
  
  public Client update(final Client client, final MultivaluedMap<String, String> data) {
    Client result = client;
    if(data.containsKey("FriendlyName")) {
      result = result.setFriendlyName(data.getFirst("FriendlyName"));
    }
    if(data.containsKey("Password")) {
      result = result.setPassword(data.getFirst("Password"));
    }
    if(data.containsKey("Status")) {
      result = result.setStatus(getStatus(data));
    }
    return result;
  }
}

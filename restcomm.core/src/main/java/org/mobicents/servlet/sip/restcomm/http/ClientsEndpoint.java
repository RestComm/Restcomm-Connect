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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.thoughtworks.xstream.XStream;

import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ClientsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.Client;
import org.mobicents.servlet.sip.restcomm.entities.ClientList;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.ClientConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.ClientListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class ClientsEndpoint extends AbstractEndpoint {
  protected final ClientsDao dao;
  protected final Gson gson;
  protected final XStream xstream;

  public ClientsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getClientsDao();
    final ClientConverter converter = new ClientConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Client.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new ClientListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  private Client createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
    final Sid sid = Sid.generate(Sid.Type.CLIENT);
    final DateTime now = DateTime.now();
    final String apiVersion = getApiVersion(data);
    final String login = data.getFirst("Login");
    final String password = data.getFirst("Password");
    final String friendlyName = getFriendlyName(login, data);
    final int status = getStatus(data);
    String rootUri = configuration.getString("root-uri");
    rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/Clients/").append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    return new Client(sid, now, now, accountSid, apiVersion, friendlyName, login, password, status, uri);
  }
  
  protected Response getClient(final String accountSid, final String sid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Client client = dao.getClient(new Sid(sid));
    if(client == null) {
      return status(NOT_FOUND).build();
    } else {
	  if(APPLICATION_XML_TYPE == responseType) {
		final RestCommResponse response = new RestCommResponse(client);
		return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(client), APPLICATION_JSON).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getClients(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Client> clients = dao.getClients(new Sid(accountSid));
    if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new ClientList(clients));
	  return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(clients), APPLICATION_JSON).build();
    } else {
      return null;
    }
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
    }
    return status;
  }
  
  public Response putClient(final String accountSid, final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Create:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); } catch(final NullPointerException exception) { 
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final Client client = createFrom(new Sid(accountSid), data);
    dao.addClient(client);
    if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(client);
  	  return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(client), APPLICATION_JSON).build();
    } else {
      return null;
    }
  }
  
  public Response updateClient(final String accountSid, final String sid,
      final MultivaluedMap<String, String> data, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Modify:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Client client = dao.getClient(new Sid(sid));
    dao.updateClient(update(client, data));
    if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(client);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(client), APPLICATION_JSON).build();
    } else {
      return null;
    }
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

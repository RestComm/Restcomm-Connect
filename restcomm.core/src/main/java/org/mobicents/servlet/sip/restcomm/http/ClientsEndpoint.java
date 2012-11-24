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
    final Client.Builder builder = Client.builder();
    final Sid sid = Sid.generate(Sid.Type.CLIENT);
    builder.setSid(sid);
    builder.setApiVersion(getApiVersion(data));
    builder.setFriendlyName(getFriendlyName(data.getFirst("Login"), data));
    builder.setAccountSid(accountSid);
    builder.setLogin(data.getFirst("Login"));
    builder.setPassword(data.getFirst("Password"));
    builder.setStatus(getStatus(data));
    builder.setVoiceUrl(getUrl("VoiceUrl", data));
    builder.setVoiceMethod(getMethod("VoiceMethod", data));
    builder.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
    builder.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
    builder.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
    String rootUri = configuration.getString("root-uri");
    rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
        .append("/Clients/").append(sid.toString());
    builder.setUri(URI.create(buffer.toString()));
    return builder.build();
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
  
  protected Response updateClient(final String accountSid, final String sid,
      final MultivaluedMap<String, String> data, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Modify:Clients"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Client client = dao.getClient(new Sid(sid));
    if(client == null) {
      return status(NOT_FOUND).build();
    } else {
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
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("Login")) {
      throw new NullPointerException("Login can not be null.");
    } else if(!data.containsKey("Password")) {
      throw new NullPointerException("Password can not be null.");
    }
  }
  
  private Client update(final Client client, final MultivaluedMap<String, String> data) {
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
    if(data.containsKey("VoiceUrl")) {
      result = result.setVoiceUrl(getUrl("VoiceUrl", data));
    }
    if(data.containsKey("VoiceMethod")) {
      result = result.setVoiceMethod(getMethod("VoiceMethod", data));
    }
    if(data.containsKey("VoiceFallbackUrl")) {
      result = result.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
    }
    if(data.containsKey("VoiceFallbackMethod")) {
      result = result.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
    }
    if(data.containsKey("VoiceApplicationSid")) {
      result = result.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
    }
    return result;
  }
}

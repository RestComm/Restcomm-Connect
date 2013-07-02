package org.mobicents.servlet.restcomm.http;

import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.MediaType.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.thoughtworks.xstream.XStream;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.restcomm.entities.Gateway;
import org.mobicents.servlet.restcomm.entities.GatewayList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.GatewayConverter;
import org.mobicents.servlet.restcomm.http.converter.GatewayListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

@ThreadSafe public class GatewaysEndpoint extends AbstractEndpoint {
  @javax.ws.rs.core.Context 
  private ServletContext context;
  protected final GatewaysDao dao;
  protected final Gson gson;
  protected final XStream xstream;

  public GatewaysEndpoint() {
    super();
    final DaoManager storage = (DaoManager)context.getAttribute(DaoManager.class.getName());
    dao = storage.getGatewaysDao();
    final GatewayConverter converter = new GatewayConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Gateway.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new GatewayListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  private Gateway createFrom(final MultivaluedMap<String, String> data) {
    final Gateway.Builder builder = Gateway.builder();
    final Sid sid = Sid.generate(Sid.Type.GATEWAY);
    builder.setSid(sid);
    String friendlyName = data.getFirst("FriendlyName");
    if(friendlyName == null || friendlyName.isEmpty()) {
      friendlyName = data.getFirst("UserName");
    }
    builder.setFriendlyName(friendlyName);
    builder.setPassword(data.getFirst("Password"));
    builder.setProxy(data.getFirst("Proxy"));
    final boolean register = Boolean.parseBoolean(data.getFirst("Register"));
    builder.setRegister(register);
    builder.setUserName(data.getFirst("UserName"));
    final int ttl = Integer.parseInt(data.getFirst("TTL"));
    builder.setTimeToLive(ttl);
    String rootUri = configuration.getString("root-uri");
    rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(getApiVersion(data)).append("/Management/")
        .append("Gateways/").append(sid.toString());
    builder.setUri(URI.create(buffer.toString()));
    return builder.build();
  }
  
  protected Response getGateway(final String sid, final MediaType responseType) {
    final Sid accountSid = Sid.generate(Sid.Type.INVALID);
    try { secure(accountSid, "RestComm:Read:Gateways"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Gateway gateway = dao.getGateway(new Sid(sid));
    if(gateway == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(gateway);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(gateway), APPLICATION_JSON).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getGateways(final MediaType responseType) {
    final Sid accountSid = Sid.generate(Sid.Type.INVALID);
    try { secure(accountSid, "RestComm:Read:Gateways"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Gateway> gateways = dao.getGateways();
    if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new GatewayList(gateways));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(gateways), APPLICATION_JSON).build();
    } else {
      return null;
    }
  }
  
  protected Response putGateway(final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    final Sid accountSid = Sid.generate(Sid.Type.INVALID);
	try { secure(accountSid, "RestComm:Create:Gateways"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
	try { validate(data); } catch(final RuntimeException exception) { 
	  return status(BAD_REQUEST).entity(exception.getMessage()).build();
	}
	final Gateway gateway = createFrom(data);
	dao.addGateway(gateway);
	if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(gateway);
  	  return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(gateway), APPLICATION_JSON).build();
    } else {
      return null;
    }
  }
  
  protected Response updateGateway(final String sid, final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    final Sid accountSid = Sid.generate(Sid.Type.INVALID);
	try { secure(accountSid, "RestComm:Modify:Gateways"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
	final Gateway gateway = dao.getGateway(new Sid(sid));
	if(gateway == null) {
	  return status(NOT_FOUND).build();
	} else {
	  dao.updateGateway(update(gateway, data));
	  if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(gateway);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(gateway), APPLICATION_JSON).build();
      } else {
        return null;
      }
	}
  }
  
  private void validate(final MultivaluedMap<String, String> data) {
    if(!data.containsKey("UserName")) {
      throw new NullPointerException("UserName can not be null.");
    } else if(!data.containsKey("Password")) {
      throw new NullPointerException("Password can not be null.");
    } else if(!data.containsKey("Proxy")) {
      throw new NullPointerException("The Proxy can not be null");
    } else if(!data.containsKey("Register")) {
      throw new NullPointerException("Register must be true or false");
    }
  }
  
  private Gateway update(final Gateway gateway, final MultivaluedMap<String, String> data) {
    Gateway result = gateway;
    if(data.containsKey("FriendlyName")) {
      result = result.setFriendlyName(data.getFirst("FriendlyName"));
    }
    if(data.containsKey("UserName")) {
      result = result.setUserName(data.getFirst("UserName"));
    }
    if(data.containsKey("Password")) {
      result = result.setPassword(data.getFirst("Password"));
    }
    if(data.containsKey("Proxy")) {
      result = result.setProxy(data.getFirst("Proxy"));
    }
    if(data.containsKey("Register")) {
      result = result.setRegister(Boolean.parseBoolean("Register"));
    }
    if(data.containsKey("TTL")) {
      result = result.setTimeToLive(Integer.parseInt(data.getFirst("TTL")));
    }
    return result;
  }
}

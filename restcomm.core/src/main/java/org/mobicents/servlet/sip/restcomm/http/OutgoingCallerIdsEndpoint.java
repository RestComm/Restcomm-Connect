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

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

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
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdsDao;
import org.mobicents.servlet.sip.restcomm.entities.OutgoingCallerId;
import org.mobicents.servlet.sip.restcomm.http.converter.OutgoingCallerIdConverter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/OutgoingCallerIds")
@ThreadSafe public final class OutgoingCallerIdsEndpoint extends AbstractEndpoint {
  private final OutgoingCallerIdsDao dao;
  private final XStream xstream;
  
  public OutgoingCallerIdsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getOutgoingCallerIdsDao();
    xstream = new XStream();
    xstream.alias("OutgoingCallerIds", List.class);
    xstream.alias("OutgoingCallerId", OutgoingCallerId.class);
    xstream.registerConverter(new OutgoingCallerIdConverter());
  }
  
  private OutgoingCallerId createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
    final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
    final DateTime now = DateTime.now();
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    PhoneNumber phoneNumber = null;
    try { phoneNumber = phoneNumberUtil.parse(data.getFirst("PhoneNumber"), "US"); }
    catch(final NumberParseException ignored) { }
    String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.NATIONAL);
    if(data.containsKey("FriendlyName")) {
      friendlyName = data.getFirst("FriendlyName");
    }
    final StringBuilder buffer = new StringBuilder();
    buffer.append("/").append(getApiVersion(null)).append("/Accounts/").append(accountSid.toString())
        .append("/OutgoingCallerIds/").append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    return new OutgoingCallerId(sid, now, now, friendlyName, accountSid,
        phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.E164), uri);
  }
  
  @Path("/{sid}")
  @DELETE public Response deleteOutgoingCallerId(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
	try { secure(new Sid(accountSid), "RestComm:Delete:OutgoingCallerIds"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    dao.removeOutgoingCallerId(new Sid(sid));
    return ok().build();
  }
  
  @Path("/{sid}")
  @GET public Response getCallerId(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:OutgoingCallerIds"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final OutgoingCallerId outgoingCallerId = dao.getOutgoingCallerId(new Sid(sid));
    if(outgoingCallerId == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(outgoingCallerId), APPLICATION_XML).build();
    }
  }
  
  @GET public Response getCallerIds(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:OutgoingCallerIds"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<OutgoingCallerId> outgoingCallerIds = dao.getOutgoingCallerIds(new Sid(accountSid));
    return ok(xstream.toXML(outgoingCallerIds), APPLICATION_XML).build();
  }

  @POST public Response putOutgoingCallerId(@PathParam("accountSid") String accountSid, final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Create:OutgoingCallerIds"); } 
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); } catch(final NullPointerException exception) { 
    	return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final OutgoingCallerId outgoingCallerId = createFrom(new Sid(accountSid), data);
    dao.addOutgoingCallerId(outgoingCallerId);
    return status(CREATED).type(APPLICATION_XML).entity(xstream.toXML(outgoingCallerId)).build();
  }
  
  @Path("/{sid}")
  @PUT public Response updateOutgoingCallerId(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid,
      final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Modify:OutgoingCallerIds"); } 
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    if(data.containsKey("FriendlyName")) {
      OutgoingCallerId outgoingCallerId = dao.getOutgoingCallerId(new Sid(sid));
      if(outgoingCallerId != null) {
        outgoingCallerId = outgoingCallerId.setFriendlyName(data.getFirst("FriendlyName"));
        dao.updateOutgoingCallerId(outgoingCallerId);
      }
    }
    return ok().build();
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("AccountSid")) {
      throw new NullPointerException("Account Sid can not be null.");
    } else if(!data.containsKey("PhoneNumber")){
      throw new NullPointerException("Phone number can not be null.");
    }
    try { PhoneNumberUtil.getInstance().parse(data.getFirst("PhoneNumber"), "US"); }
    catch(final NumberParseException exception) { throw new IllegalArgumentException("Invalid phone number."); }
  }
}

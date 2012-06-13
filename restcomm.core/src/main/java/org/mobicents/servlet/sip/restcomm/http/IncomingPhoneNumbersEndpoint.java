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
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.sip.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.http.converter.IncomingPhoneNumberConverter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/IncomingPhoneNumbers")
@ThreadSafe public final class IncomingPhoneNumbersEndpoint extends AbstractEndpoint {
  private final IncomingPhoneNumbersDao dao;
  private final XStream xstream;
  
  public IncomingPhoneNumbersEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getIncomingPhoneNumbersDao();
    xstream = new XStream();
    xstream.alias("IncomingPhoneNumbers", List.class);
    xstream.alias("IncomingPhoneNumber", IncomingPhoneNumber.class);
    xstream.registerConverter(new IncomingPhoneNumberConverter());
  }
  
  private IncomingPhoneNumber createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
    final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
    final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
    builder.setSid(sid);
    builder.setAccountSid(accountSid);
    final PhoneNumber phoneNumber = getPhoneNumber(data);
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    builder.setPhoneNumber(phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.E164));
    builder.setFriendlyName(getFriendlyName(phoneNumber, data));
    final String apiVersion = getApiVersion(data);
    builder.setApiVersion(apiVersion);
    builder.setVoiceUrl(getUrl("VoiceUrl", data));
    builder.setVoiceMethod(getMethod("VoiceMethod", data));
    builder.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
    builder.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
    builder.setStatusCallback(getUrl("StatusCallback", data));
    builder.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
    builder.setHasVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
    builder.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
    builder.setSmsUrl(getUrl("SmsUrl", data));
    builder.setSmsMethod(getMethod("SmsMethod", data));
    builder.setSmsFallbackUrl(getUrl("SmsFallbackUrl", data));
    builder.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
    builder.setSmsApplicationSid(getSid("SmsApplicationSid", data));
    final StringBuilder buffer = new StringBuilder();
    buffer.append("/").append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/IncomingPhoneNumbers/").append(sid.toString());
    builder.setUri(URI.create(buffer.toString()));
    return builder.build();
  }
  
  @Path("/{sid}")
  @DELETE public Response deleteIncomingPhoneNumber(@PathParam("accountSid") String accountSid,
      @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Delete:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    dao.removeIncomingPhoneNumber(new Sid(sid));
    return ok().build();
  }
  
  private String getFriendlyName(final PhoneNumber phoneNumber, final MultivaluedMap<String, String> data) {
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.NATIONAL);
    if(data.containsKey("FriendlyName")) {
      friendlyName = data.getFirst("FriendlyName");
    }
    return friendlyName;
  }
  
  @Path("/{sid}")
  @GET public Response getIncomingPhoneNumber(@PathParam("accountSid") String accountSid,
      @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
    if(incomingPhoneNumber == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(incomingPhoneNumber), APPLICATION_XML).build();
    }
  }
  
  @GET public Response getIncomingPhoneNumbers(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<IncomingPhoneNumber> incomingPhoneNumbers = dao.getIncomingPhoneNumbers(new Sid(accountSid));
    return ok(xstream.toXML(incomingPhoneNumbers), APPLICATION_XML).build();
  }
  
  @POST public Response putIncomingPhoneNumber(@PathParam("accountSid") String accountSid,
      final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Create:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); } catch(final NullPointerException exception) { 
    	return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final IncomingPhoneNumber incomingPhoneNumber = createFrom(new Sid(accountSid), data);
    dao.addIncomingPhoneNumber(incomingPhoneNumber);
    return status(CREATED).type(APPLICATION_XML).entity(xstream.toXML(incomingPhoneNumber)).build();
  }
  
  @Path("/{sid}")
  @PUT public Response updateIncomingPhoneNumber(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid,
      final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Modify:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
    dao.updateIncomingPhoneNumber(update(incomingPhoneNumber, data));
    return ok().build();
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("PhoneNumber")){
      throw new NullPointerException("Phone number can not be null.");
    }
    try { PhoneNumberUtil.getInstance().parse(data.getFirst("PhoneNumber"), "US"); }
    catch(final NumberParseException exception) { throw new IllegalArgumentException("Invalid phone number."); }
  }
  
  private IncomingPhoneNumber update(final IncomingPhoneNumber incomingPhoneNumber, final MultivaluedMap<String, String> data) {
    IncomingPhoneNumber result = incomingPhoneNumber;
    if(data.containsKey("ApiVersion")) {
      result = result.setApiVersion(getApiVersion(data));
    }
    if(data.containsKey("FriendlyName")) {
      result = result.setFriendlyName(data.getFirst("FriendlyName"));
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
    if(data.containsKey("StatusCallback")) {
      result = result.setStatusCallback(getUrl("StatusCallback", data));
    }
    if(data.containsKey("StatusCallbackMethod")) {
      result = result.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
    }
    if(data.containsKey("VoiceCallerIdLookup")) {
      result = result.setVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
    }
    if(data.containsKey("VoiceApplicationSid")) {
      result = result.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
    }
    if(data.containsKey("SmsUrl")) {
      result = result.setSmsUrl(getUrl("SmsUrl", data));
    }
    if(data.containsKey("SmsMethod")) {
      result = result.setSmsMethod(getMethod("SmsMethod", data));
    }
    if(data.containsKey("SmsFallbackUrl")) {
      result = result.setSmsFallbackUrl(getUrl("SmsFallbackUrl", data));
    }
    if(data.containsKey("SmsFallbackMethod")) {
      result = result.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
    }
    if(data.containsKey("SmsApplicationSid")) {
      result = result.setSmsApplicationSid(getSid("SmsApplicationSid", data));
    }
    return result;
  }
}

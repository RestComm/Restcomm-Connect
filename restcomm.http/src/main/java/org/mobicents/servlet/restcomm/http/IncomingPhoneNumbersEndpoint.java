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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import com.thoughtworks.xstream.XStream;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberConverter;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class IncomingPhoneNumbersEndpoint extends AbstractEndpoint {
  @Context protected ServletContext context;
  protected Configuration configuration;
  protected IncomingPhoneNumbersDao dao;
  protected Gson gson;
  protected XStream xstream;
  
  public IncomingPhoneNumbersEndpoint() {
    super();
  }
  
  @PostConstruct
  public void init() {
    final DaoManager storage = (DaoManager)context.getAttribute(DaoManager.class.getName());
    configuration = (Configuration)context.getAttribute(Configuration.class.getName());
    configuration = configuration.subset("runtime-settings");
    super.init(configuration);
    dao = storage.getIncomingPhoneNumbersDao();
    final IncomingPhoneNumberConverter converter = new IncomingPhoneNumberConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(IncomingPhoneNumber.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new IncomingPhoneNumberListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
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
    String rootUri = configuration.getString("root-uri");
    rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/IncomingPhoneNumbers/").append(sid.toString());
    builder.setUri(URI.create(buffer.toString()));
    return builder.build();
  }
  
  private String getFriendlyName(final PhoneNumber phoneNumber, final MultivaluedMap<String, String> data) {
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    String friendlyName = phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.NATIONAL);
    if(data.containsKey("FriendlyName")) {
      friendlyName = data.getFirst("FriendlyName");
    }
    return friendlyName;
  }
  
  protected Response getIncomingPhoneNumber(final String accountSid, final String sid,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
    if(incomingPhoneNumber == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
      } else if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getIncomingPhoneNumbers(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<IncomingPhoneNumber> incomingPhoneNumbers = dao.getIncomingPhoneNumbers(new Sid(accountSid));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(incomingPhoneNumbers), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new IncomingPhoneNumberList(incomingPhoneNumbers));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
  
  protected Response putIncomingPhoneNumber(final String accountSid,
      final MultivaluedMap<String, String> data, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Create:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); } catch(final NullPointerException exception) { 
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final IncomingPhoneNumber incomingPhoneNumber = createFrom(new Sid(accountSid), data);
    dao.addIncomingPhoneNumber(incomingPhoneNumber);
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
  
  public Response updateIncomingPhoneNumber(final String accountSid,final String sid,
      final MultivaluedMap<String, String> data, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Modify:IncomingPhoneNumbers"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
    dao.updateIncomingPhoneNumber(update(incomingPhoneNumber, data));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
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

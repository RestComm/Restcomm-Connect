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
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import com.thoughtworks.xstream.XStream;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.CallDetailRecordConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.CallDetailRecordListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallManagerException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class CallsEndpoint extends AbstractEndpoint {
  private final CallManager callManager;
  private final CallDetailRecordsDao dao;
  private final InterpreterExecutor executor;
  protected final Gson gson;
  protected final XStream xstream;

  public CallsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    dao = services.get(DaoManager.class).getCallDetailRecordsDao();
    executor = services.get(InterpreterExecutor.class);
    CallDetailRecordConverter converter = new CallDetailRecordConverter();
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(CallDetailRecord.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new CallDetailRecordListConverter());
    xstream.registerConverter(new RestCommResponseConverter());
  }
  
  private void normalize(final MultivaluedMap<String, String> data) throws IllegalArgumentException {
	  final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
	  final String from = data.getFirst("From");
	  data.remove("From");
	  try {
	    data.putSingle("From", phoneNumberUtil.format(phoneNumberUtil.parse(from, "US"), PhoneNumberFormat.E164));
	  } catch(final NumberParseException exception) { throw new IllegalArgumentException(exception); }
	  final String to = data.getFirst("To");
	  data.remove("To");
	  try {
	    data.putSingle("To", phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164));
	  } catch(final NumberParseException exception) { throw new IllegalArgumentException(exception); }
	  URI.create(data.getFirst("Url"));
  }
  
  protected Response putCall(final String accountSid, final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Create:Calls"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); normalize(data); }
    catch(final RuntimeException exception) {
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final String from = data.getFirst("From");
    final String to = data.getFirst("To");
    Call call = null;
    try { call = callManager.createExternalCall(from, to); }
    catch(final CallManagerException exception) {
      return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
    final URI url = URI.create(data.getFirst("Url"));
    executor.submit(new Sid(accountSid), getApiVersion(data), url, getMethod("VoiceMethod", data), null, null, call);
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(call), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(call);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
    if(!data.containsKey("From")) {
      throw new NullPointerException("From can not be null.");
    } else if(!data.containsKey("To")) {
      throw new NullPointerException("To can not be null.");
    } else if(!data.containsKey("Url")) {
      throw new NullPointerException("Url can not be null.");
    }
  }
}

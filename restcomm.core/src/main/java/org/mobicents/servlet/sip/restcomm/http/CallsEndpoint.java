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

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

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
import org.mobicents.servlet.sip.restcomm.entities.CallDetailRecordList;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.CallDetailRecordConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.CallDetailRecordListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallManagerException;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class CallsEndpoint extends AbstractEndpoint {
  private final CallManager callManager;
  private final DaoManager daos;
  private final InterpreterExecutor executor;
  protected final Gson gson;
  protected final XStream xstream;

  public CallsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    daos = services.get(DaoManager.class);
    executor = services.get(InterpreterExecutor.class);
    CallDetailRecordConverter converter = new CallDetailRecordConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(CallDetailRecord.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new CallDetailRecordListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  protected Response getCall(final String accountSid, final String sid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Calls"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
    final CallDetailRecord cdr = dao.getCallDetailRecord(new Sid(sid));
    if(cdr == null) {
      return status(NOT_FOUND).build();
    } else {
	  if(APPLICATION_XML_TYPE == responseType) {
		final RestCommResponse response = new RestCommResponse(cdr);
		return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(cdr), APPLICATION_JSON).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getCalls(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Calls"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
    final List<CallDetailRecord> cdrs = dao.getCallDetailRecords(new Sid(accountSid));
    if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new CallDetailRecordList(cdrs));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(cdrs), APPLICATION_JSON).build();
    } else {
      return null;
    }
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
  
  protected Response putCall(final String sid, final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    final Sid accountSid = new Sid(sid);
    try { secure(accountSid, "RestComm:Create:Calls"); }
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
    final Integer timeout = data.getFirst("Timeout") != null ? Integer.parseInt(data.getFirst("Timeout")) : null;
    executor.submit(new Sid(sid), getApiVersion(data), url, getMethod("VoiceMethod", data), null, null, null, null,
        timeout, call);
    final CallDetailRecord cdr = toCallDetailRecord(accountSid, call);
    daos.getCallDetailRecordsDao().addCallDetailRecord(cdr);
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(cdr), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(cdr);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
  
  private Integer getDuration(final Call call) {
	if(call.getDateStarted() != null && call.getDatedEnded() != null) {
	  final long start = call.getDateStarted().getMillis();
	  final long end = call.getDatedEnded().getMillis();
      return (int)((end - start) / 1000);
	} else {
	  return null;
	}
  }
  
  private CallDetailRecord toCallDetailRecord(final Sid accountSid, final Call call) {
	final CallDetailRecord.Builder builder = CallDetailRecord.builder();
	builder.setSid(call.getSid());
	builder.setDateCreated(call.getDateCreated());
	builder.setAccountSid(accountSid);
	builder.setTo(call.getRecipient());
	builder.setFrom(call.getOriginator());
	builder.setStatus(call.getStatus().toString());
	builder.setStartTime(call.getDateStarted());
	builder.setEndTime(call.getDatedEnded());
	builder.setDuration(getDuration(call));
	builder.setPrice(new BigDecimal(0.00));
	builder.setDirection(call.getDirection().toString());
	builder.setApiVersion(getApiVersion(null));
	builder.setForwardedFrom(call.getForwardedFrom());
	builder.setCallerName(call.getOriginatorName());
    final StringBuilder buffer = new StringBuilder();
    String rootUri = configuration.getString("root-uri");
    rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
    buffer.append(rootUri).append(getApiVersion(null)).append("/Accounts/");
    buffer.append(accountSid.toString()).append("/Calls/").append(call.getSid());
    buffer.toString();
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    return builder.build();
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

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

import static akka.pattern.Patterns.ask;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.CallDetailRecordConverter;
import org.mobicents.servlet.restcomm.http.converter.CallDetailRecordListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallManagerResponse;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.restcomm.telephony.ExecuteCallScript;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.util.Timeout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class CallsEndpoint extends AbstractEndpoint {
  @Context protected ServletContext context;
  protected Configuration configuration;
  private ActorRef callManager;
  private DaoManager daos;
  private Gson gson;
  private GsonBuilder builder;
  private XStream xstream;
  private CallDetailRecordListConverter listConverter;

  public CallsEndpoint() {
    super();
  }
  
  @PostConstruct
  public void init() {
    configuration = (Configuration)context.getAttribute(Configuration.class.getName());
    configuration = configuration.subset("runtime-settings");
    callManager = (ActorRef)context.getAttribute("org.mobicents.servlet.restcomm.telephony.CallManager");
    daos = (DaoManager)context.getAttribute(DaoManager.class.getName());
    super.init(configuration);
    CallDetailRecordConverter converter = new CallDetailRecordConverter(configuration);
    listConverter = new CallDetailRecordListConverter(configuration);
    builder = new GsonBuilder();
    builder.registerTypeAdapter(CallDetailRecord.class, converter);
	builder.registerTypeAdapter(CallDetailRecordList.class, listConverter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new RestCommResponseConverter(configuration));
	xstream.registerConverter(listConverter);
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
  
//Issue 153: https://bitbucket.org/telestax/telscale-restcomm/issue/153
//Issue 110: https://bitbucket.org/telestax/telscale-restcomm/issue/110  
  protected Response getCalls(final String accountSid, UriInfo info, MediaType responseType){

	  try { 
		  secure(new Sid(accountSid), "RestComm:Read:Calls"); 
	  } catch(final AuthorizationException exception) { 
		  return status(UNAUTHORIZED).build(); 
	  }
	  
	  String pageSize = info.getQueryParameters().getFirst("PageSize");
	  String page = info.getQueryParameters().getFirst("Page");
//	  String afterSid = info.getQueryParameters().getFirst("AfterSid");
	  String recipient = info.getQueryParameters().getFirst("To");
	  String sender = info.getQueryParameters().getFirst("From");
	  String status = info.getQueryParameters().getFirst("Status");
	  String startTime = info.getQueryParameters().getFirst("StartTime");
	  String parentCallSid = info.getQueryParameters().getFirst("ParentCallSid");
	  
	  if (pageSize == null) {
		  pageSize = "50";
	  }
		  
	  if (page == null) {
		  page = "0";
	  } 
	  
	  int limit = Integer.parseInt(pageSize);
	  int offset = (page == "0") ? 0 : (((Integer.parseInt(page)-1)*Integer.parseInt(pageSize))+Integer.parseInt(pageSize));
	  
	  CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
	  
	  CallDetailRecordFilter filter = new CallDetailRecordFilter(accountSid, recipient, sender, 
			  status, startTime, parentCallSid, limit, offset);

	  final List<CallDetailRecord> cdrs = dao.getCallDetailRecords(filter);
	  final int total = dao.getTotalCallDetailRecords(filter);

	  listConverter.setCount(total);
	  listConverter.setPage(Integer.parseInt(page));
	  listConverter.setPageSize(Integer.parseInt(pageSize));
	  listConverter.setPathUri(info.getRequestUri().getPath());
	  
	  if(APPLICATION_XML_TYPE == responseType) {
		  final RestCommResponse response = new RestCommResponse(new CallDetailRecordList(cdrs));
		  return ok(xstream.toXML(response), APPLICATION_XML).build();
	  } else if(APPLICATION_JSON_TYPE == responseType) {
		  return ok(gson.toJson(new CallDetailRecordList(cdrs)), APPLICATION_JSON).build();
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
	// Only try to normalize phone numbers.
	if(to.startsWith("client")) {
	  if(to.split(":").length != 2) {
	    throw new IllegalArgumentException(to + " is an invalid client identifier.");
	  }
	} else if(!to.contains("@")) {
	  data.remove("To");
	  try {
	    data.putSingle("To", phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164));
	  } catch(final NumberParseException exception) { throw new IllegalArgumentException(exception); }
	}
	URI.create(data.getFirst("Url"));
  }
  
  @SuppressWarnings("unchecked")
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
    final Integer timeout = getTimeout(data);
    final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
    CreateCall create = null;
    try {
      if(to.contains("@")) {
        create = new CreateCall(from, to, true, timeout != null ? timeout : 30, CreateCall.Type.SIP);
      } else if(to.startsWith("client")) {
        create = new CreateCall(from, to, true, timeout != null ? timeout : 30, CreateCall.Type.CLIENT);
      } else {
        create = new CreateCall(from, to, true, timeout != null ? timeout : 30, CreateCall.Type.PSTN);
      }
      Future<Object> future = (Future<Object>)ask(callManager, create, expires);
      Object object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
      Class<?> klass = object.getClass();
      if(CallManagerResponse.class.equals(klass)) {
        final CallManagerResponse<ActorRef> managerResponse = (CallManagerResponse<ActorRef>)object;
        if(managerResponse.succeeded()) {
          final ActorRef call = managerResponse.get();
          future = (Future<Object>)ask(call, new GetCallInfo(), expires);
          object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
          klass = object.getClass();
          if(CallResponse.class.equals(klass)) {
            final CallResponse<CallInfo> callResponse = (CallResponse<CallInfo>)object;
            if(callResponse.succeeded()) {
              final CallInfo callInfo = callResponse.get();
              // Execute the call script.
              final String version = getApiVersion(data);
              final URI url = getUrl("Url", data);
              final String method = getMethod("Method", data);
              final URI fallbackUrl = getUrl("FallbackUrl", data);
              final String fallbackMethod = getMethod("FallbackMethod", data);
              final URI callback = getUrl("StatusCallback", data);
              final String callbackMethod = getMethod("StatusCallbackMethod", data);
              final ExecuteCallScript execute = new ExecuteCallScript(call, accountSid, version, url, method,
                  fallbackUrl, fallbackMethod, callback, callbackMethod);
              callManager.tell(execute, null);
              // Create a call detail record for the call.
		      final CallDetailRecord.Builder builder = CallDetailRecord.builder();
		      builder.setSid(callInfo.sid());
		      builder.setDateCreated(callInfo.dateCreated());
		      builder.setAccountSid(accountSid);
		      builder.setTo(callInfo.to());
		      builder.setCallerName(callInfo.fromName());
		      builder.setFrom(callInfo.from());
		      builder.setForwardedFrom(callInfo.forwardedFrom());
		      builder.setStatus(callInfo.state().toString());
		      final DateTime now = DateTime.now();
		      builder.setStartTime(now);
		      builder.setDirection(callInfo.direction());
		      builder.setApiVersion(version);
		      final StringBuilder buffer = new StringBuilder();
		      buffer.append("/").append(version).append("/Accounts/");
		      buffer.append(accountSid.toString()).append("/Calls/");
		      buffer.append(callInfo.sid().toString());
		      final URI uri = URI.create(buffer.toString());
		      builder.setUri(uri);
              final CallDetailRecord cdr = builder.build();
              daos.getCallDetailRecordsDao().addCallDetailRecord(cdr);
              if(APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(cdr), APPLICATION_JSON).build();
              } else if(APPLICATION_XML_TYPE == responseType) {
                return ok(xstream.toXML(new RestCommResponse(cdr)), APPLICATION_XML).build();
              } else {
                return null;
              }
            }
          }
        }
      }
      return status(INTERNAL_SERVER_ERROR).build();
    } catch(final Exception exception) {
      return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
  }
  
  private Integer getTimeout(final MultivaluedMap<String, String> data) {
    Integer result = 60;
    if(data.containsKey("Timeout")) {
      result = Integer.parseInt(data.getFirst("Timeout"));
    }
    return result;
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

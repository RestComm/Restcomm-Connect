/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
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
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdPushEndpoint extends AbstractEndpoint {

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    private ActorRef ussdCallManager;
    private DaoManager daos;
    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private CallDetailRecordListConverter listConverter;

    public UssdPushEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        ussdCallManager = (ActorRef) context.getAttribute("org.mobicents.servlet.restcomm.ussd.telephony.UssdCallManager");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
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

    @SuppressWarnings("unchecked")
    protected Response putCall(final String accountSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid accountId = new Sid(accountSid);
        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Create:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final RuntimeException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        final String from = data.getFirst("From");
        final String to = data.getFirst("To");
        final String username = data.getFirst("Username");
        final String password = data.getFirst("Password");
        final Integer timeout = getTimeout(data);
        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        CreateCall create = null;
        try {
            create = new CreateCall(from, to, username, password, true, timeout != null ? timeout : 30, CreateCall.Type.USSD,
                    accountId);
            create.setCreateCDR(false);
            Future<Object> future = (Future<Object>) ask(ussdCallManager, create, expires);
            Object object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
            Class<?> klass = object.getClass();
            if (CallManagerResponse.class.equals(klass)) {
                final CallManagerResponse<ActorRef> managerResponse = (CallManagerResponse<ActorRef>) object;
                if (managerResponse.succeeded()) {
                    final ActorRef call = managerResponse.get();
                    future = (Future<Object>) ask(call, new GetCallInfo(), expires);
                    object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
                    klass = object.getClass();
                    if (CallResponse.class.equals(klass)) {
                        final CallResponse<CallInfo> callResponse = (CallResponse<CallInfo>) object;
                        if (callResponse.succeeded()) {
                            final CallInfo callInfo = callResponse.get();
                            // Execute the call script.
                            final String version = getApiVersion(data);
                            final URI url = getUrl("Url", data);
                            final String method = getMethod("Method", data);
                            final URI fallbackUrl = getUrl("FallbackUrl", data);
                            final String fallbackMethod = getMethod("FallbackMethod", data);
                            final URI callback = getUrl("StatusCallback", data);
                            final String callbackMethod = getMethod("StatusCallbackMethod", data);
                            final ExecuteCallScript execute = new ExecuteCallScript(call, accountId, version, url, method,
                                    fallbackUrl, fallbackMethod, callback, callbackMethod);
                            ussdCallManager.tell(execute, null);
                            // Create a call detail record for the call.
//                            final CallDetailRecord.Builder builder = CallDetailRecord.builder();
//                            builder.setSid(callInfo.sid());
//                            builder.setDateCreated(callInfo.dateCreated());
//                            builder.setAccountSid(accountId);
//                            builder.setTo(to);
//                            builder.setCallerName(callInfo.fromName());
//                            builder.setFrom(from);
//                            builder.setForwardedFrom(callInfo.forwardedFrom());
//                            builder.setStatus(callInfo.state().toString());
//                            final DateTime now = DateTime.now();
//                            builder.setStartTime(now);
//                            builder.setDirection(callInfo.direction());
//                            builder.setApiVersion(version);
//                            final StringBuilder buffer = new StringBuilder();
//                            buffer.append("/").append(version).append("/Accounts/");
//                            buffer.append(accountId.toString()).append("/Calls/");
//                            buffer.append(callInfo.sid().toString());
//                            final URI uri = URI.create(buffer.toString());
//                            builder.setUri(uri);
//
//                            builder.setCallPath(call.path().toString());
//
//                            final CallDetailRecord cdr = builder.build();
//                            daos.getCallDetailRecordsDao().addCallDetailRecord(cdr);
                            CallDetailRecord cdr = daos.getCallDetailRecordsDao().getCallDetailRecord(callInfo.sid());
                            if (APPLICATION_JSON_TYPE == responseType) {
                                return ok(gson.toJson(cdr), APPLICATION_JSON).build();
                            } else if (APPLICATION_XML_TYPE == responseType) {
                                return ok(xstream.toXML(new RestCommResponse(cdr)), APPLICATION_XML).build();
                            } else {
                                return null;
                            }
                        }
                    }
                }
            }
            return status(INTERNAL_SERVER_ERROR).build();
        } catch (final Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }

    private Integer getTimeout(final MultivaluedMap<String, String> data) {
        Integer result = 60;
        if (data.containsKey("Timeout")) {
            result = Integer.parseInt(data.getFirst("Timeout"));
        }
        return result;
    }

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        if (!data.containsKey("From")) {
            throw new NullPointerException("From can not be null.");
        } else if (!data.containsKey("To")) {
            throw new NullPointerException("To can not be null.");
        } else if (!data.containsKey("Url")) {
            throw new NullPointerException("Url can not be null.");
        }
    }

}

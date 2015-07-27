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
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

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
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.CallinfoConverter;
import org.mobicents.servlet.restcomm.http.converter.CallinfoListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallInfoList;
import org.mobicents.servlet.restcomm.telephony.GetLiveCalls;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.telestax.servlet.MonitoringService;
import com.thoughtworks.xstream.XStream;

import akka.actor.ActorRef;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class SupervisorEndpoint extends AbstractEndpoint{

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    private DaoManager daos;
    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private ActorRef monitoringService;

    public SupervisorEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        monitoringService = (ActorRef) context.getAttribute(MonitoringService.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        super.init(configuration);
        CallinfoConverter converter = new CallinfoConverter(configuration);
        CallinfoListConverter listConverter = new CallinfoListConverter(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(CallInfo.class, converter);
        builder.registerTypeAdapter(CallInfoList.class, listConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(listConverter);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Response pong(final String accountSid, final MediaType responseType) {
        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse("pong");
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson("pong"), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getLiveCalls(final String accountSid, MediaType responseType) {
        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        //Get the list of live calls from Monitoring Service
        CallInfoList liveCalls;
        try {
            final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            GetLiveCalls getLiveCalls = new GetLiveCalls();
            Future<Object> future = (Future<Object>) ask(monitoringService, getLiveCalls, expires);
            liveCalls = (CallInfoList) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (liveCalls != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(liveCalls);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(liveCalls), APPLICATION_JSON).build();
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected Response registerForUpdates(final String accountSid, final MultivaluedMap<String, String> data, MediaType responseType) {
        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        //Get the list of live calls from Monitoring Service
        CallInfoList liveCalls;
        try {
            final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            GetLiveCalls getLiveCalls = new GetLiveCalls();
            Future<Object> future = (Future<Object>) ask(monitoringService, getLiveCalls, expires);
            liveCalls = (CallInfoList) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (liveCalls != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(liveCalls);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(liveCalls), APPLICATION_JSON).build();
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected Response registerForCallUpdates(final String accountSid, final String callSid, final MultivaluedMap<String, String> data, MediaType responseType) {
        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        //Get the list of live calls from Monitoring Service
        CallInfoList liveCalls;
        try {
            final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            GetLiveCalls getLiveCalls = new GetLiveCalls();
            Future<Object> future = (Future<Object>) ask(monitoringService, getLiveCalls, expires);
            liveCalls = (CallInfoList) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (liveCalls != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(liveCalls);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(liveCalls), APPLICATION_JSON).build();
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}

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
package org.restcomm.connect.http;

import akka.actor.ActorRef;
import akka.util.Timeout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.CallDetailRecordFilter;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.http.converter.CallinfoConverter;
import org.restcomm.connect.http.converter.MonitoringServiceConverter;
import org.restcomm.connect.http.converter.MonitoringServiceConverterCallDetails;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.monitoringservice.LiveCallsDetails;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.GetLiveCalls;
import org.restcomm.connect.telephony.api.GetStatistics;
import org.restcomm.connect.telephony.api.MonitoringServiceResponse;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class SupervisorEndpoint extends SecuredEndpoint{
    private static Logger logger = Logger.getLogger(SupervisorEndpoint.class);

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
        MonitoringServiceConverter listConverter = new MonitoringServiceConverter(configuration);
        MonitoringServiceConverterCallDetails callDetailsConverter = new MonitoringServiceConverterCallDetails(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(CallInfo.class, converter);
        builder.registerTypeAdapter(MonitoringServiceResponse.class, listConverter);
        builder.registerTypeAdapter(LiveCallsDetails.class, callDetailsConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(listConverter);
        xstream.registerConverter(callDetailsConverter);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Response pong(final String accountSid, final MediaType responseType) {
        //following 2 things are enough to grant access: 1. a valid authentication token is present. 2 it is a super admin.
        checkAuthenticatedAccount();
        allowOnlySuperAdmin();
        CallDetailRecordFilter filterForTotal;
        try {
            filterForTotal = new CallDetailRecordFilter("", null, null, null, null, null,null,
                    null, null, null, null);
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }
        int totalCalls = daos.getCallDetailRecordsDao().getTotalCallDetailRecords(filterForTotal);
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse("TotalCalls: "+totalCalls);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson("TotalCalls: "+totalCalls), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getMetrics(final String accountSid, final UriInfo info, final MediaType responseType) {
        //following 2 things are enough to grant access: 1. a valid authentication token is present. 2 it is a super admin.
        checkAuthenticatedAccount();
        allowOnlySuperAdmin();
        boolean withLiveCallDetails = false;
        if (info != null && info.getQueryParameters().containsKey("LiveCallDetails") ) {
            withLiveCallDetails = Boolean.parseBoolean(info.getQueryParameters().getFirst("LiveCallDetails"));
        }
        //Get the list of live calls from Monitoring Service
        MonitoringServiceResponse monitoringServiceResponse;
        try {
            final Timeout expires = new Timeout(Duration.create(5, TimeUnit.SECONDS));
            GetStatistics getStatistics = new GetStatistics(withLiveCallDetails, accountSid);
            Future<Object> future = (Future<Object>) ask(monitoringService, getStatistics, expires);
            monitoringServiceResponse = (MonitoringServiceResponse) Await.result(future, Duration.create(5, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (monitoringServiceResponse != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(monitoringServiceResponse);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(monitoringServiceResponse), APPLICATION_JSON).build();
               if(logger.isDebugEnabled()){
                   logger.debug("Supervisor endpoint response: "+gson.toJson(monitoringServiceResponse));
               }
               return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected Response getLiveCalls(final String accountSid, final MediaType responseType) {
        //following 2 things are enough to grant access: 1. a valid authentication token is present. 2 it is a super admin.
        checkAuthenticatedAccount();
        allowOnlySuperAdmin();
        LiveCallsDetails callDetails;
        try {
            final Timeout expires = new Timeout(Duration.create(5, TimeUnit.SECONDS));
            GetLiveCalls getLiveCalls = new GetLiveCalls();
            Future<Object> future = (Future<Object>) ask(monitoringService, getLiveCalls, expires);
            callDetails = (LiveCallsDetails) Await.result(future, Duration.create(5, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (callDetails != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(callDetails);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                Response response = ok(gson.toJson(callDetails), APPLICATION_JSON).build();
                if(logger.isDebugEnabled()){
                    logger.debug("Supervisor endpoint response: "+gson.toJson(callDetails));
                }
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    //Register a remote location where Restcomm will send monitoring updates
    protected Response registerForUpdates(final String accountSid, final UriInfo info, MediaType responseType) {
        //following 2 things are enough to grant access: 1. a valid authentication token is present. 2 it is a super admin.
        checkAuthenticatedAccount();
        allowOnlySuperAdmin();
        boolean withLiveCallDetails = false;
        if (info != null && info.getQueryParameters().containsKey("LiveCallDetails") ) {
            withLiveCallDetails = Boolean.parseBoolean(info.getQueryParameters().getFirst("LiveCallDetails"));
        }
        //Get the list of live calls from Monitoring Service
        MonitoringServiceResponse monitoringServiceResponse;
        try {
            final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            GetStatistics getStatistics = new GetStatistics(withLiveCallDetails, accountSid);
            Future<Object> future = (Future<Object>) ask(monitoringService, getStatistics, expires);
            monitoringServiceResponse = (MonitoringServiceResponse) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (monitoringServiceResponse != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(monitoringServiceResponse);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(monitoringServiceResponse), APPLICATION_JSON).build();
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    //Register a remote location where Restcomm will send monitoring updates for a specific Call
    protected Response registerForCallUpdates(final String accountSid, final String callSid, final MultivaluedMap<String, String> data, MediaType responseType) {
        //following 2 things are enough to grant access: 1. a valid authentication token is present. 2 it is a super admin.
        checkAuthenticatedAccount();
        allowOnlySuperAdmin();
        final String url = data.getFirst("Url");
        final String refresh = data.getFirst("Refresh");
        boolean withLiveCallDetails = false;
        if (data != null && data.containsKey("LiveCallDetails")) {
            withLiveCallDetails = Boolean.parseBoolean(data.getFirst("LiveCallDetails"));
        }
        //Get the list of live calls from Monitoring Service
        MonitoringServiceResponse monitoringServiceResponse;
        try {
            final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
            GetStatistics getStatistics = new GetStatistics(withLiveCallDetails, accountSid);
            Future<Object> future = (Future<Object>) ask(monitoringService, getStatistics, expires);
            monitoringServiceResponse = (MonitoringServiceResponse) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        if (monitoringServiceResponse != null) {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(monitoringServiceResponse);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
               Response response = ok(gson.toJson(monitoringServiceResponse), APPLICATION_JSON).build();
                return response;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}

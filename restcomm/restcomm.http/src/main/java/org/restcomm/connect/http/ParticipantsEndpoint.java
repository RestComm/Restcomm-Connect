/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.http;

import static akka.pattern.Patterns.ask;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.text.ParseException;
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
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.http.converter.CallDetailRecordListConverter;
import org.restcomm.connect.http.converter.RecordingListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.http.converter.ConferenceParticipantConverter;
import org.restcomm.connect.http.converter.RecordingConverter;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.RecordingsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.CallDetailRecordFilter;
import org.restcomm.connect.dao.entities.CallDetailRecordList;
import org.restcomm.connect.dao.entities.Recording;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CallStateChanged;
import org.restcomm.connect.telephony.api.GetCall;
import org.restcomm.connect.telephony.api.GetCallInfo;
import org.restcomm.connect.mscontrol.api.messages.Mute;
import org.restcomm.connect.mscontrol.api.messages.Unmute;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.util.Timeout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@NotThreadSafe
public abstract class ParticipantsEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected ActorRef callManager;
    protected DaoManager daos;
    protected Gson gson;
    protected GsonBuilder builder;
    protected XStream xstream;
    protected CallDetailRecordListConverter listConverter;
    protected AccountsDao accountsDao;
    protected RecordingsDao recordingsDao;
    protected String instanceId;

    public ParticipantsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        callManager = (ActorRef) context.getAttribute("org.restcomm.connect.telephony.CallManager");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        accountsDao = daos.getAccountsDao();
        recordingsDao = daos.getRecordingsDao();
        super.init(configuration);
        ConferenceParticipantConverter converter = new ConferenceParticipantConverter(configuration);
        listConverter = new CallDetailRecordListConverter(configuration);
        final RecordingConverter recordingConverter = new RecordingConverter(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(CallDetailRecord.class, converter);
        builder.registerTypeAdapter(CallDetailRecordList.class, listConverter);
        builder.registerTypeAdapter(Recording.class, recordingConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(recordingConverter);
        xstream.registerConverter(new RecordingListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        xstream.registerConverter(listConverter);

        instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
    }

    protected Response getCall(final String accountSid, final String sid, final MediaType responseType) {
        Account account = daos.getAccountsDao().getAccount(accountSid);
        try {
            secure(account, "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
        final CallDetailRecord cdr = dao.getCallDetailRecord(new Sid(sid));
        if (cdr == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secure(account, cdr.getAccountSid(), SecuredType.SECURED_STANDARD);
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(cdr);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(cdr), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getCalls(final String accountSid, final String conferenceSid, UriInfo info, MediaType responseType) {
        Account account = daos.getAccountsDao().getAccount(accountSid);
        try {
            secure(account, "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        boolean localInstanceOnly = true;
        try {
            String localOnly = info.getQueryParameters().getFirst("localOnly");
            if (localOnly != null && localOnly.equalsIgnoreCase("false"))
                localInstanceOnly = false;
        } catch (Exception e) {
        }

        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");

        String status = CallStateChanged.State.IN_PROGRESS.toString();

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page == "0") ? 0 : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer
                .parseInt(pageSize));

        CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();

        CallDetailRecordFilter filterForTotal;
        try {

            if (localInstanceOnly) {
                filterForTotal = new CallDetailRecordFilter(accountSid, null, null, null, status, null, null,
                        null, conferenceSid, null, null);
            } else {
                filterForTotal = new CallDetailRecordFilter(accountSid, null, null, null, status, null, null,
                        null, conferenceSid, null, null, instanceId);
            }
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final int total = dao.getTotalCallDetailRecords(filterForTotal);

        if (Integer.parseInt(page) > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        CallDetailRecordFilter filter;
        try {
            if (localInstanceOnly) {
                filter = new CallDetailRecordFilter(accountSid, null, null, null, status, null, null,
                        null, conferenceSid, limit, offset);
            } else {
                filter = new CallDetailRecordFilter(accountSid, null, null, null, status, null, null,
                        null, conferenceSid, limit, offset, instanceId);
            }
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final List<CallDetailRecord> cdrs = dao.getCallDetailRecords(filter);
        if (logger.isDebugEnabled()) {
            final List<CallDetailRecord> allCdrs = dao.getCallDetailRecordsByAccountSid(new Sid(accountSid));
            logger.debug("CDR with filter size: "+ cdrs.size()+", all CDR with no filter size: "+allCdrs.size());
            logger.debug("CDRs for ConferenceSid: "+conferenceSid);
            for (CallDetailRecord cdr: allCdrs) {
                logger.debug("CDR sid: "+cdr.getSid()+", status: "+cdr.getStatus()+", conferenceSid: "+cdr.getConferenceSid());
            }
        }

        listConverter.setCount(total);
        listConverter.setPage(Integer.parseInt(page));
        listConverter.setPageSize(Integer.parseInt(pageSize));
        listConverter.setPathUri(info.getRequestUri().getPath());

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new CallDetailRecordList(cdrs));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(new CallDetailRecordList(cdrs)), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Response updateCall(final String sid, final String callSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid accountSid = new Sid(sid);
        Account account = daos.getAccountsDao().getAccount(accountSid);
        try {
            secure(account, "RestComm:Modify:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
        CallDetailRecord cdr = null;
        try {
            cdr = dao.getCallDetailRecord(new Sid(callSid));

            if (cdr != null) {
                try {
                    secure(account, cdr.getAccountSid(), SecuredType.SECURED_STANDARD);
                } catch (final AuthorizationException exception) {
                    return status(UNAUTHORIZED).build();
                }
            } else {
                return Response.status(NOT_ACCEPTABLE).build();
            }
        } catch (Exception e) {
            return status(BAD_REQUEST).build();
        }

        final String mutedStr = data.getFirst("Muted");
        // Mute/UnMute call
        if (mutedStr != null) {

            boolean muted = Boolean.parseBoolean(mutedStr);
            String callPath = null;
            final ActorRef call;
            final CallInfo callInfo;

            try {
                callPath = cdr.getCallPath();
                Future<Object> future = (Future<Object>) ask(callManager, new GetCall(callPath), expires);
                call = (ActorRef) Await.result(future, Duration.create(100000, TimeUnit.SECONDS));

                future = (Future<Object>) ask(call, new GetCallInfo(), expires);
                CallResponse<CallInfo> response = (CallResponse<CallInfo>) Await.result(future,
                        Duration.create(100000, TimeUnit.SECONDS));
                callInfo = response.get();

            } catch (Exception exception) {
                return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
            }
            if (callInfo.state().name().equalsIgnoreCase("IN_PROGRESS")){
                if (muted) {
                    if (call != null) {
                        call.tell(new Mute(), call);
                    }
                } else {
                    if (call != null) {
                        call.tell(new Unmute(), call);
                    }
                }
                cdr = cdr.setMuted(muted);
                dao.updateCallDetailRecord(cdr);
            }
        }
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(cdr), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            return ok(xstream.toXML(new RestCommResponse(cdr)), APPLICATION_XML).build();
        } else {
            return null;
        }
    }
}
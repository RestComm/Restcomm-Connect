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

import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.RecordingsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.CallDetailRecordFilter;
import org.restcomm.connect.dao.entities.CallDetailRecordList;
import org.restcomm.connect.dao.entities.Recording;
import org.restcomm.connect.dao.entities.RecordingList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.http.converter.CallDetailRecordConverter;
import org.restcomm.connect.http.converter.CallDetailRecordListConverter;
import org.restcomm.connect.http.converter.RecordingConverter;
import org.restcomm.connect.http.converter.RecordingListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.CallManagerResponse;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CreateCall;
import org.restcomm.connect.telephony.api.ExecuteCallScript;
import org.restcomm.connect.telephony.api.GetCall;
import org.restcomm.connect.telephony.api.GetCallInfo;
import org.restcomm.connect.telephony.api.Hangup;
import org.restcomm.connect.telephony.api.UpdateCallScript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.thoughtworks.xstream.XStream;

import akka.actor.ActorRef;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

//import org.joda.time.DateTime;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@NotThreadSafe
public abstract class CallsEndpoint extends SecuredEndpoint {
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

    protected boolean normalizePhoneNumbers;

    public CallsEndpoint() {
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
        CallDetailRecordConverter converter = new CallDetailRecordConverter(configuration);
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

        normalizePhoneNumbers = configuration.getBoolean("normalize-numbers-for-outbound-calls");
    }

    protected Response getCall(final String accountSid, final String sid, final MediaType responseType) {
        Account account = daos.getAccountsDao().getAccount(accountSid);
        secure(account, "RestComm:Read:Calls");
        final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
        final CallDetailRecord cdr = dao.getCallDetailRecord(new Sid(sid));
        if (cdr == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(account, cdr.getAccountSid(), SecuredType.SECURED_STANDARD);
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

    // Issue 153: https://bitbucket.org/telestax/telscale-restcomm/issue/153
    // Issue 110: https://bitbucket.org/telestax/telscale-restcomm/issue/110
    protected Response getCalls(final String accountSid, UriInfo info, MediaType responseType) {
        Account account = daos.getAccountsDao().getAccount(accountSid);
        secure(account, "RestComm:Read:Calls");

        boolean localInstanceOnly = true;
        try {
            String localOnly = info.getQueryParameters().getFirst("localOnly");
            if (localOnly != null && localOnly.equalsIgnoreCase("false"))
                localInstanceOnly = false;
        } catch (Exception e) {
        }
        // shall we include sub-accounts cdrs in our query ?
        boolean querySubAccounts = false; // be default we don't
        String querySubAccountsParam = info.getQueryParameters().getFirst("SubAccounts");
        if (querySubAccountsParam != null && querySubAccountsParam.equalsIgnoreCase("true"))
            querySubAccounts = true;

        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");
        // String afterSid = info.getQueryParameters().getFirst("AfterSid");
        String recipient = info.getQueryParameters().getFirst("To");
        String sender = info.getQueryParameters().getFirst("From");
        String status = info.getQueryParameters().getFirst("Status");
        String startTime = info.getQueryParameters().getFirst("StartTime");
        String endTime = info.getQueryParameters().getFirst("EndTime");
        String parentCallSid = info.getQueryParameters().getFirst("ParentCallSid");
        String conferenceSid = info.getQueryParameters().getFirst("ConferenceSid");

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page == "0") ? 0 : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer
                .parseInt(pageSize));

        // Shall we query cdrs of sub-accounts too ?
        // if we do, we need to find the sub-accounts involved first
        List<String> ownerAccounts = null;
        if (querySubAccounts) {
            ownerAccounts = new ArrayList<String>();
            ownerAccounts.add(accountSid); // we will also return parent account cdrs
            ownerAccounts.addAll(accountsDao.getSubAccountSidsRecursive(new Sid(accountSid)));
        }

        CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();

        CallDetailRecordFilter filterForTotal;
        try {

            if (localInstanceOnly) {
                filterForTotal = new CallDetailRecordFilter(accountSid, ownerAccounts, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, null, null);
            } else {
                filterForTotal = new CallDetailRecordFilter(accountSid, ownerAccounts, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, null, null, instanceId);
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
                filter = new CallDetailRecordFilter(accountSid, ownerAccounts, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, limit, offset);
            } else {
                filter = new CallDetailRecordFilter(accountSid, ownerAccounts, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, limit, offset, instanceId);
            }
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final List<CallDetailRecord> cdrs = dao.getCallDetailRecords(filter);

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

    private void normalize(final MultivaluedMap<String, String> data) throws IllegalArgumentException {
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        final String from = data.getFirst("From");
        if (!from.contains("@")) {
            // https://github.com/Mobicents/RestComm/issues/150 Don't complain in case of URIs in the From header
            data.remove("From");
            try {
                data.putSingle("From", phoneNumberUtil.format(phoneNumberUtil.parse(from, "US"), PhoneNumberFormat.E164));
            } catch (final NumberParseException exception) {
                throw new IllegalArgumentException(exception);
            }
        }
        final String to = data.getFirst("To");
        // Only try to normalize phone numbers.
        if (to.startsWith("client")) {
            if (to.split(":").length != 2) {
                throw new IllegalArgumentException(to + " is an invalid client identifier.");
            }
        } else if (!to.contains("@")) {
            data.remove("To");
            try {
                data.putSingle("To", phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164));
            } catch (final NumberParseException exception) {
                throw new IllegalArgumentException(exception);
            }
        }
        URI.create(data.getFirst("Url"));
    }

    @SuppressWarnings("unchecked")
    protected Response putCall(final String accountSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid accountId;
        try {
            accountId = new Sid(accountSid);
        } catch (final IllegalArgumentException exception){
            return status(INTERNAL_SERVER_ERROR).entity(buildErrorResponseBody(exception.getMessage(),responseType)).build();
        }
        secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Create:Calls");
        try {
            validate(data);
            if (normalizePhoneNumbers)
                normalize(data);
        } catch (final RuntimeException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        final String from = data.getFirst("From").trim();
        final String to = data.getFirst("To").trim();
        final String username = data.getFirst("Username");
        final String password = data.getFirst("Password");
        final Integer timeout = getTimeout(data);
        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        CreateCall create = null;
        try {
            if (to.contains("@")) {
                create = new CreateCall(from, to, username, password, true, timeout != null ? timeout : 30, CreateCall.Type.SIP,
                        accountId, null);
            } else if (to.startsWith("client")) {
                create = new CreateCall(from, to, username, password, true, timeout != null ? timeout : 30, CreateCall.Type.CLIENT,
                        accountId, null);
            } else {
                create = new CreateCall(from, to, username, password, true, timeout != null ? timeout : 30, CreateCall.Type.PSTN,
                        accountId, null);
            }
            create.setCreateCDR(false);
            if (callManager == null)
                callManager = (ActorRef) context.getAttribute("org.restcomm.connect.telephony.CallManager");
            Future<Object> future = (Future<Object>) ask(callManager, create, expires);
            Object object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
            Class<?> klass = object.getClass();
            if (CallManagerResponse.class.equals(klass)) {
                final CallManagerResponse<ActorRef> managerResponse = (CallManagerResponse<ActorRef>) object;
                if (managerResponse.succeeded()) {
                    List<ActorRef> dialBranches;
                    if (managerResponse.get() instanceof List) {
                        dialBranches = (List<ActorRef>) managerResponse.get();
                    } else {
                        dialBranches = new CopyOnWriteArrayList<ActorRef>();
                        dialBranches.add(managerResponse.get());
                    }
                    List<CallDetailRecord> cdrs = new CopyOnWriteArrayList<CallDetailRecord>();
                    for (ActorRef call : dialBranches) {
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
                                callManager.tell(execute, null);
                                cdrs.add(daos.getCallDetailRecordsDao().getCallDetailRecord(callInfo.sid()));
                            }
                        }
                    }
                    if (APPLICATION_XML_TYPE == responseType) {
                        if (cdrs.size()==1) {
                            return ok(xstream.toXML(cdrs.get(0)), APPLICATION_XML).build();
                        } else {
                            final RestCommResponse response = new RestCommResponse(new CallDetailRecordList(cdrs));
                            return ok(xstream.toXML(response), APPLICATION_XML).build();
                        }
                    } else if (APPLICATION_JSON_TYPE == responseType) {
                        if (cdrs.size()==1) {
                            return ok(gson.toJson(cdrs.get(0)), APPLICATION_JSON).build();
                        } else {
                            return ok(gson.toJson(cdrs), APPLICATION_JSON).build();
                        }
                    } else {
                        return null;
                    }
//                    if (APPLICATION_JSON_TYPE == responseType) {
//                        return ok(gson.toJson(cdrs), APPLICATION_JSON).build();
//                    } else if (APPLICATION_XML_TYPE == responseType) {
//                        return ok(xstream.toXML(new RestCommResponse(cdrs)), APPLICATION_XML).build();
//                    } else {
//                        return null;
//                    }
                } else {
                    return status(INTERNAL_SERVER_ERROR).entity(managerResponse.cause().getMessage()).build();
                }
            }
            return status(INTERNAL_SERVER_ERROR).build();
        } catch (final Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }

    // Issue 139: https://bitbucket.org/telestax/telscale-restcomm/issue/139
    @SuppressWarnings("unchecked")
    protected Response updateCall(final String sid, final String callSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        final Sid accountSid = new Sid(sid);
        Account account = daos.getAccountsDao().getAccount(accountSid);
        secure(account, "RestComm:Modify:Calls");

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));

        final CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();
        CallDetailRecord cdr = null;
        try {
            cdr = dao.getCallDetailRecord(new Sid(callSid));

            if (cdr != null) {
                secure(account, cdr.getAccountSid(), SecuredType.SECURED_STANDARD);
            } else {
                return Response.status(NOT_ACCEPTABLE).build();
            }
        } catch (Exception e) {
            return status(BAD_REQUEST).build();
        }

        final String url = data.getFirst("Url");
        String method = data.getFirst("Method");
        final String status = data.getFirst("Status");
        final String fallBackUrl = data.getFirst("FallbackUrl");
        String fallBackMethod = data.getFirst("FallbackMethod");
        final String statusCallBack = data.getFirst("StatusCallback");
        String statusCallbackMethod = data.getFirst("StatusCallbackMethod");
        //Restcomm-  Move connected call leg (if exists) to the new URL
        Boolean moveConnectedCallLeg = Boolean.valueOf(data.getFirst("MoveConnectedCallLeg"));

        String callPath = null;
        final ActorRef call;
        final CallInfo callInfo;

        try {
            callPath = cdr.getCallPath();
            Future<Object> future = (Future<Object>) ask(callManager, new GetCall(callPath), expires);
            call = (ActorRef) Await.result(future, Duration.create(10, TimeUnit.SECONDS));

            future = (Future<Object>) ask(call, new GetCallInfo(), expires);
            CallResponse<CallInfo> response = (CallResponse<CallInfo>) Await.result(future,
                    Duration.create(10, TimeUnit.SECONDS));
            callInfo = response.get();
        } catch (Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }

        if (method == null)
            method = "POST";

        if (url != null && status != null) {
            // Throw exception. We can either redirect a running call using Url or change the state of a Call with Status
            final String errorMessage = "You can either redirect a running call using \"Url\" or change the state of a Call with \"Status\"";
            return status(javax.ws.rs.core.Response.Status.CONFLICT).entity(errorMessage).build();
        }

        // Modify state of a call
        if (status != null) {
            if (status.equalsIgnoreCase("canceled")) {
                if (callInfo.state().name().equalsIgnoreCase("queued") || callInfo.state().name().equalsIgnoreCase("ringing")) {
                    if (call != null) {
                        call.tell(new Hangup(), null);
                    }
                } else if (callInfo.state().name().equalsIgnoreCase("wait_for_answer")){
                    // We can cancel Wait For Answer calls
                    if (call != null) {
                        call.tell(new Hangup(SipServletResponse.SC_REQUEST_TERMINATED), null);
                    }
                }
                else{
                    // Do Nothing. We can only cancel Queued or Ringing calls
                }
            }

            if (status.equalsIgnoreCase("completed")) {
                // Specifying "completed" will attempt to hang up a call even if it's already in progress.
                if (call != null) {
                    call.tell(new Hangup(SipServletResponse.SC_REQUEST_TERMINATED), null);
                }
            }
        }

        if (url != null && call != null) {
            try {
                final String version = getApiVersion(data);
                final URI uri = (new URL(url)).toURI();

                URI fallbackUri = (fallBackUrl != null) ? (new URL(fallBackUrl)).toURI() : null;
                fallBackMethod = (fallBackMethod == null) ? "POST" : fallBackMethod;
                URI callbackUri = (statusCallBack != null) ? (new URL(statusCallBack)).toURI() : null;
                statusCallbackMethod = (statusCallbackMethod == null) ? "POST" : statusCallbackMethod;

                final UpdateCallScript update = new UpdateCallScript(call, accountSid, version, uri, method, fallbackUri,
                        fallBackMethod, callbackUri, statusCallbackMethod, moveConnectedCallLeg);
                callManager.tell(update, null);
            } catch (Exception exception) {
                return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
            }
        } else {
            if (logger.isInfoEnabled()) {
                if (url == null) {
                    logger.info("Problem during Call Update, Url is null. Make sure you provide Url parameter");
                }
                if (call == null) {
                    logger.info("Problem during Call update, Call is null. Make sure you provide the proper Call SID");
                }
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

    protected Response getRecordingsByCall(final String accountSid, final String callSid, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Read:Recordings");

        final List<Recording> recordings = recordingsDao.getRecordingsByCall(new Sid(callSid));
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(recordings), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new RecordingList(recordings));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }

    }

}

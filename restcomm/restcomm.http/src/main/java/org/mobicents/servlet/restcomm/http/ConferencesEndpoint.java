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
package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.text.ParseException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordList;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.CallDetailRecordConverter;
import org.mobicents.servlet.restcomm.http.converter.CallDetailRecordListConverter;
import org.mobicents.servlet.restcomm.http.converter.RecordingConverter;
import org.mobicents.servlet.restcomm.http.converter.RecordingListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author maria-farooq@live.com (Maria Farooq)
 */
@NotThreadSafe
public abstract class ConferencesEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    private DaoManager daoManager;
    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private CallDetailRecordListConverter listConverter;

    public ConferencesEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        daoManager = (DaoManager) context.getAttribute(DaoManager.class.getName());
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
    }

    protected Response getConference(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(daoManager.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();
        final CallDetailRecord cdr = dao.getCallDetailRecord(new Sid(sid));
        if (cdr == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(daoManager.getAccountsDao(), accountSid, String.valueOf(cdr.getAccountSid()));
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

    protected Response getConferences(final String accountSid, UriInfo info, MediaType responseType) {

        try {
            secure(daoManager.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
            secureLevelControl(daoManager.getAccountsDao(), accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");
        String recipient = info.getQueryParameters().getFirst("To");
        String sender = info.getQueryParameters().getFirst("From");
        String status = info.getQueryParameters().getFirst("Status");
        String startTime = info.getQueryParameters().getFirst("StartTime");
        String endTime = info.getQueryParameters().getFirst("EndTime");
        String parentCallSid = info.getQueryParameters().getFirst("ParentCallSid");

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page == "0") ? 0 : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer
                .parseInt(pageSize));

        CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();

        CallDetailRecordFilter filterForTotal;
        try {
            filterForTotal = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
                    parentCallSid, null, null);
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final int total = dao.getTotalCallDetailRecords(filterForTotal);

        if (Integer.parseInt(page) > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        CallDetailRecordFilter filter;
        try {
            filter = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
                    parentCallSid, limit, offset);
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

}

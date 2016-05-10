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
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.text.ParseException;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@NotThreadSafe
public abstract class ParticipantsEndpoint extends CallsEndpoint {

    public ParticipantsEndpoint() {
        super();
    }

    protected Response getParticipants(final String accountSid, final String conferenceSid, UriInfo info, MediaType responseType) {

        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Calls");
            secureLevelControl(daos.getAccountsDao(), accountSid, null);
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

        CallDetailRecordsDao dao = daos.getCallDetailRecordsDao();

        CallDetailRecordFilter filterForTotal;
        try {

            if (localInstanceOnly) {
                filterForTotal = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, null, null);
            } else {
                filterForTotal = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
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
                filter = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
                        parentCallSid, conferenceSid, limit, offset);
            } else {
                filter = new CallDetailRecordFilter(accountSid, recipient, sender, status, startTime, endTime,
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

}
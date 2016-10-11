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
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.http.converter.ConferenceDetailRecordConverter;
import org.restcomm.connect.http.converter.ConferenceDetailRecordListConverter;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.commons.dao.Sid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author maria-farooq@live.com (Maria Farooq)
 */
@NotThreadSafe
public abstract class ConferencesEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    private DaoManager daoManager;
    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private ConferenceDetailRecordListConverter listConverter;

    public ConferencesEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        daoManager = (DaoManager) context.getAttribute(DaoManager.class.getName());
        super.init(configuration);
        ConferenceDetailRecordConverter converter = new ConferenceDetailRecordConverter(configuration);
        listConverter = new ConferenceDetailRecordListConverter(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(ConferenceDetailRecord.class, converter);
        builder.registerTypeAdapter(ConferenceDetailRecordList.class, listConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(listConverter);
    }

    protected Response getConference(final String accountSid, final String sid, final MediaType responseType) {
        Account account = daoManager.getAccountsDao().getAccount(accountSid);
        try {
            secure(account, "RestComm:Read:Conferences");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final ConferenceDetailRecordsDao dao = daoManager.getConferenceDetailRecordsDao();
        final ConferenceDetailRecord cdr = dao.getConferenceDetailRecord(new Sid(sid));
        if (cdr == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                //secureLevelControl(daoManager.getAccountsDao(), accountSid, String.valueOf(cdr.getAccountSid()));
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

    protected Response getConferences(final String accountSid, UriInfo info, MediaType responseType) {
        Account account = daoManager.getAccountsDao().getAccount(accountSid);
        try {
            secure(account, "RestComm:Read:Conferences");
            //secureLevelControl(daoManager.getAccountsDao(), accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");
        String status = info.getQueryParameters().getFirst("Status");
        String dateCreated = info.getQueryParameters().getFirst("DateCreated");
        String dateUpdated = info.getQueryParameters().getFirst("DateUpdated");
        String friendlyName = info.getQueryParameters().getFirst("FriendlyName");

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page == "0") ? 0 : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer
                .parseInt(pageSize));

        ConferenceDetailRecordsDao dao = daoManager.getConferenceDetailRecordsDao();

        ConferenceDetailRecordFilter filterForTotal;
        try {
            filterForTotal = new ConferenceDetailRecordFilter(accountSid, status, dateCreated, dateUpdated, friendlyName,
                    null, null);
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final int total = dao.getTotalConferenceDetailRecords(filterForTotal);

        if (Integer.parseInt(page) > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        ConferenceDetailRecordFilter filter;
        try {
            filter = new ConferenceDetailRecordFilter(accountSid, status, dateCreated, dateUpdated, friendlyName,
                    limit, offset);
        } catch (ParseException e) {
            return status(BAD_REQUEST).build();
        }

        final List<ConferenceDetailRecord> cdrs = dao.getConferenceDetailRecords(filter);

        listConverter.setCount(total);
        listConverter.setPage(Integer.parseInt(page));
        listConverter.setPageSize(Integer.parseInt(pageSize));
        listConverter.setPathUri(info.getRequestUri().getPath());

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new ConferenceDetailRecordList(cdrs));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(new ConferenceDetailRecordList(cdrs)), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

}

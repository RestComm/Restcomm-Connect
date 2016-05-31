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
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.QueueDao;
import org.mobicents.servlet.restcomm.entities.Member;
import org.mobicents.servlet.restcomm.entities.MemberList;
import org.mobicents.servlet.restcomm.entities.Queue;
import org.mobicents.servlet.restcomm.entities.QueueRecord;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.MemberConverter;
import org.mobicents.servlet.restcomm.http.converter.MemberListConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author muhammad.bilal@gmail.com (Muhammad Bilal)
 */

@NotThreadSafe
public abstract class MembersEndpoint extends SecuredEndpoint {
    @Context
    private ServletContext context;

    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private String instanceId;
    private AccountsDao accountsDao;
    private QueueDao queueDao;
    private DaoManager daos;

    public MembersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        accountsDao = daos.getAccountsDao();
        queueDao = daos.getQueueDao();
        super.init(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Member.class, new MemberConverter(configuration));
        builder.registerTypeAdapter(MemberList.class, new MemberListConverter(configuration));
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new MemberConverter(configuration));
        xstream.registerConverter(new MemberListConverter(configuration));

        instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
    }

    protected Response getFrontQueueMember(final String accountSid, final String queueSid, UriInfo info,
            MediaType responseType) {
        java.util.Queue<QueueRecord> queueList = new java.util.LinkedList<QueueRecord>();
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Members");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        }
        queueList = queue.toCollectionFromBytes();
        QueueRecord record = queueList.poll();
        queueDao.setQueueBytes(queueList, queue);
        Member member = new Member(new Sid(record.getCallerSid()), record.toDateTime(), 0, 0);
        final RestCommResponse response = new RestCommResponse(member);
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getQueueMember(final String accountSid, final String queueSid, String callSid, UriInfo info,
            MediaType responseType) {
        java.util.Queue<QueueRecord> queueList = new java.util.LinkedList<QueueRecord>();
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Members");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        }
        queueList = queue.toCollectionFromBytes();
        Member member = null;
        boolean found = false;
        int position = 0;
        for (QueueRecord record : queueList) {
            if (record.getCallerSid().equals(callSid)) {
                member = new Member(new Sid(record.getCallerSid()), record.toDateTime(), 0, position);
                queueList.remove(record);
                found = true;
                break;
            }
            ++position;

        }
        if (!found) {
            return status(BAD_REQUEST).build();
        }
        queueDao.setQueueBytes(queueList, queue);
        final RestCommResponse response = new RestCommResponse(member);
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getQueueMembers(final String accountSid, final String queueSid, UriInfo info, MediaType responseType) {
        java.util.Queue<QueueRecord> queueList = new java.util.LinkedList<QueueRecord>();
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Members");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        }
        queueList = queue.toCollectionFromBytes();
        final List<Member> members = new ArrayList<Member>();
        Member member = null;
        int position = 0;
        for (QueueRecord record : queueList) {
            member = new Member(new Sid(record.getCallerSid()), record.toDateTime(), 0, position++);
            members.add(member);
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new MemberList(members));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(members), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response enqueue(final String accountSid, final String queueSid, final String callSid,
            final MultivaluedMap<String, String> data, final MediaType responseType) {

        java.util.Queue<QueueRecord> queueList = new java.util.LinkedList<QueueRecord>();
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Members");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        }
        queueList = queue.toCollectionFromBytes();
        QueueRecord record = new QueueRecord(new Sid(callSid).toString(), new Date());
        queueList.offer(record);
        queueDao.setQueueBytes(queueList, queue);
        Member member = new Member(new Sid(callSid), new DateTime(), 0, queueList.size());
        final RestCommResponse response = new RestCommResponse(member);
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }
}

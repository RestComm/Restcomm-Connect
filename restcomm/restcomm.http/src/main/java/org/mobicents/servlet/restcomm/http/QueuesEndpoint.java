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
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.util.ArrayList;
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
import org.mobicents.servlet.restcomm.entities.Queue;
import org.mobicents.servlet.restcomm.entities.QueueList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Sid.Type;
import org.mobicents.servlet.restcomm.http.converter.QueueConverter;
import org.mobicents.servlet.restcomm.http.converter.QueueListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author muhammad.bilal@gmail.com (Muhammad Bilal)
 */

@NotThreadSafe
public abstract class QueuesEndpoint extends AbstractEndpoint {

    @Context
    private ServletContext context;

    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private String instanceId;
    private AccountsDao accountsDao;
    private DaoManager daos;

    public QueuesEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        accountsDao = daos.getAccountsDao();
        super.init(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Queue.class, new QueueConverter(configuration));
       // builder.registerTypeAdapter(QueueList.class, new QueueListConverter(configuration));
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        xstream.registerConverter(new QueueConverter(configuration));
        xstream.registerConverter(new QueueListConverter(configuration));
        instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
    }

    protected Response getQueues(final String accountSid, UriInfo info, MediaType responseType) {

        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Queues");
            secureLevelControl(daos.getAccountsDao(), accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = new Queue(Sid.generate(Type.QUEUE), new DateTime(), new DateTime(), "testqueue",
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), info.getRequestUri().getPath());
        final Queue queue1 = new Queue(Sid.generate(Type.QUEUE), new DateTime(), new DateTime(), "testqueue",
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), info.getRequestUri());
        List<Queue> queueList = new ArrayList<Queue>();
        queueList.add(queue);
        queueList.add(queue1);
        final RestCommResponse response = new RestCommResponse(new QueueList(queueList));
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getQueue(final String accountSid, final String queueSid, UriInfo info, MediaType responseType) {

        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Queues");
            secureLevelControl(daos.getAccountsDao(), accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = new Queue(Sid.generate(Type.QUEUE), new DateTime(), new DateTime(), "testqueue",
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), info.getRequestUri());

        final RestCommResponse response = new RestCommResponse(queue);
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response enqueue(final String accountSid, final String callSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {

        try {
            secure(daos.getAccountsDao().getAccount(accountSid), "RestComm:Read:Queues");
            secureLevelControl(daos.getAccountsDao(), accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = new Queue(Sid.generate(Type.QUEUE), new DateTime(), new DateTime(), "testqueue",
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), null);

        final RestCommResponse response = new RestCommResponse(queue);
        if (APPLICATION_XML_TYPE == responseType) {

            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(response), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }
}

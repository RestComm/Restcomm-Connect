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
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
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
import org.mobicents.servlet.restcomm.dao.QueuesDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Queue;
import org.mobicents.servlet.restcomm.entities.QueueFilter;
import org.mobicents.servlet.restcomm.entities.QueueList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.QueueConverter;
import org.mobicents.servlet.restcomm.http.converter.QueueListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author muhammad.bilal@gmail.com (Muhammad Bilal)
 */

@NotThreadSafe
public abstract class QueuesEndpoint extends SecuredEndpoint {

    @Context
    private ServletContext context;

    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;
    private String instanceId;
    private AccountsDao accountsDao;
    private QueuesDao queueDao;
    private DaoManager daos;
    private QueueListConverter listConverter;

    public QueuesEndpoint() {
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
        listConverter = new QueueListConverter(configuration);
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Queue.class, new QueueConverter(configuration));
        builder.registerTypeAdapter(QueueList.class, listConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        xstream.registerConverter(new QueueConverter(configuration));
        xstream.registerConverter(listConverter);
        instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
    }

    private Queue createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {

        final DateTime now = DateTime.now();
        final Sid sid = Sid.generate(Sid.Type.QUEUE);

        String friendlyName = "";
        if (data.containsKey("FriendlyName")) {
            friendlyName = data.getFirst("FriendlyName");
        }
        Integer maxSize = 100;
        if (data.containsKey("MaxSize")) {
            maxSize = Integer.valueOf(data.getFirst("MaxSize"));
        }
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString()).append("/Queues/")
                .append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        return new Queue(sid, now, now, friendlyName, 0, 0, maxSize, accountSid, uri);
    }

    protected Response getQueues(final String accountSid, UriInfo info, MediaType responseType) {

        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Queue");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String page = info.getQueryParameters().getFirst("Page");

        if (pageSize == null) {
            pageSize = "50";
        }

        if (page == null) {
            page = "0";
        }

        int limit = Integer.parseInt(pageSize);
        int offset = (page == "0") ? 0
                : (((Integer.parseInt(page) - 1) * Integer.parseInt(pageSize)) + Integer.parseInt(pageSize));

        QueueFilter filter = new QueueFilter(accountSid.toString(), offset, limit);
        final int total = queueDao.getTotalQueueByAccount(filter);
        if (Integer.parseInt(page) > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        filter = new QueueFilter(accountSid.toString(), offset, limit);
        final List<Queue> queues = queueDao.getQueues(filter);

        listConverter.setCount(total);
        listConverter.setPage(Integer.parseInt(page));
        listConverter.setPageSize(Integer.parseInt(pageSize));
        listConverter.setPathUri(info.getRequestUri().getPath());

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new QueueList(queues));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(queues), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getQueue(final String accountSid, final String queueSid, UriInfo info, MediaType responseType) {

        Account operatedAccount = accountsDao.getAccount(accountSid);
        try {
            secure(operatedAccount, "RestComm:Read:Queue");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secure(operatedAccount, queue.getAccountSid(), SecuredType.SECURED_STANDARD);
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(queue);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(queue), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response deleteQueue(final String queueSid, final String accountSid) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        try {
            secure(operatedAccount, "RestComm:Delete:Queue");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(queueSid));
        try {
            secure(operatedAccount, queue.getAccountSid(), SecuredType.SECURED_STANDARD);
        } catch (AuthorizationException e) {
            return status(UNAUTHORIZED).build();
        }
        // Prevent removal of Administrator account
        if (queue != null && queue.getCurrentSize() > 0)
            return status(BAD_REQUEST).build();

        queueDao.removeQueue(new Sid(queueSid));
        return ok().build();
    }

    public Response createQueue(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Queue");
            // secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        } catch (final InvalidMaxSizeException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        Queue queue = queueDao.getQueueByFriendlyName(data.getFirst("FriendlyName"));
        if (queue == null) {
            queue = createFrom(new Sid(accountSid), data);
            queueDao.addQueue(queue);
        } else if (!queue.getAccountSid().toString().equals(accountSid)) {
            return status(CONFLICT)
                    .entity("A queue with the same name was already created by another account. Please, choose a different name and try again.")
                    .build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(queue);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(queue), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response updateQueue(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        try {
            secure(operatedAccount, "RestComm:Modify:Queue");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Queue queue = queueDao.getQueue(new Sid(sid));
        if (queue == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                // secureLevelControl(accountsDao, accountSid, String.valueOf(client.getAccountSid()));
                secure(operatedAccount, queue.getAccountSid(), SecuredType.SECURED_STANDARD);
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            queueDao.updateQueue(update(queue, data));
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(queue);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(queue), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private Queue update(final Queue queue, final MultivaluedMap<String, String> data) {
        Queue result = queue;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("MaxSize")) {
            result = result.setMaxSize(Integer.valueOf(data.getFirst("MaxSize")));
        }
        return result;
    }

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException, InvalidMaxSizeException {
        if (!data.containsKey("FriendlyName")) {
            throw new NullPointerException("Friendly name can not be null.");
        }
        if (data.containsKey("MaxSize")) {
            Integer maxSize = Integer.valueOf(data.getFirst("MaxSize"));
            if (maxSize > 1000) {
                throw new InvalidMaxSizeException("MaxSize cannot be greater than 1000");
            }
        }
    }

    @SuppressWarnings("serial")
    private static class InvalidMaxSizeException extends Exception {
        // Parameterless Constructor
        public InvalidMaxSizeException() {
        }

        // Constructor that accepts a message
        public InvalidMaxSizeException(String message) {
            super(message);
        }
    }

}

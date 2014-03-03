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
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.telephony.GetActiveProxy;
import org.mobicents.servlet.restcomm.telephony.GetProxies;
import org.mobicents.servlet.restcomm.telephony.SwitchProxy;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.util.Timeout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class OutboundProxyEndpoint extends AbstractEndpoint {

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    private ActorRef callManager;
    private DaoManager daos;
    private Gson gson;
    private GsonBuilder builder;
    private XStream xstream;

    public OutboundProxyEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        callManager = (ActorRef) context.getAttribute("org.mobicents.servlet.restcomm.telephony.CallManager");
        daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        super.init(configuration);
        builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Response getProxies(final String accountSid, final MediaType responseType) {
        try {
            secure(new Sid(accountSid), "RestComm:Read:OutboundProxies");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        Map<String, String> proxies;

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        try {
            Future<Object> future = (Future<Object>) ask(callManager, new GetProxies(), expires);
            proxies = (Map<String, String>) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(proxies);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(proxies), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response switchProxy(final String accountSid, final MediaType responseType) {
        try {
            secure(new Sid(accountSid), "RestComm:Read:OutboundProxies");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        Map<String, String> proxyAfterSwitch;

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        try {
            Future<Object> future = (Future<Object>) ask(callManager, new SwitchProxy(new Sid(accountSid)), expires);
            proxyAfterSwitch = (Map<String, String>) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(proxyAfterSwitch);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(proxyAfterSwitch), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response getActiveProxy(final String accountSid, final MediaType responseType) {
        try {
            secure(new Sid(accountSid), "RestComm:Read:OutboundProxies");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }

        Map<String, String> activeProxy;

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        try {
            Future<Object> future = (Future<Object>) ask(callManager, new GetActiveProxy(), expires);
            activeProxy = (Map<String, String>) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(activeProxy);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(activeProxy), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }
}

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
package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.GetLastSmppRequest;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionAttribute;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionInfo;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionRequest;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionResponse;
import org.mobicents.servlet.restcomm.telephony.TextMessage;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.ImmutableMap;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmppSessionHandler extends UntypedActor {
    // Logger
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Runtime stuff.

    private final Configuration configuration;
    private final SipFactory factory;
    private final DaoManager storage;
    private final List<ActorRef> observers;
    private final SipURI transport;
    private final ActorRef monitoringService;

    private final Map<String, Object> attributes;
    // Map for custom headers from inbound SIP MESSAGE
    private ConcurrentHashMap<String, String> customRequestHeaderMap = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, String> customHttpHeaderMap;

    private SmppSessionRequest initial;
    private SmppSessionRequest last;


    public SmppSessionHandler(Configuration Configuration,
            SipFactory sipFactory, SipURI outboundInterface,
            DaoManager storage, ActorRef monitoringService) {

        super();
        this.configuration = Configuration;
        this.factory = sipFactory;
        this.observers = new ArrayList<ActorRef>();
        this.transport = outboundInterface;
        this.attributes = new HashMap<String, Object>();
        this.storage = storage;
        this.monitoringService = monitoringService;


        // TODO Auto-generated constructor stub
    }

    private void inbound(final Object message) throws IOException {
        final ActorRef self = self();
        final String prefix = configuration.getString("outbound-prefix");
        final String service = configuration.getString("outbound-endpoint");
        if (service == null) {
            return;
        }
        String from = null;
        String to = null;
        String body = null;
        if (message instanceof SmppInboundMessageEntity ){
            final SmppInboundMessageEntity request = (SmppInboundMessageEntity) message;
            from = request.getSmppFrom();
            to = request.getSmppTo();
            body = request.getSmppContent();

            // Store the last sms event.
            last = new SmppSessionObjects().new SmppSessionRequest (from, to, body, null);
            if (initial == null) {
                initial = last;
            }
            // Notify the observers.
            for (final ActorRef observer : observers) {
                observer.tell(last, self);
            }


        }else if(message instanceof SmppSessionRequest ){
            final SmppSessionRequest request = (SmppSessionRequest) message;
            from = request.from();
            to = request.to();
            body = request.body();


            // Store the last sms event.
            last = new SmppSessionObjects().new SmppSessionRequest (from, to, body, null);
            if (initial == null) {
                initial = last;
            }


            monitoringService.tell(new TextMessage(from, to, TextMessage.SmsState.OUTBOUND), self());

            final ClientsDao clients = storage.getClientsDao();
            final Client toClient = clients.getClient(to);
            Registration toClientRegistration = null;
            if (toClient != null) {
                final RegistrationsDao registrations = storage.getRegistrationsDao();
                toClientRegistration = registrations.getRegistration(toClient.getLogin());
            }

            final SipApplicationSession application = factory.createApplicationSession();
            StringBuilder buffer = new StringBuilder();
            buffer.append("sip:").append(from).append("@").append(transport.getHost() + ":" + transport.getPort());
            final String sender = buffer.toString();
            buffer = new StringBuilder();
            if (toClient != null && toClientRegistration != null) {
                buffer.append(toClientRegistration.getLocation());
            } else {
                buffer.append("sip:");
                if (prefix != null) {
                    buffer.append(prefix);
                }
                buffer.append(to).append("@").append(service);
            }
            final String recipient = buffer.toString();

            try {
                application.setAttribute(SmppSessionHandler.class.getName(), self);
                if (last.getOrigRequest() != null) {
                    application.setAttribute(SipServletRequest.class.getName(), last.getOrigRequest());
                }
                final SipServletRequest sms = factory.createRequest(application, "MESSAGE", sender, recipient);
                final SipURI uri = (SipURI) factory.createURI(recipient);
                sms.pushRoute(uri);
                sms.setRequestURI(uri);
                sms.setContent(body, "text/plain");
                final SipSession session = sms.getSession();
                session.setHandler("SmsService");
                if (customHttpHeaderMap != null && !customHttpHeaderMap.isEmpty()) {
                    Iterator<String> iter = customHttpHeaderMap.keySet().iterator();
                    while (iter.hasNext()) {
                        String headerName = iter.next();
                        sms.setHeader(headerName, customHttpHeaderMap.get(headerName));
                    }
                }

                sms.send();
            } catch (final Exception exception) {
                // Notify the observers.
                final SmppSessionInfo info = info();
                final SmppSessionResponse error = new SmppSessionObjects().new SmppSessionResponse(info, false);
                for (final ActorRef observer : observers) {
                    observer.tell(error, self);
                }
                // Log the exception.
                logger.error(exception.getMessage(), exception);
            }
        }

    }

    private SmppSessionInfo info() {
        final String from = initial.from();
        final String to = initial.to();
        final Map<String, Object> attributes = ImmutableMap.copyOf(this.attributes);
        return new SmppSessionObjects().new SmppSessionInfo(from, to, attributes);
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (GetLastSmppRequest.class.equals(klass)) {
            if(last == null){
            }else{
                sender.tell(last, self);
            }
        } else if (SmppSessionAttribute.class.equals(klass)) {
            final SmppSessionAttribute attribute = (SmppSessionAttribute) message;
            attributes.put(attribute.name(), attribute.value());
        } else if (SmppSessionRequest.class.equals(klass)) {
            customHttpHeaderMap = ((SmppSessionRequest) message).headers();
            inbound(message);
        }else if (message instanceof SmppInboundMessageEntity) {
            inbound(message);
        }
        else if (message instanceof SipServletResponse) {
            response(message);
        }
    }

    private void response(final Object message) {
        final SipServletResponse response = (SipServletResponse) message;
        final int status = response.getStatus();
        final SmppSessionInfo info = info();
        SmppSessionObjects.SmppSessionResponse result = null;
        if (SipServletResponse.SC_ACCEPTED == status || SipServletResponse.SC_OK == status) {
            result =  new SmppSessionObjects().new SmppSessionResponse(info, true);
        } else {
            result = new SmppSessionObjects().new SmppSessionResponse(info, false);
        }
        // Notify the observers.
        final ActorRef self = self();
        for (final ActorRef observer : observers) {
            observer.tell(result, self);
        }
    }


    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

}

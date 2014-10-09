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
package org.mobicents.servlet.restcomm.sms;

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
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.ImmutableMap;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsSession extends UntypedActor {
    // Logger
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Runtime stuff.
    private final Configuration configuration;
    private final SipFactory factory;
    private final List<ActorRef> observers;
    private final SipURI transport;
    private final Map<String, Object> attributes;
    //Map for custom headers from inbound SIP MESSAGE
    private ConcurrentHashMap<String, String> customRequestHeaderMap = new ConcurrentHashMap<String, String>();
    //Map for custom headers from HTTP App Server (when creating outbound SIP MESSAGE)
    private ConcurrentHashMap<String, String> customHttpHeaderMap;

    private SmsSessionRequest initial;
    private SmsSessionRequest last;

    public SmsSession(final Configuration configuration, final SipFactory factory, final SipURI transport) {
        super();
        this.configuration = configuration;
        this.factory = factory;
        this.observers = new ArrayList<ActorRef>();
        this.transport = transport;
        this.attributes = new HashMap<String, Object>();
    }

    private void inbound(final Object message) throws IOException {
        final SipServletRequest request = (SipServletRequest) message;
        // Handle the SMS.
        SipURI uri = (SipURI) request.getFrom().getURI();
        final String from = uri.getUser();
        uri = (SipURI) request.getTo().getURI();
        final String to = uri.getUser();
        String body = null;
        if (request.getContentLength() > 0) {
            body = new String(request.getRawContent());
        }
        Iterator<String> headerIt = request.getHeaderNames();
        while (headerIt.hasNext()) {
            String headerName = headerIt.next();
            if (headerName.startsWith("X-")) {
                customRequestHeaderMap.put(headerName, request.getHeader(headerName));
            }
        }
        // Store the last sms event.
        last = new SmsSessionRequest(from, to, body, customRequestHeaderMap);
        if (initial == null) {
            initial = last;
        }
        // Notify the observers.
        final ActorRef self = self();
        for (final ActorRef observer : observers) {
            observer.tell(last, self);
        }
    }

    private SmsSessionInfo info() {
        final String from = initial.from();
        final String to = initial.to();
        final Map<String, Object> attributes = ImmutableMap.copyOf(this.attributes);
        return new SmsSessionInfo(from, to, attributes);
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
        } else if (GetLastSmsRequest.class.equals(klass)) {
            sender.tell(last, self);
        } else if (SmsSessionAttribute.class.equals(klass)) {
            final SmsSessionAttribute attribute = (SmsSessionAttribute) message;
            attributes.put(attribute.name(), attribute.value());
        } else if (SmsSessionRequest.class.equals(klass)) {
            customHttpHeaderMap = ((SmsSessionRequest)message).headers();
            outbound(message);
        } else if (message instanceof SipServletRequest) {
            inbound(message);
        } else if (message instanceof SipServletResponse) {
            response(message);
        }
    }

    private void response(final Object message) {
        final SipServletResponse response = (SipServletResponse) message;
        final int status = response.getStatus();
        final SmsSessionInfo info = info();
        SmsSessionResponse result = null;
        if (SipServletResponse.SC_ACCEPTED == status || SipServletResponse.SC_OK == status) {
            result = new SmsSessionResponse(info, true);
        } else {
            result = new SmsSessionResponse(info, false);
        }
        // Notify the observers.
        final ActorRef self = self();
        for (final ActorRef observer : observers) {
            observer.tell(result, self);
        }
    }

    private void outbound(final Object message) {
        last = (SmsSessionRequest) message;
        if (initial == null) {
            initial = last;
        }
        final ActorRef self = self();
        final String from = last.from();
        final String to = last.to();
        final String body = last.body();
        final String prefix = configuration.getString("outbound-prefix");
        final String service = configuration.getString("outbound-endpoint");
        if (service == null) {
            return;
        }
        final SipApplicationSession application = factory.createApplicationSession();
        StringBuilder buffer = new StringBuilder();
        buffer.append("sip:").append(from).append("@").append(transport.getHost() + ":" + transport.getPort());
        final String sender = buffer.toString();
        buffer = new StringBuilder();
        buffer.append("sip:");
        if (prefix != null) {
            buffer.append(prefix);
        }
        buffer.append(to).append("@").append(service);
        final String recipient = buffer.toString();
        try {
            application.setAttribute(SmsSession.class.getName(), self);
            final SipServletRequest sms = factory.createRequest(application, "MESSAGE", sender, recipient);
            final SipURI uri = (SipURI) factory.createURI(recipient);
            sms.pushRoute(uri);
            sms.setRequestURI(uri);
            sms.setContent(body, "text/plain");
            final SipSession session = sms.getSession();
            session.setHandler("SmsService");
            if(customHttpHeaderMap != null && !customHttpHeaderMap.isEmpty()) {
                Iterator<String> iter = customHttpHeaderMap.keySet().iterator();
                while(iter.hasNext()){
                    String headerName = iter.next();
                    sms.setHeader(headerName, customHttpHeaderMap.get(headerName));
                }
            }
            sms.send();
        } catch (final Exception exception) {
            // Notify the observers.
            final SmsSessionInfo info = info();
            final SmsSessionResponse error = new SmsSessionResponse(info, false);
            for (final ActorRef observer : observers) {
                observer.tell(error, self);
            }
            // Log the exception.
            logger.error(exception.getMessage(), exception);
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

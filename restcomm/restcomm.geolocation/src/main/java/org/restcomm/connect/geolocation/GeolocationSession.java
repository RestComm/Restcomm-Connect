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

package org.restcomm.connect.geolocation;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.geolocation.api.GetLastGeolocationRequest;
import org.restcomm.connect.geolocation.api.GeolocationSessionAttribute;
import org.restcomm.connect.geolocation.api.GeolocationSessionInfo;
import org.restcomm.connect.geolocation.api.GeolocationSessionRequest;
import org.restcomm.connect.geolocation.api.GeolocationSessionResponse;
import org.restcomm.connect.dao.ClientsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.RegistrationsDao;
import org.restcomm.connect.dao.entities.Client;
import org.restcomm.connect.dao.entities.Registration;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 */
public final class GeolocationSession extends UntypedActor {
    // Logger
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Runtime stuff.
    private final Configuration geolocationConfiguration;
    private final Configuration configuration;
    private final SipFactory factory;
    private final List<ActorRef> observers;
    private final SipURI transport;
    private final Map<String, Object> attributes;
    // Map for custom headers from inbound SIP MESSAGE
    private ConcurrentHashMap<String, String> customRequestHeaderMap = new ConcurrentHashMap<String, String>();
    // Map for custom headers from HTTP App Server (when creating outbound SIP MESSAGE)
    private ConcurrentHashMap<String, String> customHttpHeaderMap;

    private final DaoManager storage;

    private GeolocationSessionRequest initial;
    private GeolocationSessionRequest last;

    private String externalIP;

    private final ServletContext servletContext;

    private ActorRef smppMessageHandler;

    private final ActorRef monitoringService;

    public GeolocationSession(final Configuration configuration, final SipFactory factory, final SipURI transport,
                              final DaoManager storage, final ActorRef monitoringService, final ServletContext servletContext) {
        super();
        this.configuration = configuration;
        this.geolocationConfiguration = configuration.subset("Geolocation-aggregator");
        this.factory = factory;
        this.observers = new ArrayList<ActorRef>();
        this.transport = transport;
        this.attributes = new HashMap<String, Object>();
        this.storage = storage;
        this.monitoringService = monitoringService;
        this.servletContext = servletContext;
        String defaultHost = transport.getHost();
        this.externalIP = this.configuration.subset("runtime-settings").getString("external-ip");
        if (externalIP == null || externalIP.isEmpty() || externalIP.equals(""))
            externalIP = defaultHost;
    }

    private void inbound(final Object message) throws IOException {
        if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            // Handle the Geolocation.
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
            // Store the last Geolocation event.
            last = new GeolocationSessionRequest(from, to, body, null, null, customRequestHeaderMap);
            if (initial == null) {
                initial = last;
            }
            // Notify the observers.
            final ActorRef self = self();
            for (final ActorRef observer : observers) {
                observer.tell(last, self);
            }
        }
    }

    private GeolocationSessionInfo info() {
        final String from = initial.from();
        final String to = initial.to();
        final Map<String, Object> attributes = ImmutableMap.copyOf(this.attributes);
        return new GeolocationSessionInfo(from, to, attributes);
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
        } else if (GetLastGeolocationRequest.class.equals(klass)) {
            sender.tell(last, self);
        } else if (GeolocationSessionAttribute.class.equals(klass)) {
            final GeolocationSessionAttribute attribute = (GeolocationSessionAttribute) message;
            attributes.put(attribute.name(), attribute.value());
        } else if (GeolocationSessionRequest.class.equals(klass)) {
            customHttpHeaderMap = ((GeolocationSessionRequest) message).headers();
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
        final GeolocationSessionInfo info = info();
        GeolocationSessionResponse result = null;
        if (SipServletResponse.SC_ACCEPTED == status || SipServletResponse.SC_OK == status) {
            result = new GeolocationSessionResponse(info, true);
        } else {
            result = new GeolocationSessionResponse(info, false);
        }
        // Notify the observers.
        final ActorRef self = self();
        for (final ActorRef observer : observers) {
            observer.tell(result, self);
        }
    }

    private void outbound(final Object message) {
        last = (GeolocationSessionRequest) message;
        if (initial == null) {
            initial = last;
        }
        final ActorRef self = self();
        final Charset charset;
        if(logger.isInfoEnabled()) {
            logger.info("Geolocation encoding:  " + last.encoding() );
        }
//        switch(last.encoding()) {
//            case GSM:
//                charset = CharsetUtil.CHARSET_GSM;
//                break;
//            case UCS_2:
//                charset = CharsetUtil.CHARSET_UCS_2;
//                break;
//            case UTF_8:
//                charset = CharsetUtil.CHARSET_UTF_8;
//                break;
//            default:
//                charset = CharsetUtil.CHARSET_GSM;
//        }
//
//        monitoringService.tell(new TextMessage(last.from(), last.to(), TextMessage.GeolocationState.OUTBOUND), self());
        final ClientsDao clients = storage.getClientsDao();
        final Client toClient = clients.getClient(last.to());
        Registration toClientRegistration = null;
        if (toClient != null) {
            final RegistrationsDao registrations = storage.getRegistrationsDao();
            toClientRegistration = registrations.getRegistration(toClient.getLogin());
        }

//        // Try to find an application defined for the phone number.
//        final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
//        IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(to);

        //Turns out that Geolocation was not send using SMPP so we procedd as usual with SIP MESSAGE
        final String prefix = geolocationConfiguration.getString("outbound-prefix");
        final String service = geolocationConfiguration.getString("outbound-endpoint");
        if (service == null) {
            return;
        }

        final SipApplicationSession application = factory.createApplicationSession();
        StringBuilder buffer = new StringBuilder();
        //buffer.append("sip:").append(from).append("@").append(transport.getHost() + ":" + transport.getPort());
        buffer.append("sip:").append(last.from()).append("@").append(externalIP + ":" + transport.getPort());
        final String sender = buffer.toString();
        buffer = new StringBuilder();
        if (toClient != null && toClientRegistration != null) {
            buffer.append(toClientRegistration.getLocation());
        } else {
            buffer.append("sip:");
            if (prefix != null) {
                buffer.append(prefix);
            }
            buffer.append(last.to()).append("@").append(service);
        }
        final String recipient = buffer.toString();

        try {
            application.setAttribute(GeolocationSession.class.getName(), self);
            if (last.getOriginSipRequest() != null) {
                application.setAttribute(SipServletRequest.class.getName(), last.getOriginSipRequest());
            }
            final SipServletRequest Geolocation = factory.createRequest(application, "MESSAGE", sender, recipient);
            final SipURI uri = (SipURI) factory.createURI(recipient);
            Geolocation.pushRoute(uri);
            Geolocation.setRequestURI(uri);
            Geolocation.setContent(last.body(), "text/plain");
            final SipSession session = Geolocation.getSession();
            session.setHandler("GeolocationService");
            if (customHttpHeaderMap != null && !customHttpHeaderMap.isEmpty()) {
                Iterator<String> iter = customHttpHeaderMap.keySet().iterator();
                while (iter.hasNext()) {
                    String headerName = iter.next();
                    Geolocation.setHeader(headerName, customHttpHeaderMap.get(headerName));
                }
            }
            Geolocation.send();
        } catch (final Exception exception) {
            // Notify the observers.
            final GeolocationSessionInfo info = info();
            final GeolocationSessionResponse error = new GeolocationSessionResponse(info, false);
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


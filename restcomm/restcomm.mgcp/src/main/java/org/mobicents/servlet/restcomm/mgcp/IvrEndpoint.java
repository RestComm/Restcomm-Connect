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
package org.mobicents.servlet.restcomm.mgcp;

import static jain.protocol.ip.mgcp.message.parms.ReturnCode.Transaction_Executed_Normally;
import jain.protocol.ip.mgcp.JainIPMgcpException;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.protocols.mgcp.jain.pkg.AUMgcpEvent;
import org.mobicents.protocols.mgcp.jain.pkg.AUPackage;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class IvrEndpoint extends GenericEndpoint {
    private static final PackageName PACKAGE_NAME = AUPackage.AU;
    private static final RequestedEvent[] REQUESTED_EVENTS = new RequestedEvent[2];
    static {
        final RequestedAction[] action = new RequestedAction[] { RequestedAction.NotifyImmediately };
        REQUESTED_EVENTS[0] = new RequestedEvent(new EventName(PACKAGE_NAME, AUMgcpEvent.auoc), action);
        REQUESTED_EVENTS[1] = new RequestedEvent(new EventName(PACKAGE_NAME, AUMgcpEvent.auof), action);
    }
    private static final String EMPTY_STRING = new String();
    private static final String DEFAULT_REQUEST_ID = "0";

    private final NotifiedEntity agent;
    private final List<ActorRef> observers;

    public IvrEndpoint(final ActorRef gateway, final MediaSession session, final NotifiedEntity agent, final String domain) {
        super(gateway, session, agent, new EndpointIdentifier("mobicents/ivr/$", domain));
        this.agent = agent;
        this.observers = new ArrayList<ActorRef>();
    }

    private void send(final Object message) {
        final Class<?> klass = message.getClass();
        final String parameters = message.toString();
        MgcpEvent event = null;
        if (Play.class.equals(klass)) {
            event = AUMgcpEvent.aupa.withParm(parameters);
        } else if (PlayCollect.class.equals(klass)) {
            event = AUMgcpEvent.aupc.withParm(parameters);
        } else if (PlayRecord.class.equals(klass)) {
            event = AUMgcpEvent.aupr.withParm(parameters);
        }
        final EventName[] signal = new EventName[1];
        signal[0] = new EventName(PACKAGE_NAME, event);
        final RequestIdentifier requestId = new RequestIdentifier(DEFAULT_REQUEST_ID);
        final NotificationRequest request = new NotificationRequest(self(), id, requestId);
        request.setNotifiedEntity(agent);
        request.setRequestedEvents(REQUESTED_EVENTS);
        request.setSignalRequests(signal);
        gateway.tell(request, self());
    }

    private void stop() {
        final EventName[] signal = new EventName[1];
        signal[0] = new EventName(PACKAGE_NAME, AUMgcpEvent.aues);
        final RequestIdentifier requestId = new RequestIdentifier(DEFAULT_REQUEST_ID);
        final NotificationRequest request = new NotificationRequest(self(), id, requestId);
        request.setSignalRequests(signal);
        request.setNotifiedEntity(agent);
        request.setRequestedEvents(REQUESTED_EVENTS);
        gateway.tell(request, self());
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (Observe.class.equals(klass)) {
            final Observe request = (Observe) message;
            final ActorRef observer = request.observer();
            if (observer != null) {
                observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        } else if (StopObserving.class.equals(klass)) {
            final StopObserving request = (StopObserving) message;
            final ActorRef observer = request.observer();
            if (observer != null) {
                observers.remove(observer);
            }
        } else if (InviteEndpoint.class.equals(klass)) {
            final EndpointCredentials credentials = new EndpointCredentials(id);
            sender.tell(credentials, self);
        } else if (UpdateEndpointId.class.equals(klass)) {
            final UpdateEndpointId request = (UpdateEndpointId) message;
            id = request.id();
        } else if (Play.class.equals(klass) || PlayCollect.class.equals(klass) || PlayRecord.class.equals(klass)) {
            send(message);
        } else if (StopEndpoint.class.equals(klass)) {
            stop();
        } else if (Notify.class.equals(klass)) {
            notification(message);
        } else if (NotificationRequestResponse.class.equals(klass)) {
            response(message);
        }
    }

    private void fail(final int code) {
        // Notify observers that the event failed.
        final ActorRef self = self();
        final String error = Integer.toString(code);
        final String message = "The IVR request failed with the following error code " + error;
        final JainIPMgcpException exception = new JainIPMgcpException(message);
        final IvrEndpointResponse<String> response = new IvrEndpointResponse<String>(exception);
        for (final ActorRef observer : observers) {
            observer.tell(response, self);
        }
    }

    private void response(final Object message) {
        final NotificationRequestResponse response = (NotificationRequestResponse) message;
        final ReturnCode code = response.getReturnCode();
        if (!Transaction_Executed_Normally.equals(code)) {
            final int value = code.getValue();
            fail(value);
        }
    }

    private void notification(final Object message) {
        final Notify notification = (Notify) message;
        final ActorRef self = self();
        // Let the media server know we successfully got the notification.
        final NotifyResponse response = new NotifyResponse(this, Transaction_Executed_Normally);
        final int transaction = notification.getTransactionHandle();
        response.setTransactionHandle(transaction);
        gateway.tell(response, self);
        // We are only expecting one event "operation completed" or "operation failed".
        final EventName[] observedEvents = notification.getObservedEvents();
        if (observedEvents.length == 1) {
            final MgcpEvent event = observedEvents[0].getEventIdentifier();
            final Map<String, String> parameters = parse(event.getParms());
            final int code = Integer.parseInt(parameters.get("rc"));
            switch (code) {
                case 326: // No digits
                case 327: // No speech
                case 328: // Spoke too long
                case 329: // Digit pattern not matched
                case 100: { // Success
                    String digits = parameters.get("dc");
                    if (digits == null) {
                        digits = EMPTY_STRING;
                    }
                    // Notify the observers that the event successfully completed.
                    final IvrEndpointResponse<String> result = new IvrEndpointResponse<String>(digits);
                    for (final ActorRef observer : observers) {
                        observer.tell(result, self);
                    }
                    break;
                }
                default: {
                    fail(code);
                }
            }
        }
    }

    private Map<String, String> parse(final String input) {
        final Map<String, String> parameters = new HashMap<String, String>();
        final String[] tokens = input.split(" ");
        for (final String token : tokens) {
            final String[] values = token.split("=");
            if (values.length == 1) {
                parameters.put(values[0], null);
            } else if (values.length == 2) {
                parameters.put(values[0], values[1]);
            }
        }
        return parameters;
    }
}

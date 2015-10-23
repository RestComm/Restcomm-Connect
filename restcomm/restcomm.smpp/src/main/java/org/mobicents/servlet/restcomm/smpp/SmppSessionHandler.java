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
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipServletResponse;

import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.GetLastSmppRequest;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionAttribute;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionInfo;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppSessionRequest;

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

    private final List<ActorRef> observers;
    private final Map<String, Object> attributes;


    private SmppSessionRequest initial;
    private SmppSessionRequest last;

    public SmppSessionHandler() {
        super();
        this.observers = new ArrayList<ActorRef>();
        this.attributes = new HashMap<String, Object>();

    }

    private void inbound(final Object message) throws IOException {

        String from = null;
        String to = null;
        String body = null;
        if (message instanceof SmppInboundMessageEntity ){
            final SmppInboundMessageEntity request = (SmppInboundMessageEntity) message;
            from = request.getSmppFrom();
            to = request.getSmppTo();
            body = request.getSmppContent();
        }else if(message instanceof SmppSessionRequest ){
            final SmppSessionRequest request = (SmppSessionRequest) message;
            from = request.from();
            to = request.to();
            body = request.body();
        }

        // Store the last sms event.
        last = new SmppSessionObjects().new SmppSessionRequest (from, to, body, null);
        if (initial == null) {
            initial = last;
        }
        // Notify the observers.
        final ActorRef self = self();
        for (final ActorRef observer : observers) {
            observer.tell(last, self);
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
            //customHttpHeaderMap = ((SmppSessionRequest) message).headers();
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

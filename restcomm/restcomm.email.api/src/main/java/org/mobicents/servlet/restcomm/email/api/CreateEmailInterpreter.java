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
package org.mobicents.servlet.restcomm.email.api;

import javax.mail.Session;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.mobicents.servlet.restcomm.email.EmailRequest;
import org.mobicents.servlet.restcomm.email.EmailResponse;
import org.mobicents.servlet.restcomm.email.Mail;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public abstract class CreateEmailInterpreter extends UntypedActor {
    // Logger.
    final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Email client session.
    final Session session;
    private final List<ActorRef> observers;

    public CreateEmailInterpreter(final Session session) { //Constructor for Email-service
        super();
        this.session = session;
        this.observers = new ArrayList<ActorRef>();
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

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
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
        }else if (EmailRequest.class.equals(klass)) {
            EmailRequest request = (EmailRequest)message;
            sender.tell(send(request.getObject()), self);
        }
    }

    abstract EmailResponse send(final Mail mail);

}



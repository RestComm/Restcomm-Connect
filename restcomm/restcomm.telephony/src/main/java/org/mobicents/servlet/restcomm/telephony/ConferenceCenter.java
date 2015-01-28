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
package org.mobicents.servlet.restcomm.telephony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 */
public final class ConferenceCenter extends UntypedActor {
    private final MediaServerControllerFactory factory;
    private final Map<String, ActorRef> conferences;
    private final Map<String, List<ActorRef>> initializing;

    public ConferenceCenter(final MediaServerControllerFactory factory) {
        super();
        this.factory = factory;
        this.conferences = new HashMap<String, ActorRef>();
        this.initializing = new HashMap<String, List<ActorRef>>();
    }

    private ActorRef getConference(final String name) {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Conference(name, factory.provideConferenceController().getSelf());
            }
        }));
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        if (CreateConference.class.equals(klass)) {
            create(message, sender);
        } else if (ConferenceStateChanged.class.equals(klass)) {
            notify(message, sender);
        } else if (DestroyConference.class.equals(klass)) {
            destroy(message);
        }
    }

    private void destroy(final Object message) {
        final DestroyConference request = (DestroyConference) message;
        final String name = request.name();
        final ActorRef conference = conferences.remove(name);
        final UntypedActorContext context = getContext();
        if (conference != null) {
            context.stop(conference);
        }
    }

    private void notify(final Object message, final ActorRef sender) {
        final ConferenceStateChanged update = (ConferenceStateChanged) message;
        final String name = update.name();
        final ActorRef self = self();
        // Stop observing events from the conference room.
        sender.tell(new StopObserving(self), self);
        // Figure out what happened.
        ConferenceCenterResponse response = null;
        if (ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT == update.state()
                || ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT == update.state()) {
            conferences.put(name, sender);
            response = new ConferenceCenterResponse(sender);
        } else {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("The conference room ").append(name).append(" failed to initialize.");
            final CreateConferenceException exception = new CreateConferenceException(buffer.toString());
            response = new ConferenceCenterResponse(exception);
        }
        // Notify the observers.
        final List<ActorRef> observers = initializing.remove(name);
        for (final ActorRef observer : observers) {
            observer.tell(response, self);
        }
        // Clean up.
        observers.clear();
    }

    private void create(final Object message, final ActorRef sender) {
        final CreateConference request = (CreateConference) message;
        final String name = request.name();
        final ActorRef self = self();
        // Check to see if the conference already exists.
        ActorRef conference = conferences.get(name);
        if (conference != null && !conference.isTerminated()) {
            sender.tell(new ConferenceCenterResponse(conference), self);
            return;
        }
        // Check to see if it's already created but not initialized.
        // If it is then just add it to the list of observers that will
        // be notified when the conference room is ready.
        List<ActorRef> observers = initializing.get(name);
        if (observers != null) {
            observers.add(sender);
        } else {
            observers = new ArrayList<ActorRef>();
            observers.add(sender);
            conference = getConference(name);
            conference.tell(new Observe(self), self);
            conference.tell(new StartConference(), self);
            initializing.put(name, observers);
        }
    }
}

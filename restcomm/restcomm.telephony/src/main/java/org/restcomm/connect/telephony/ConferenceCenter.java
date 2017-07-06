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
package org.restcomm.connect.telephony;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.telephony.api.ConferenceCenterResponse;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;
import org.restcomm.connect.telephony.api.CreateConference;
import org.restcomm.connect.telephony.api.DestroyConference;
import org.restcomm.connect.telephony.api.StartConference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public final class ConferenceCenter extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final MediaServerControllerFactory factory;
    private final Map<String, ActorRef> conferences;
    private final Map<String, List<ActorRef>> initializing;
    private final DaoManager storage;

    public ConferenceCenter(final MediaServerControllerFactory factory, final DaoManager storage) {
        super();
        this.factory = factory;
        this.conferences = new HashMap<String, ActorRef>();
        this.initializing = new HashMap<String, List<ActorRef>>();
        this.storage = storage;
    }

    private ActorRef getConference(final String name) {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                //Here Here we can pass Gateway where call is connected
                return new Conference(name, factory, storage, getSelf());
            }
        });
        return getContext().actorOf(props);
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(" ********** ConferenceCenter " + self().path() + ", Processing Message: " + message.getClass().getName() + " isTerminated? "+self().isTerminated());
        }
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
        // sender.tell(new StopObserving(self), self);

        // Figure out what happened.
        ConferenceCenterResponse response = null;
        if (isRunning(update.state())) {
            // Only executes during a conference initialization
            // Adds conference to collection if started successfully
            if (!conferences.containsKey(name)) {
                if(logger.isInfoEnabled()) {
                    logger.info("Conference " + name + " started successfully");
                }
                conferences.put(name, sender);
                response = new ConferenceCenterResponse(sender);
            }
        } else if (ConferenceStateChanged.State.COMPLETED.equals(update.state())) {
            // A conference completed with no errors
            // Remove it from conference collection and stop the actor
            if(logger.isInfoEnabled()) {
                logger.info("Conference " + name + " completed without issues");
            }
            conferences.remove(update.name());
            //stop sender(conference who sent this msg) bcz it was already removed from map in Stopping state
            context().stop(sender);
        } else if (ConferenceStateChanged.State.STOPPING.equals(update.state())) {
            // A conference is in stopping state
            // Remove it from conference collection
            // https://github.com/RestComm/Restcomm-Connect/issues/2312
            if(logger.isInfoEnabled()) {
                logger.info("Conference " + name + " is going to stop, will remove it from available conferences.");
            }
            conferences.remove(update.name());
        } else if (ConferenceStateChanged.State.FAILED.equals(update.state())) {
            if (conferences.containsKey(name)) {
                // A conference completed with errors
                // Remove it from conference collection and stop the actor
                if(logger.isInfoEnabled()) {
                    logger.info("Conference " + name + " completed with issues");
                }
                ActorRef conference = conferences.remove(update.name());
                context().stop(conference);
            } else {
                // Failed to initialize a conference
                // Warn voice interpreter conference initialization failed
                if(logger.isInfoEnabled()) {
                    logger.info("Conference " + name + " failed to initialize");
                }
                final StringBuilder buffer = new StringBuilder();
                buffer.append("The conference room ").append(name).append(" failed to initialize.");
                final CreateConferenceException exception = new CreateConferenceException(buffer.toString());
                response = new ConferenceCenterResponse(exception);
            }
        }

        // Notify the observers if any
        final List<ActorRef> observers = initializing.remove(name);
        if (observers != null) {
            for (final ActorRef observer : observers) {
                observer.tell(response, self);
            }
            observers.clear();
        }
    }

    private boolean isRunning(ConferenceStateChanged.State state) {
        return ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT.equals(state)
                || ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT.equals(state);
    }

    private void create(final Object message, final ActorRef sender) {
        final CreateConference request = (CreateConference) message;
        final String name = request.name();
        final ActorRef self = self();
        // Check to see if the conference already exists.
        ActorRef conference = conferences.get(name);
        if(logger.isDebugEnabled()) {
            logger.debug("ConferenceCenter conference: " + conference + " name: "+name);
        }
        if (conference != null && !conference.isTerminated()) {
            sender.tell(new ConferenceCenterResponse(conference), self);
            return;
        }
        // Check to see if it's already created but not initialized.
        // If it is then just add it to the list of observers that will
        // be notified when the conference room is ready.
        List<ActorRef> observers = initializing.get(name);
        if (observers != null) {
            if(logger.isDebugEnabled()) {
                logger.debug("ConferenceCenter this conference is already being initialize. sender will be notied when conference is successfuly started."+ " ConferenceCenter isTerminated? "+self().isTerminated());
            }
            observers.add(sender);
        } else {
            if(logger.isDebugEnabled()) {
                logger.debug("ConferenceCenter this conference initialization started. sender will be notied when conference is successfuly started."+ " ConferenceCenter isTerminated? "+self().isTerminated());
            }
            observers = new ArrayList<ActorRef>();
            observers.add(sender);
            conference = getConference(name);
            conference.tell(new Observe(self), self);
            conference.tell(new StartConference(request.initialitingCallSid()), self);
            initializing.put(name, observers);

            if(logger.isDebugEnabled()) {
                logger.debug("Conference: "+ conference +" "+ " Conference isTerminated? "+conference.isTerminated()+ " ConferenceCenter isTerminated? "+self().isTerminated());
            }
        }
    }
}

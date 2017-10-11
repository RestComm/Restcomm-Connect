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

package org.restcomm.connect.mscontrol.mms;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerConferenceControllerStateChanged;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.mscontrol.api.MediaServerController;
import org.restcomm.connect.mscontrol.api.messages.CloseMediaSession;
import org.restcomm.connect.mscontrol.api.messages.CreateMediaSession;
import org.restcomm.connect.mscontrol.api.messages.JoinCall;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.JoinConference;
import org.restcomm.connect.mscontrol.api.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.restcomm.connect.mscontrol.api.messages.Stop;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Maria Farooq (maria.farooq@telestax.com)
 */
@Immutable
public final class MockFailingMmsConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Observers
    private final List<ActorRef> observers;
    private Sid conferenceSid;

    public MockFailingMmsConferenceController(final ActorRef mrb) {
        super();
        // Observers
        this.observers = new ArrayList<ActorRef>(2);
    }

    private void broadcast(Object message) {
        if (!this.observers.isEmpty()) {
            final ActorRef self = self();
            synchronized (this.observers) {
                for (ActorRef observer : observers) {
                    observer.tell(message, self);
                }
            }
        }
    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        final ActorRef self = self();

        if(logger.isInfoEnabled()) {
            logger.info(" ********** MockFailingMmsConferenceController Conference Controller Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (CreateMediaSession.class.equals(klass)) {
        	broadcast(new MediaServerConferenceControllerStateChanged(MediaServerControllerState.ACTIVE, conferenceSid, ""+ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT, true));
        } else if (Stop.class.equals(klass)) {
        	//do nothing so conference stay in stopping state
        } else if(JoinCall.class.equals(klass)){
        	onJoinCall((JoinCall)message, self, sender);
        } else{
        	logger.error("Unhanldles Request Received.");
        }
    }

    private void onJoinCall(JoinCall message, ActorRef self, ActorRef sender) {
    	sender.tell(new JoinComplete(), message.getCall());
    }

    private void onObserve(Observe message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            synchronized (this.observers) {
                this.observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.remove(observer);
        }
    }

}

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
package org.restcomm.connect.data.recorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.data.recorder.api.interfaces.CallDataRecorder;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 *
 */
@Immutable
public final class CallDataRecorderImpl extends UntypedActor implements CallDataRecorder{

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final List<ActorRef> observers;
    private final DaoManager daoManager;

    public CallDataRecorderImpl(final DaoManager daoManager) {
        super();
        this.observers = Collections.synchronizedList(new ArrayList<ActorRef>());
        this.daoManager = daoManager;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if(logger.isInfoEnabled()) {
            logger.info("********** CallDataRecorder" + self().path() + " Processing Message: \"" + klass.getName() + " sender : "
                + sender.path().toString());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        }
    }

    private void onObserve(Observe message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = message.observer();
        if (observer != null) {
            synchronized (this.observers) {
                this.observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    private void onStopObserving(StopObserving stopObservingMessage, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = stopObservingMessage.observer();
        if (observer != null) {
            observer.tell(stopObservingMessage, self);
            this.observers.remove(observer);
        } else {
            Iterator<ActorRef> observerIter = observers.iterator();
            while (observerIter.hasNext()) {
                ActorRef observerNext = observerIter.next();
                observerNext.tell(stopObservingMessage, self);
            }
            this.observers.clear();
        }
    }

    @Override
    public void postStop() {
        try {
            onStopObserving(new StopObserving(), self(), null);
            getContext().stop(self());
        } catch (Exception exception) {
            if(logger.isInfoEnabled()) {
                logger.info("Exception during Call postStop while trying to remove observers: "+exception);
            }
        }
        super.postStop();
    }
}

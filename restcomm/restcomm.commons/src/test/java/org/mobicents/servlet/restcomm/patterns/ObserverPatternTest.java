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
package org.mobicents.servlet.restcomm.patterns;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.patterns.TooManyObserversException;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public class ObserverPatternTest {
    private static ActorSystem system;

    public ObserverPatternTest() {
        super();
    }

    @BeforeClass
    public static void before() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void after() {
        system.shutdown();
    }

    @Test
    public void testSuccessScenario() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create an observable actor.
                final Props properties = new Props(ObservableActor.class);
                final ActorRef observable = system.actorOf(properties);
                // Start observing the observable actor.
                observable.tell(new Observe(observer), observer);
                // Verify that we are now observing the observable actor.
                Observing response = expectMsgClass(Observing.class);
                assertTrue(response.succeeded());
                // Tell the observable actor to broadcast an event.
                observable.tell(new BroadcastHelloWorld(), observer);
                // Verify we get the broadcasted event.
                expectMsgEquals("Hello World!");
                // Stop observing the observable actor.
                observable.tell(new StopObserving(observer), observer);
            }
        };
    }

    @Test
    public void testTooManyObserversException() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create an observable actor.
                final Props properties = new Props(ObservableActor.class);
                final ActorRef observable = system.actorOf(properties);
                // Start observing the observable actor.
                observable.tell(new Observe(observer), observer);
                // Verify that we are now observing the observable actor.
                Observing response = expectMsgClass(Observing.class);
                assertTrue(response.succeeded());
                // Try to observe twice.
                observable.tell(new Observe(observer), observer);
                // Verify that observing more than once is not allowed.
                response = expectMsgClass(Observing.class);
                assertFalse(response.succeeded());
                assertTrue(response.cause() instanceof TooManyObserversException);
                // Stop observing the observable actor.
                observable.tell(new StopObserving(observer), observer);
            }
        };
    }

    private static final class BroadcastHelloWorld {
    }

    private static final class ObservableActor extends UntypedActor {
        private final List<ActorRef> listeners;

        @SuppressWarnings("unused")
        public ObservableActor() {
            super();
            listeners = new ArrayList<ActorRef>();
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            final ActorRef self = self();
            final ActorRef sender = sender();
            if (Observe.class.equals(klass)) {
                final Observe request = (Observe) message;
                final ActorRef observer = request.observer();
                if (!listeners.contains(observer)) {
                    listeners.add(observer);
                    final Observing response = new Observing(self);
                    sender.tell(response, self);
                } else {
                    final Observing response = new Observing(new TooManyObserversException("Already observing this actor."));
                    sender.tell(response, self);
                }
            } else if (BroadcastHelloWorld.class.equals(klass)) {
                for (final ActorRef listener : listeners) {
                    listener.tell("Hello World!", self());
                }
            } else if (StopObserving.class.equals(klass)) {
                final StopObserving request = (StopObserving) message;
                final ActorRef observer = request.observer();
                if (listeners.contains(observer)) {
                    listeners.remove(observer);
                }
            }
        }
    }
}

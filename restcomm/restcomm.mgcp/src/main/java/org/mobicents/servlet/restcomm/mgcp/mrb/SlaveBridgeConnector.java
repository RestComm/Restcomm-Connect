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
package org.mobicents.servlet.restcomm.mgcp.mrb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.fsm.TransitionFailedException;
import org.mobicents.servlet.restcomm.fsm.TransitionNotFoundException;
import org.mobicents.servlet.restcomm.fsm.TransitionRollbackException;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.StartSlaveBridgeConnector;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SlaveBridgeConnector extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;

    // Observer pattern
    private final List<ActorRef> observers;

    final ActorRef masterMediaGateway;

    public SlaveBridgeConnector(final ActorRef masterMediaGateway){
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.uninitialized));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        this.initializing = new State("initializing", new Initializing(source), null);

        // Observers
        this.observers = new ArrayList<ActorRef>(1);
        this.masterMediaGateway = masterMediaGateway;
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** SlaveBridgeConnector " + self().path() + " Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (StartSlaveBridgeConnector.class.equals(klass)){
            onStartSlaveBridgeConnector((StartSlaveBridgeConnector) message, self, sender);
        }
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

    private void onStartSlaveBridgeConnector(StartSlaveBridgeConnector message, ActorRef self, ActorRef sender) throws TransitionFailedException, TransitionNotFoundException, TransitionRollbackException {
        fsm.transition(message, initializing);
    }

    /*
     * ACTIONS
     *
     */

    protected abstract class AbstractAction implements Action {

        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class Initializing extends AbstractAction {

        public Initializing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object msg) throws Exception {

        }

    }
}

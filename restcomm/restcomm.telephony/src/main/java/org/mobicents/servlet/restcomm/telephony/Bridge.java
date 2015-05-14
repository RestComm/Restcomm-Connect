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

package org.mobicents.servlet.restcomm.telephony;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.BridgeStateChanged.BridgeState;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Bridge extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;
    private final State ready;
    private final State bridging;
    private final State halfBridged;
    private final State bridged;
    private final State stopping;
    private final State failed;
    private final State complete;

    // Media Server Controller
    private final ActorRef mscontroller;

    // Call bridging
    private ActorRef inboundCall;
    private ActorRef outboundCall;

    // Observer pattern
    private final List<ActorRef> observers;

    public Bridge(final ActorRef mscontroller) {
        final ActorRef source = self();

        // Media Server Controller
        this.mscontroller = mscontroller;

        // States for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.initializing = new State("initializing", new Initializing(source), null);
        this.ready = new State("ready", new Ready(source), null);
        this.bridging = new State("bridging", new Bridging(source), null);
        this.halfBridged = new State("half bridged", new HalfBridged(source), null);
        this.bridged = new State("bridged", new Bridged(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.complete = new State("complete", new Complete(source), null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializing));
        transitions.add(new Transition(initializing, failed));
        transitions.add(new Transition(initializing, ready));
        transitions.add(new Transition(ready, bridging));
        transitions.add(new Transition(ready, stopping));
        transitions.add(new Transition(bridging, halfBridged));
        transitions.add(new Transition(bridging, stopping));
        transitions.add(new Transition(halfBridged, stopping));
        transitions.add(new Transition(halfBridged, bridged));
        transitions.add(new Transition(bridged, stopping));
        transitions.add(new Transition(stopping, failed));
        transitions.add(new Transition(stopping, complete));

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // Observer pattern
        this.observers = new ArrayList<ActorRef>(3);
    }

    private boolean is(final State state) {
        return this.fsm.state().equals(state);
    }

    private void broadcast(final Object message) {
        if (!this.observers.isEmpty()) {
            final ActorRef self = self();
            for (ActorRef observer : observers) {
                observer.tell(message, self);
            }
        }
    }

    /*
     * Events
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();

        logger.info("********** Bridge " + self.path() + " State: \"" + state.toString());
        logger.info("********** Bridge " + self.path() + " Processing: \"" + klass.getName() + " Sender: " + sender.path());

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (StartBridge.class.equals(klass)) {
            onStartBridge((StartBridge) message, self, sender);
        } else if (JoinCalls.class.equals(klass)) {
            onJoinCalls((JoinCalls) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (MediaServerControllerResponse.class.equals(klass)) {
            onMediaServerControllerResponse((MediaServerControllerResponse<?>) message, self, sender);
        } else if (MediaServerControllerStateChanged.class.equals(klass)) {
            onMediaServerControllerStateChanged((MediaServerControllerStateChanged) message, self, sender);
        } else if (MediaGroupResponse.class.equals(klass)) {
            onMediaGroupResponse((MediaGroupResponse<?>) message, self, sender);
        } else if (StopBridge.class.equals(klass)) {
            onStopBridge((StopBridge) message, self, sender);
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

    private void onStartBridge(StartBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.fsm.transition(message, initializing);
        }
    }

    private void onJoinCalls(JoinCalls message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ready)) {
            this.inboundCall = message.getInboundCall();
            this.outboundCall = message.getOutboundCall();
            fsm.transition(message, bridging);
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) throws Exception {
        if (is(bridging)) {
            this.fsm.transition(message, halfBridged);
        } else if (is(halfBridged)) {
            this.fsm.transition(message, bridged);
        }
    }

    private void onMediaServerControllerResponse(MediaServerControllerResponse<?> message, ActorRef self, ActorRef sender) {
        Object obj = message.get();
        Class<?> klass = obj.getClass();
        // XXX Implement when necessary
    }

    private void onMediaServerControllerStateChanged(MediaServerControllerStateChanged message, ActorRef self, ActorRef sender)
            throws Exception {
        MediaServerControllerState state = message.getState();
        switch (state) {
            case ACTIVE:
                if (is(initializing)) {
                    this.fsm.transition(message, ready);
                }
                break;
            case INACTIVE:
                if (is(stopping)) {
                    this.fsm.transition(message, complete);
                }
                break;
            case FAILED:
                if (is(initializing)) {
                    this.fsm.transition(message, failed);
                }
                break;
            default:
                // ignore unknown state
                break;
        }
    }

    private void onMediaGroupResponse(MediaGroupResponse<?> message, ActorRef self, ActorRef sender) {
        if (message.succeeded()) {
            // XXX do something
        } else {
            // XXX do something
        }

    }

    private void onStopBridge(StopBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ready) || is(bridging) || is(halfBridged) || is(bridged)) {
            this.fsm.transition(message, stopping);
        }
    }

    /*
     * Actions
     */
    private abstract class AbstractAction implements Action {

        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private class Initializing extends AbstractAction {

        public Initializing(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Start observing state changes in the MSController
            final Observe observe = new Observe(super.source);
            mscontroller.tell(observe, super.source);

            // Initialize the MS Controller
            final CreateMediaSession createMediaSession = new CreateMediaSession();
            mscontroller.tell(createMediaSession, super.source);
        }

    }

    private class Ready extends AbstractAction {

        public Ready(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeStateChanged.BridgeState.READY);
            broadcast(notification);
        }

    }

    private class Bridging extends AbstractAction {

        public Bridging(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Ask mscontroller to join inbound call
            final JoinCall join = new JoinCall(inboundCall, ConnectionMode.SendRecv);
            mscontroller.tell(join, super.source);
        }

    }

    private class HalfBridged extends AbstractAction {

        public HalfBridged(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Notify observers that inbound call has been bridged successfully
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeStateChanged.BridgeState.HALF_BRIDGED);
            broadcast(notification);

            // Ask mscontroller to join outbound call
            final JoinCall join = new JoinCall(outboundCall, ConnectionMode.SendRecv);
            mscontroller.tell(join, super.source);
        }

    }

    private class Bridged extends AbstractAction {

        public Bridged(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeStateChanged.BridgeState.BRIDGED);
            broadcast(notification);
        }

    }

    private class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            inboundCall = null;
            outboundCall = null;

            // Ask the MS Controller to stop
            // This will stop any current media operations and clean media resources
            mscontroller.tell(new Stop(), super.source);
        }

    }

    private class Complete extends AbstractAction {

        public Complete(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeState.INACTIVE);
            broadcast(notification);
            observers.clear();
        }

    }

    private class Failed extends AbstractAction {

        public Failed(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeState.FAILED);
            broadcast(notification);
            observers.clear();
        }

    }

}

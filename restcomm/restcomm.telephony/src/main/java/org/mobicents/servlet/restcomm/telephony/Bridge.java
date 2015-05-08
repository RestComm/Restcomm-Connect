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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.Leave;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.patterns.Observe;
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
    private final State initializingMediaResources;
    private final State ready;
    private final State joining;
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
    private ActorRef interpreter;

    // Observer pattern
    private final List<ActorRef> observers;

    public Bridge(final ActorRef mscontroller) {
        final ActorRef source = self();

        // Media Server Controller
        this.mscontroller = mscontroller;

        // States for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.initializingMediaResources = new State("initializing media resources", new InitializingMediaResources(source),
                null);
        this.ready = new State("ready", new Ready(source), null);
        this.joining = new State("joining", new Joining(source), null);
        this.halfBridged = new State("half bridged", new HalfBridged(source), null);
        this.bridged = new State("bridged", new Bridged(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.complete = new State("complete", new Complete(source), null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializingMediaResources));
        transitions.add(new Transition(initializingMediaResources, failed));
        transitions.add(new Transition(initializingMediaResources, ready));
        transitions.add(new Transition(ready, joining));
        transitions.add(new Transition(ready, stopping));
        transitions.add(new Transition(joining, halfBridged));
        transitions.add(new Transition(joining, bridged));
        transitions.add(new Transition(joining, stopping));
        transitions.add(new Transition(halfBridged, stopping));
        transitions.add(new Transition(halfBridged, joining));
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

        if (StartBridge.class.equals(klass)) {
            onStartBridge((StartBridge) message, self, sender);
        } else if (AddParticipant.class.equals(klass)) {
            onAddParticipant((AddParticipant) message, self, sender);
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

    private void onStartBridge(StartBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.fsm.transition(message, initializingMediaResources);
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ready) || is(halfBridged)) {
            fsm.transition(message, joining);
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) throws Exception {
        if (is(joining)) {
            if (this.outboundCall == null) {
                this.fsm.transition(message, halfBridged);
            } else {
                this.fsm.transition(message, bridged);
            }
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
                if (is(initializingMediaResources)) {
                    this.fsm.transition(message, ready);
                }
                break;
            case INACTIVE:
                if (is(stopping)) {
                    this.fsm.transition(message, complete);
                }
                break;
            case FAILED:
                if (is(initializingMediaResources)) {
                    this.fsm.transition(message, failed);
                }
                break;
            default:
                // ignore unknown state
                break;
        }
    }
    
    private void onMediaGroupResponse(MediaGroupResponse<?> message, ActorRef self, ActorRef sender) {
        if(message.succeeded()) {
            // XXX do something
        } else {
            // XXX do something
        }
        
    }

    private void onStopBridge(StopBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ready) || is(joining) || is(halfBridged) || is(bridged)) {
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

    private class InitializingMediaResources extends AbstractAction {

        public InitializingMediaResources(ActorRef source) {
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

    private class Joining extends AbstractAction {

        public Joining(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            Class<?> klass = message.getClass();

            if (AddParticipant.class.equals(klass)) {
                AddParticipant addParticipant = (AddParticipant) message;
                if (inboundCall == null) {
                    // Half Bridged
                    inboundCall = addParticipant.call();
                } else {
                    // Fully Bridged
                    outboundCall = addParticipant.call();
                }
                mscontroller.tell(addParticipant, super.source);
            }
        }

    }

    private class HalfBridged extends AbstractAction {

        public HalfBridged(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final BridgeStateChanged notification = new BridgeStateChanged(BridgeStateChanged.BridgeState.HALF_BRIDGED);
            broadcast(notification);
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
            // Ask participants (if any) to leave the bridge
            remove(inboundCall);
            remove(outboundCall);

            // Ask the MS Controller to stop
            // This will stop any current media operations and clean media resources
            mscontroller.tell(new Stop(), super.source);
        }

        private void remove(ActorRef call) {
            if (call != null) {
                call.tell(new Leave(), super.source);
            }
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
        }

    }

}

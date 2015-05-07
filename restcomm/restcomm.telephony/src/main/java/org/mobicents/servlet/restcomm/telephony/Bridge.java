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
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupCreated;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupDestroyed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionClosed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionInfo;

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
    private final State openingMediaSession;
    private final State creatingMediaGroup;
    private final State ready;
    private final State joining;
    private final State halfBridged;
    private final State bridged;
    private final State destroyingMediaGroup;
    private final State closingMediaSession;
    private final State failed;
    private final State complete;
    private Boolean fail;

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
        this.openingMediaSession = new State("opening media session", new OpeningMediaSession(source), null);
        this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(source), null);
        this.ready = new State("ready", new Ready(source), null);
        this.joining = new State("joining", new Joining(source), null);
        this.halfBridged = new State("half bridged", new HalfBridged(source), null);
        this.bridged = new State("bridged", new Bridged(source), null);
        this.destroyingMediaGroup = new State("destroying media group", new DestroyingMediaGroup(source), null);
        this.closingMediaSession = new State("closing media session", new ClosingMediaSession(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.complete = new State("complete", new Complete(source), null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, openingMediaSession));
        transitions.add(new Transition(openingMediaSession, failed));
        transitions.add(new Transition(openingMediaSession, creatingMediaGroup));
        transitions.add(new Transition(creatingMediaGroup, closingMediaSession));
        transitions.add(new Transition(creatingMediaGroup, ready));
        transitions.add(new Transition(ready, joining));
        transitions.add(new Transition(ready, destroyingMediaGroup));
        transitions.add(new Transition(joining, halfBridged));
        transitions.add(new Transition(joining, bridged));
        transitions.add(new Transition(joining, destroyingMediaGroup));
        transitions.add(new Transition(halfBridged, destroyingMediaGroup));
        transitions.add(new Transition(halfBridged, joining));
        transitions.add(new Transition(bridged, destroyingMediaGroup));
        transitions.add(new Transition(destroyingMediaGroup, closingMediaSession));
        transitions.add(new Transition(closingMediaSession, failed));
        transitions.add(new Transition(closingMediaSession, complete));

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        this.fail = Boolean.FALSE;

        // Observer pattern
        this.observers = new ArrayList<ActorRef>(3);
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
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
        } else if (MediaServerControllerError.class.equals(klass)) {
            onMediaServerControllerError((MediaServerControllerError) message, self, sender);
        } else if (StopBridge.class.equals(klass)) {
            onStopBridge((StopBridge) message, self, sender);
        }
    }

    private void onStartBridge(StartBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.fsm.transition(message, openingMediaSession);
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) {
        if (is(ready)) {
            this.inboundCall = message.call();
            this.mscontroller.tell(message, self);
        } else if (is(halfBridged)) {
            this.outboundCall = message.call();
            this.mscontroller.tell(message, self);
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        if (is(joining)) {

        }
    }

    private void onMediaServerControllerResponse(MediaServerControllerResponse<?> message, ActorRef self, ActorRef sender) {
        Object obj = message.get();
        Class<?> klass = obj.getClass();

        if (MediaSessionInfo.class.equals(klass)) {
            if (is(openingMediaSession)) {

            }
        } else if (MediaGroupCreated.class.equals(klass)) {
            if (is(creatingMediaGroup)) {

            }
        } else if (MediaGroupDestroyed.class.equals(klass)) {
            if (is(destroyingMediaGroup)) {

            }
        } else if (MediaSessionClosed.class.equals(klass)) {
            if (is(closingMediaSession)) {

            }
        }
    }

    private void onMediaServerControllerError(MediaServerControllerError message, ActorRef self, ActorRef sender) {
        if (is(openingMediaSession)) {

        } else if (is(creatingMediaGroup)) {

        }
    }

    private void onStopBridge(StopBridge message, ActorRef self, ActorRef sender) {
        if (is(ready)) {

        } else if (is(joining)) {

        } else if (is(halfBridged)) {

        } else if (is(bridged)) {

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

    private class OpeningMediaSession extends AbstractAction {

        public OpeningMediaSession(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class CreatingMediaGroup extends AbstractAction {

        public CreatingMediaGroup(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class Ready extends AbstractAction {

        public Ready(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class Joining extends AbstractAction {

        public Joining(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class HalfBridged extends AbstractAction {

        public HalfBridged(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class Bridged extends AbstractAction {

        public Bridged(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class DestroyingMediaGroup extends AbstractAction {

        public DestroyingMediaGroup(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class ClosingMediaSession extends AbstractAction {

        public ClosingMediaSession(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class Complete extends AbstractAction {

        public Complete(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

    private class Failed extends AbstractAction {

        public Failed(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // TODO Auto-generated method stub

        }

    }

}

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

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.Leave;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartRecording;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopRecording;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author henrique.rosa@telestax.com (Henrique Rosa)
 */
@Immutable
public final class Conference extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite state machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;
    private final State waiting;
    private final State running;
    private final State stopping;
    private final State stopped;
    private final State failed;

    // Runtime stuff
    private final String name;
    private final List<ActorRef> calls;
    private final List<ActorRef> observers;

    // Media Session Controller
    private final ActorRef mscontroller;

    public Conference(final String name, final ActorRef msController) {
        super();
        final ActorRef source = self();

        // Finite states
        this.uninitialized = new State("uninitialized", null, null);
        this.initializing = new State("initialiing", new Initializing(source), null);
        this.waiting = new State("waiting", new Waiting(source), null);
        this.running = new State("running", new Running(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);
        this.stopped = new State("stopped", new Stopped(source), null);
        this.failed = new State("failed", new Failed(source), null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializing));
        transitions.add(new Transition(initializing, waiting));
        transitions.add(new Transition(initializing, stopping));
        transitions.add(new Transition(initializing, failed));
        transitions.add(new Transition(waiting, running));
        transitions.add(new Transition(waiting, stopping));
        transitions.add(new Transition(running, stopping));
        transitions.add(new Transition(stopping, stopped));
        transitions.add(new Transition(stopping, failed));

        // Finite state machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // Runtime stuff
        this.name = name;
        this.mscontroller = msController;
        this.calls = new ArrayList<ActorRef>();
        this.observers = new ArrayList<ActorRef>();
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private boolean isRunning() {
        return is(waiting) || is(running);
    }

    private void broadcast(final Object message) {
        if (!this.observers.isEmpty()) {
            final ActorRef self = self();
            for (ActorRef observer : observers) {
                observer.tell(message, self);
            }
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** Conference " + self().path() + " Current State: " + state.toString());
            logger.info(" ********** Conference " + self().path() + " Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (GetConferenceInfo.class.equals(klass)) {
            onGetConferenceInfo((GetConferenceInfo) message, self, sender);
        } else if (StartConference.class.equals(klass)) {
            onStartConference((StartConference) message, self, sender);
        } else if (StopConference.class.equals(klass)) {
            onStopConference((StopConference) message, self, sender);
        } else if (ConferenceModeratorPresent.class.equals(klass)) {
            onConferenceModeratorPresent((ConferenceModeratorPresent) message, self, sender);
        } else if (AddParticipant.class.equals(klass)) {
            onAddParticipant((AddParticipant) message, self, sender);
        } else if (RemoveParticipant.class.equals(klass)) {
            onRemoveParticipant((RemoveParticipant) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (MediaServerControllerStateChanged.class.equals(klass)) {
            onMediaServerControllerStateChanged((MediaServerControllerStateChanged) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecording((StartRecording) message, self, sender);
        } else if (StopRecording.class.equals(klass)) {
            onStopRecording((StopRecording) message, self, sender);
        }
    }

    /*
     * ACTIONS
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

    private final class Waiting extends AbstractAction {
        public Waiting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            broadcast(new ConferenceStateChanged(name, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT));
        }
    }

    private final class Running extends AbstractAction {
        public Running(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Stop the background music if present
            mscontroller.tell(new StopMediaGroup(), super.source);

            // Notify the observers
            broadcast(new ConferenceStateChanged(name, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT));
        }
    }

    private class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Tell every call to leave the conference room.
            if (!calls.isEmpty()) {
                for (final ActorRef call : calls) {
                    final Leave leave = new Leave();
                    call.tell(leave, super.source);
                }
                calls.clear();
            }

            // Ask the MS Controller to stop
            // This will stop any current media operations and clean media resources
            mscontroller.tell(new Stop(), super.source);
        }

    }

    private abstract class FinalizingAction extends AbstractAction {

        protected final ConferenceStateChanged.State finalState;

        public FinalizingAction(ActorRef source, ConferenceStateChanged.State state) {
            super(source);
            finalState = state;
        }

        @Override
        public void execute(Object message) throws Exception {
            // Notify the observers.
            broadcast(new ConferenceStateChanged(name, this.finalState));
            observers.clear();
        }

    }

    private final class Stopped extends FinalizingAction {

        public Stopped(final ActorRef source) {
            super(source, ConferenceStateChanged.State.COMPLETED);
        }

    }

    private final class Failed extends FinalizingAction {

        public Failed(final ActorRef source) {
            super(source, ConferenceStateChanged.State.FAILED);
        }

    }

    /*
     * EVENTS
     */
    private void onObserve(Observe message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private void onGetConferenceInfo(GetConferenceInfo message, ActorRef self, ActorRef sender) throws Exception {
        ConferenceInfo information = null;
        if (is(waiting)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT, name);
        } else if (is(running)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT, name);
        } else if (is(stopped)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.COMPLETED, name);
        }
        sender.tell(new ConferenceResponse<ConferenceInfo>(information), self);
    }

    private void onStartConference(StartConference message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.fsm.transition(message, initializing);
        }
    }

    private void onStopConference(StopConference message, ActorRef self, ActorRef sender) throws Exception {
        if (is(initializing)) {
            this.fsm.transition(message, stopped);
        } else if (is(waiting) || is(running)) {
            this.fsm.transition(message, stopping);
        }
    }

    private void onConferenceModeratorPresent(ConferenceModeratorPresent message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(waiting)) {
            this.fsm.transition(message, running);
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            final JoinCall joinCall = new JoinCall(message.call(), ConnectionMode.Confrnce);
            this.mscontroller.tell(joinCall, self);
        }
    }

    private void onRemoveParticipant(RemoveParticipant message, ActorRef self, ActorRef sender) throws Exception {
        if (isRunning()) {
            final ActorRef call = message.call();
            if (calls.remove(call)) {
                final Leave leave = new Leave();
                call.tell(leave, self);
            }

            if (calls.size() == 0) {
                // If no more participants we should stop it now.
                fsm.transition(message, stopping);
            }
        }
    }

    private void onMediaServerControllerStateChanged(MediaServerControllerStateChanged message, ActorRef self, ActorRef sender)
            throws Exception {
        MediaServerControllerState state = message.getState();
        switch (state) {
            case ACTIVE:
                if (is(initializing)) {
                    this.fsm.transition(message, waiting);
                }
                break;
            case INACTIVE:
                if (is(stopping)) {
                    this.fsm.transition(message, stopped);
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

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        this.calls.add(sender);
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        }
    }

    private void onStartRecording(StartRecording message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        }
    }

    private void onStopRecording(StopRecording message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        }
    }

}

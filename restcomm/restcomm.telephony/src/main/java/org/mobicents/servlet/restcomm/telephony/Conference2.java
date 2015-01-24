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

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.interpreter.StopInterpreter;
import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.DestroyConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.Join;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.Leave;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionControllerResponse;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author henrique.rosa@telestax.com (Henrique Rosa)
 *
 */
@Immutable
public final class Conference2 extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite state machine
    private final FiniteStateMachine fsm;

    // Finite States
    private final State uninitialized;
    private final State runningModeratorAbsent;
    private final State runningModeratorPresent;
    private final State completed;

    // Intermediate states
    private final State creatingMediaSession;
    private final State stopping;

    // Runtime stuff
    private final String name;
    private ActorRef confVoiceInterpreter;
    private final List<ActorRef> calls;
    private final List<ActorRef> observers;

    // Media Session Controller
    // XXX initialize mscontroller for conference
    private ActorRef mscontroller;

    public Conference2(final String name) {
        super();
        final ActorRef source = self();

        // Finite states
        this.uninitialized = new State("uninitialized", null, null);
        this.runningModeratorAbsent = new State("running moderator absent", null, null);
        this.runningModeratorPresent = new State("running moderator present", null, null);
        this.completed = new State("completed", null, null);

        // Intermediate states
        this.creatingMediaSession = new State("creating media session", null, null);
        this.stopping = new State("stopping", null, null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, creatingMediaSession));
        transitions.add(new Transition(creatingMediaSession, runningModeratorPresent));
        transitions.add(new Transition(creatingMediaSession, runningModeratorAbsent));
        transitions.add(new Transition(creatingMediaSession, completed));
        transitions.add(new Transition(runningModeratorPresent, stopping));
        transitions.add(new Transition(runningModeratorPresent, runningModeratorAbsent));
        transitions.add(new Transition(runningModeratorAbsent, stopping));
        transitions.add(new Transition(runningModeratorAbsent, runningModeratorPresent));
        transitions.add(new Transition(stopping, completed));

        // Finite state machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // Runtime stuff
        this.name = name;
        this.calls = new ArrayList<ActorRef>();
        this.observers = new ArrayList<ActorRef>();
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private boolean isRunning() {
        return is(runningModeratorAbsent) || is(runningModeratorPresent);
    }

    private void stopAndCleanConfVoiceInter(final ActorRef source) {
        if (this.confVoiceInterpreter != null) {
            StopInterpreter stopInterpreter = StopInterpreter.instance();
            this.confVoiceInterpreter.tell(stopInterpreter, source);
            this.confVoiceInterpreter = null;
        }
    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** Conference Current State: " + state.toString());
            logger.info(" ********** Conference Processing Message: " + klass.getName());
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
        } else if (MediaSessionControllerResponse.class.equals(klass)) {

        } else if (ConferenceModeratorPresent.class.equals(klass)) {
            onConferenceModeratorPresent((ConferenceModeratorPresent) message, self, sender);
        } else if (CreateWaitUrlConfMediaGroup.class.equals(klass)) {
            onCreateWaitUrlConfMediaGroup((CreateWaitUrlConfMediaGroup) message, self, sender);
        } else if (DestroyWaitUrlConfMediaGroup.class.equals(klass)) {
            onDestroyWaitUrlConfMediaGroup((DestroyWaitUrlConfMediaGroup) message, self, sender);
        } else if (AddParticipant.class.equals(klass)) {
            onAddParticipant((AddParticipant) message, self, sender);
        } else if (RemoveParticipant.class.equals(klass)) {
            onRemoveParticipant((RemoveParticipant) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (CreateMediaGroup.class.equals(klass)) {
            onCreateMediaGroup((CreateMediaGroup) message, self, sender);
        } else if (DestroyMediaGroup.class.equals(klass)) {
            onDestroyMediaGroup((DestroyMediaGroup) message, self, sender);
        }
    }

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
        if (is(runningModeratorAbsent)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT);
        } else if (is(runningModeratorPresent)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT);
        } else if (is(completed)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.COMPLETED);
        }
        sender.tell(new ConferenceResponse<ConferenceInfo>(information), self);
    }

    private void onStartConference(StartConference message, ActorRef self, ActorRef sender) throws Exception {
        this.fsm.transition(message, creatingMediaSession);
    }

    private void onStopConference(StopConference message, ActorRef self, ActorRef sender) throws Exception {
        if (is(creatingMediaSession)) {
            this.fsm.transition(message, completed);
        } else if (is(runningModeratorAbsent) || is(runningModeratorPresent)) {
            this.fsm.transition(message, stopping);
        }
    }

    private void onConferenceModeratorPresent(ConferenceModeratorPresent message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(runningModeratorAbsent)) {
            this.fsm.transition(message, runningModeratorPresent);
        }
    }

    private void onCreateWaitUrlConfMediaGroup(CreateWaitUrlConfMediaGroup message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            ActorRef confVoiceInterpreterTmp = message.getConfVoiceInterpreter();
            if (this.confVoiceInterpreter == null) {
                this.confVoiceInterpreter = confVoiceInterpreterTmp;
                final StartInterpreter startInterpreter = new StartInterpreter(self);
                this.confVoiceInterpreter.tell(startInterpreter, self);
            } else {
                StopInterpreter stopInterpreter = StopInterpreter.instance();
                confVoiceInterpreterTmp.tell(stopInterpreter, self);
            }
        }
    }

    private void onDestroyWaitUrlConfMediaGroup(DestroyWaitUrlConfMediaGroup message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            ActorRef waitUrlMediaGroup = message.getWaitUrlConfMediaGroup();
            if (waitUrlMediaGroup != null && !waitUrlMediaGroup.isTerminated()) {
                final UntypedActorContext context = getContext();
                context.stop(waitUrlMediaGroup);
            }

            // ConferenceVoiceInterpreter is dead now. Set it to null
            this.confVoiceInterpreter = null;
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            final Join join = new Join(cnf, ConnectionMode.Confrnce);
            final ActorRef call = message.call();
            call.tell(join, self);
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
                // If no more participants and back ground music was on, we should stop it now.
                logger.info("calls size is zero in conference " + name);
                stopAndCleanConfVoiceInter(self);
                if (is(runningModeratorPresent)) {
                    fsm.transition(message, runningModeratorAbsent);
                }
            }
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        this.calls.add(sender);
    }
    
    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // XXX Forward media group creation to MSController
            final ActorRef group = getMediaGroup(message);
            sender.tell(new ConferenceResponse<ActorRef>(group), sender);
        }
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // XXX Forward media group destruction to MSController
            getContext().stop(message.group());
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

    private final class CreatingMediaSession extends AbstractAction {

        public CreatingMediaSession(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            mscontroller.tell(new CreateMediaSession(), super.source);
        }

    }

    private final class RunningModeratorAbsent extends AbstractAction {
        public RunningModeratorAbsent(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            gateway.tell(new DestroyConnection(connection), source);
            connection = null;
            // Notify the observers.
            final ConferenceStateChanged event = new ConferenceStateChanged(name,
                    ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

    private final class RunningModeratorPresent extends AbstractAction {
        public RunningModeratorPresent(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Stop the background music if present
            stopAndCleanConfVoiceInter(source);

            // Notify the observers.
            final ConferenceStateChanged event = new ConferenceStateChanged(name,
                    ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

    private final class Stopping extends AbstractAction {
        public Stopping(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final State state = fsm.state();
            if (openingConnection.equals(state)) {
                connection.tell(new CloseConnection(), source);
            }
        }
    }

    private final class Stopped extends AbstractAction {

        public Stopped(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Tell every call to leave the conference room.
            for (final ActorRef call : calls) {
                final Leave leave = new Leave();
                call.tell(leave, source);
            }
            calls.clear();

            // Clean up resources
            final ActorRef self = self();
            if (connection != null) {
                gateway.tell(new DestroyConnection(connection), self);
                connection = null;
            }
            if (cnf != null) {
                gateway.tell(new DestroyEndpoint(cnf), self);
                cnf = null;
            }

            stopAndCleanConfVoiceInter(source);
            // Notify the observers.
            final ConferenceStateChanged event = new ConferenceStateChanged(name, ConferenceStateChanged.State.COMPLETED);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            observers.clear();
        }
    }

}

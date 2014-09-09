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

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.fsm.TransitionFailedException;
import org.mobicents.servlet.restcomm.fsm.TransitionNotFoundException;
import org.mobicents.servlet.restcomm.fsm.TransitionRollbackException;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.interpreter.StopInterpreter;
import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateConferenceEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.DestroyConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 */
public final class Conference extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite state machine stuff.
    private final State uninitialized;
    // private final State running;
    private final State runningModeratorAbsent;
    private final State runningModeratorPresent;
    private final State completed;
    // Special intermediate states.
    private final State acquiringMediaSession;
    private final State acquiringCnf;
    private final State acquiringConnection;
    private final State initializingConnection;
    private final State openingConnection;
    private final State closingConnection;
    private final State stopping;
    // FSM.
    private final FiniteStateMachine fsm;
    // MGCP runtime stuff.
    private final String name;
    private final ActorRef gateway;
    private MediaSession session;
    private ActorRef cnf;
    private ActorRef connection;

    private ActorRef confVoiceInterpreter;
    // Runtime stuff.
    private final List<ActorRef> calls;
    private final List<ActorRef> observers;

    public Conference(final String name, final ActorRef gateway) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        // running = new State("running", new Running(source), null);

        runningModeratorAbsent = new State("running moderator absent", new RunningModeratorAbsent(source), null);
        runningModeratorPresent = new State("running moderator present", new RunningModeratorPresent(source), null);

        completed = new State("completed", new Completed(source), null);
        acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        acquiringCnf = new State("acquiring cnf", new AcquiringCnf(source), null);
        acquiringConnection = new State("acquiring connection", new AcquiringConnection(source), null);
        initializingConnection = new State("initializing connection", new InitializingConnection(source), null);
        openingConnection = new State("opening connection", new OpeningConnection(source), null);
        closingConnection = new State("closing connection", new ClosingConnection(source), null);
        stopping = new State("stopping", new Stopping(source), null);
        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringMediaSession));
        transitions.add(new Transition(acquiringMediaSession, completed));
        transitions.add(new Transition(acquiringMediaSession, acquiringCnf));
        transitions.add(new Transition(acquiringCnf, completed));
        transitions.add(new Transition(acquiringCnf, acquiringConnection));
        transitions.add(new Transition(acquiringConnection, completed));
        transitions.add(new Transition(acquiringConnection, initializingConnection));
        transitions.add(new Transition(initializingConnection, completed));
        transitions.add(new Transition(initializingConnection, openingConnection));
        transitions.add(new Transition(openingConnection, closingConnection));
        transitions.add(new Transition(openingConnection, completed));
        transitions.add(new Transition(openingConnection, stopping));
        transitions.add(new Transition(closingConnection, stopping));

        transitions.add(new Transition(closingConnection, runningModeratorAbsent));
        transitions.add(new Transition(runningModeratorAbsent, runningModeratorPresent));

        transitions.add(new Transition(runningModeratorPresent, completed));
        transitions.add(new Transition(runningModeratorPresent, runningModeratorAbsent));
        transitions.add(new Transition(runningModeratorAbsent, completed));

        transitions.add(new Transition(stopping, completed));
        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the rest of the conference state.
        this.name = name;
        this.gateway = gateway;
        this.calls = new ArrayList<ActorRef>();
        this.observers = new ArrayList<ActorRef>();
    }

    private void add(final Object message, final ActorRef sender) {
        calls.add(sender);
    }

    private ActorRef getMediaGroup(final Object message) {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MediaGroup(gateway, session, cnf);
            }
        }));
    }

    private void info(final Object message, final ActorRef sender) {
        ConferenceInfo information = null;
        final State state = fsm.state();
        if (runningModeratorAbsent.equals(state)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT);
        } else if (runningModeratorPresent.equals(state)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT);
        } else if (completed.equals(state)) {
            information = new ConferenceInfo(calls, ConferenceStateChanged.State.COMPLETED);
        }
        final ActorRef self = self();
        sender.tell(new ConferenceResponse<ConferenceInfo>(information), self);
    }

    private void invite(final Object message) {
        final AddParticipant request = (AddParticipant) message;
        final Join join = new Join(cnf, ConnectionMode.Confrnce);
        final ActorRef call = request.call();
        final ActorRef self = self();
        call.tell(join, self);
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    // FSM Logic.
    @Override
    public void onReceive(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** Conference Current State: " + state.toString());
            logger.info(" ********** Conference Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (StartConference.class.equals(klass)) {
            fsm.transition(message, acquiringMediaSession);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            if (acquiringMediaSession.equals(state)) {
                fsm.transition(message, acquiringCnf);
            } else if (acquiringCnf.equals(state)) {
                fsm.transition(message, acquiringConnection);
            } else if (acquiringConnection.equals(state)) {
                fsm.transition(message, initializingConnection);
            }
        } else if (ConnectionStateChanged.class.equals(klass)) {
            final ConnectionStateChanged response = (ConnectionStateChanged) message;
            if (initializingConnection.equals(state)) {
                fsm.transition(message, openingConnection);
            } else if (openingConnection.equals(state)) {
                if (ConnectionStateChanged.State.HALF_OPEN == response.state()) {
                    fsm.transition(message, closingConnection);
                } else if (ConnectionStateChanged.State.CLOSED == response.state()) {
                    fsm.transition(message, completed);
                }
            } else if (closingConnection.equals(state)) {

                fsm.transition(message, runningModeratorAbsent);

            } else if (stopping.equals(state)) {
                fsm.transition(message, completed);
            }
        } else if (StopConference.class.equals(klass)) {
            if (openingConnection.equals(state) || closingConnection.equals(state)) {
                fsm.transition(message, stopping);
            } else {
                fsm.transition(message, completed);
            }
        } else if (runningModeratorAbsent.equals(state) || runningModeratorPresent.equals(state)) {
            if (CreateWaitUrlConfMediaGroup.class.equals(klass)) {
                handleCreateWaitUrlConfMediaGroup(message);
            } else if (CreateMediaGroup.class.equals(klass)) {
                final ActorRef group = getMediaGroup(message);
                sender.tell(new ConferenceResponse<ActorRef>(group), sender);
            } else if (DestroyMediaGroup.class.equals(klass)) {
                final DestroyMediaGroup request = (DestroyMediaGroup) message;
                context.stop(request.group());
            } else if (DestroyWaitUrlConfMediaGroup.class.equals(klass)) {
                final DestroyWaitUrlConfMediaGroup request = (DestroyWaitUrlConfMediaGroup) message;
                ActorRef waitUrlMediaGroup = request.getWaitUrlConfMediaGroup();
                if(waitUrlMediaGroup != null && !waitUrlMediaGroup.isTerminated())
                    context.stop(waitUrlMediaGroup);

                // ConferenceVoiceInterpreter is dead now. Set it to null
                this.confVoiceInterpreter = null;
            } else if (AddParticipant.class.equals(klass)) {
                invite(message);
            } else if (JoinComplete.class.equals(klass)) {
                add(message, sender);
            } else if (RemoveParticipant.class.equals(klass)) {
                remove(message);
            } else if (GetConferenceInfo.class.equals(klass)) {
                info(message, sender);
            } else if (ConferenceModeratorPresent.class.equals(klass)) {
                if (runningModeratorAbsent.equals(state)) {
                    fsm.transition(message, runningModeratorPresent);
                }
            }
        }
    }

    private void handleCreateWaitUrlConfMediaGroup(final Object message) {
        CreateWaitUrlConfMediaGroup createWaitUrlConfMediaGroup = (CreateWaitUrlConfMediaGroup) message;
        ActorRef confVoiceInterpreterTmp = createWaitUrlConfMediaGroup.getConfVoiceInterpreter();
        if (confVoiceInterpreter == null) {
            this.confVoiceInterpreter = confVoiceInterpreterTmp;

            StartInterpreter startInterpreter = new StartInterpreter(getSelf());
            this.confVoiceInterpreter.tell(startInterpreter, getSelf());

        } else {
            final ActorRef self = self();
            StopInterpreter stopInterpreter = StopInterpreter.instance();
            confVoiceInterpreterTmp.tell(stopInterpreter, self);
        }
    }

    private void remove(final Object message) throws TransitionFailedException, TransitionNotFoundException,
            TransitionRollbackException {
        final RemoveParticipant request = (RemoveParticipant) message;
        final ActorRef call = request.call();
        final ActorRef self = self();
        if (calls.remove(call)) {
            final Leave leave = new Leave();
            call.tell(leave, self);
        }

        if (calls.size() == 0) {
            // If no more participants and back ground music was on, we should stop it now.
            logger.info("calls size is zero in conference " + name);
            this.stopAndCleanConfVoiceInter(self);
            if (runningModeratorPresent.equals(fsm.state())) {
                fsm.transition(message, runningModeratorAbsent);
            }
        }
    }

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringMediaSession extends AbstractAction {
        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            gateway.tell(new CreateMediaSession(), source);
        }
    }

    private final class AcquiringCnf extends AbstractAction {
        public AcquiringCnf(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<MediaSession> response = (MediaGatewayResponse<MediaSession>) message;
            session = response.get();
            gateway.tell(new CreateConferenceEndpoint(session), source);
        }
    }

    private final class AcquiringConnection extends AbstractAction {
        public AcquiringConnection(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            cnf = response.get();
            gateway.tell(new CreateConnection(session), source);
        }
    }

    private final class InitializingConnection extends AbstractAction {
        public InitializingConnection(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            connection = response.get();
            connection.tell(new Observe(source), source);
            connection.tell(new InitializeConnection(cnf), source);
        }
    }

    private final class OpeningConnection extends AbstractAction {
        public OpeningConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            connection.tell(new OpenConnection(ConnectionMode.SendRecv), source);
        }
    }

    private final class ClosingConnection extends AbstractAction {
        public ClosingConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            connection.tell(new CloseConnection(), source);
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

    private final class Completed extends AbstractAction {
        public Completed(final ActorRef source) {
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

    private void stopAndCleanConfVoiceInter(final ActorRef source) {
        if (this.confVoiceInterpreter != null) {
            StopInterpreter stopInterpreter = StopInterpreter.instance();
            this.confVoiceInterpreter.tell(stopInterpreter, source);
            this.confVoiceInterpreter = null;
        }
    }
}

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
package org.mobicents.servlet.restcomm.mgcp;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;

import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Link extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Finite state machine stuff.
    private final State uninitialized;
    private final State closed;
    private final State open;
    // Special intermediate states to indicate we are waiting for a response from the media gateway
    // before completing a move in to a different state.
    private final State initializingPrimary;
    private final State initializingSecondary;
    private final State closingPrimary;
    private final State closingSecondary;
    private final State opening;
    // Special intermediate state used when waiting for a response to a modify connection request.
    private final State modifying;
    private final FiniteStateMachine fsm;
    // Runtime Stuff.
    private final ActorRef gateway;
    private final MediaSession session;
    private final NotifiedEntity agent;
    private final long timeout;
    private final List<ActorRef> observers;
    private ActorRef primaryEndpoint;
    private ActorRef secondaryEndpoint;
    private EndpointIdentifier primaryEndpointId;
    private EndpointIdentifier secondaryEndpointId;
    private ConnectionIdentifier primaryConnId;
    private ConnectionIdentifier secondaryConnId;

    public Link(final ActorRef gateway, final MediaSession session, final NotifiedEntity agent, final long timeout) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        closed = new State("closed", new ClosedAction(source), null);
        open = new State("open", new OpenAction(source), null);
        initializingPrimary = new State("initializing primary", new InitializingPrimary(source), null);
        initializingSecondary = new State("initializing secondary", new EnteringInitializingSecondary(source),
                new ExitingInitializingSecondary(source));
        closingPrimary = new State("closing primary", new ClosingPrimary(source), null);
        closingSecondary = new State("closing secondary", new ClosingSecondary(source), null);
        opening = new State("opening", new Opening(source), null);
        modifying = new State("modifying", new Modifying(source), null);
        // Initialize the main transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializingPrimary));
        transitions.add(new Transition(uninitialized, closed));
        transitions.add(new Transition(closed, opening));
        transitions.add(new Transition(open, closingPrimary));
        transitions.add(new Transition(open, modifying));
        transitions.add(new Transition(open, closed));
        // Initialize the intermediate transitions for the FSM.
        transitions.add(new Transition(initializingPrimary, initializingSecondary));
        transitions.add(new Transition(initializingSecondary, closed));
        transitions.add(new Transition(closingPrimary, closingSecondary));
        transitions.add(new Transition(closingSecondary, closed));
        transitions.add(new Transition(opening, closed));
        transitions.add(new Transition(opening, open));
        transitions.add(new Transition(modifying, open));
        transitions.add(new Transition(modifying, closingPrimary));
        // Initialize transitions needed in case the media gateway
        // goes away.
        transitions.add(new Transition(closingPrimary, closed));
        transitions.add(new Transition(modifying, closed));
        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the rest of the connection state.
        this.gateway = gateway;
        this.session = session;
        this.agent = agent;
        this.timeout = timeout;
        this.observers = new ArrayList<ActorRef>();
        this.primaryConnId = null;
        this.secondaryConnId = null;
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

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();
        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (InitializeLink.class.equals(klass)) {
            fsm.transition(message, initializingPrimary);
        } else if (EndpointCredentials.class.equals(klass)) {
            if (initializingPrimary.equals(state)) {
                fsm.transition(message, initializingSecondary);
            } else if (initializingSecondary.equals(state)) {
                fsm.transition(message, closed);
            }
        } else if (OpenLink.class.equals(klass)) {
            fsm.transition(message, opening);
        } else if (UpdateLink.class.equals(klass)) {
            fsm.transition(message, modifying);
        } else if (CloseLink.class.equals(klass)) {
            if (!closingPrimary.equals(state))
                fsm.transition(message, closingPrimary);
        } else if (message instanceof JainMgcpResponseEvent) {
            final JainMgcpResponseEvent response = (JainMgcpResponseEvent) message;
            final int code = response.getReturnCode().getValue();
            if (code == ReturnCode.TRANSACTION_BEING_EXECUTED) {
                return;
            } else if (code == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
                if (opening.equals(state)) {
                    fsm.transition(message, open);
                } else if (closingPrimary.equals(state)) {
                    fsm.transition(message, closingSecondary);
                } else if (closingSecondary.equals(state)) {
                    fsm.transition(message, closed);
                } else if (modifying.equals(state)) {
                    fsm.transition(message, open);
                }
            } else {
                if (modifying.equals(state)) {
                    fsm.transition(message, closingPrimary);
                } else {
                    fsm.transition(message, closed);
                }
            }
        } else if (message instanceof ReceiveTimeout) {
            fsm.transition(message, closed);
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

        protected void log(final LinkStateChanged.State state) {
            final StringBuilder buffer = new StringBuilder();
            // Start printing on a new line.
            buffer.append("\n");
            // Log the message.
            switch (state) {
                case CLOSED: {
                    buffer.append("Closed a link");
                }
                case OPEN: {
                    buffer.append("Opened a link");
                }
            }
            if (primaryConnId != null && primaryEndpointId != null && secondaryConnId != null && secondaryEndpointId != null) {
                buffer.append(" with primary connection ID of ").append(primaryConnId.toString());
                buffer.append(" secondary connection ID of ").append(secondaryConnId.toString());
                buffer.append(" A primary endpoint ID of ").append(primaryEndpointId.toString());
                buffer.append(" and a secondary endpoint ID of ").append(secondaryEndpointId.toString());
            }
            logger.debug(buffer.toString());
        }
    }

    private final class ClosedAction extends AbstractAction {
        public ClosedAction(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            /* Stop the timer. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());
            // Notify the observers.
            final LinkStateChanged event = new LinkStateChanged(LinkStateChanged.State.CLOSED);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            // Log the state change.
            log(LinkStateChanged.State.CLOSED);
            // If we timed out log it.
            if (message instanceof ReceiveTimeout) {
                logger.error("The media gateway failed to respond in the requested timout period.");
            }
        }
    }

    private final class OpenAction extends AbstractAction {
        public OpenAction(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            /* Stop the timer. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());
            /* Configure the connection and end points. */
            final Class<?> klass = message.getClass();
            if (CreateConnectionResponse.class.equals(klass)) {
                final CreateConnectionResponse response = (CreateConnectionResponse) message;
                if (primaryConnId == null) {
                    primaryConnId = response.getConnectionIdentifier();
                }
                if (primaryEndpointId.getLocalEndpointName().endsWith("$")) {
                    primaryEndpointId = response.getSpecificEndpointIdentifier();
                    primaryEndpoint.tell(new UpdateEndpointId(primaryEndpointId), source);
                }
                if (secondaryConnId == null) {
                    secondaryConnId = response.getSecondConnectionIdentifier();
                }
                if (secondaryEndpointId.getLocalEndpointName().endsWith("$")) {
                    secondaryEndpointId = response.getSecondEndpointIdentifier();
                    secondaryEndpoint.tell(new UpdateEndpointId(secondaryEndpointId), source);
                }
            }
            final LinkStateChanged event = new LinkStateChanged(LinkStateChanged.State.OPEN);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            // Log the state change.
            log(LinkStateChanged.State.CLOSED);
        }
    }

    private final class InitializingPrimary extends AbstractAction {
        public InitializingPrimary(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final InitializeLink request = (InitializeLink) message;
            primaryEndpoint = request.primaryEndpoint();
            secondaryEndpoint = request.secondaryEndpoint();
            if (primaryEndpoint != null) {
                primaryEndpoint.tell(new InviteEndpoint(), source);
            }
        }
    }

    private final class EnteringInitializingSecondary extends AbstractAction {
        public EnteringInitializingSecondary(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final EndpointCredentials response = (EndpointCredentials) message;
            primaryEndpointId = response.endpointId();
            if (secondaryEndpoint != null) {
                secondaryEndpoint.tell(new InviteEndpoint(), source);
            }
        }
    }

    private final class ExitingInitializingSecondary extends AbstractAction {
        public ExitingInitializingSecondary(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final EndpointCredentials response = (EndpointCredentials) message;
            secondaryEndpointId = response.endpointId();
        }
    }

    private final class ClosingPrimary extends AbstractAction {
        public ClosingPrimary(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final DeleteConnection dlcx = new DeleteConnection(source, callId, primaryEndpointId, primaryConnId);
            gateway.tell(dlcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    private final class ClosingSecondary extends AbstractAction {
        public ClosingSecondary(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            /* Stop the timer here. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());

            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final DeleteConnection dlcx = new DeleteConnection(source, callId, secondaryEndpointId, secondaryConnId);
            gateway.tell(dlcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    private final class Modifying extends AbstractAction {
        public Modifying(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateLink request = (UpdateLink) message;
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            ModifyConnection mdcx = null;
            switch (request.type()) {
                case PRIMARY: {
                    mdcx = new ModifyConnection(source, callId, primaryEndpointId, primaryConnId);
                }
                case SECONDARY: {
                    mdcx = new ModifyConnection(source, callId, secondaryEndpointId, secondaryConnId);
                }
            }
            final ConnectionMode mode = request.mode();
            if (mode != null) {
                mdcx.setMode(mode);
            }
            gateway.tell(mdcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    private final class Opening extends AbstractAction {
        public Opening(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final OpenLink request = (OpenLink) message;
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final CreateConnection crcx = new CreateConnection(source, callId, primaryEndpointId, request.mode());
            crcx.setNotifiedEntity(agent);
            crcx.setSecondEndpointIdentifier(secondaryEndpointId);
            gateway.tell(crcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }
}

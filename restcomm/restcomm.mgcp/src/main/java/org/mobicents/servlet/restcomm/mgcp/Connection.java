/*
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
package org.mobicents.servlet.restcomm.mgcp;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
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
public final class Connection extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Finite state machine stuff.
    private final State uninitialized;
    private final State closed;
    private final State halfOpen;
    private final State open;
    // Special intermediate states to indicate we are waiting for a response from the media gateway
    // before completing a move in to a different state.
    private final State initializing;
    private final State closing;
    private final State openingHalfWay;
    private final State opening;
    // Special intermediate state used when waiting for a response to a modify connection request.
    private final State modifying;
    // FSM.
    private final FiniteStateMachine fsm;
    // Runtime stuff.
    private final ActorRef gateway;
    private final MediaSession session;
    private final NotifiedEntity agent;
    private final long timeout;
    private final List<ActorRef> observers;
    private ActorRef endpoint;
    private EndpointIdentifier endpointId;
    private ConnectionIdentifier connId;
    private ConnectionDescriptor localDesc;
    private ConnectionDescriptor remoteDesc;

    public Connection(final ActorRef gateway, final MediaSession session, final NotifiedEntity agent, final long timeout) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        closed = new State("closed", new Closed(source), null);
        halfOpen = new State("half open", new HalfOpen(source), null);
        open = new State("open", new Open(source), null);
        initializing = new State("initializing", new EnteringInitialization(source), new ExitingInitialization(source));
        closing = new State("closing", new Closing(source), null);
        openingHalfWay = new State("opening halfway", new OpeningHalfWay(source), null);
        opening = new State("opening", new Opening(source), null);
        modifying = new State("modifying", new Modifying(source), null);
        // Initialize the main transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializing));
        transitions.add(new Transition(uninitialized, closed));
        transitions.add(new Transition(closed, openingHalfWay));
        transitions.add(new Transition(closed, opening));
        transitions.add(new Transition(halfOpen, closing));
        transitions.add(new Transition(halfOpen, modifying));
        transitions.add(new Transition(open, closing));
        transitions.add(new Transition(open, closed));
        transitions.add(new Transition(open, modifying));
        // Initialize the intermediate transitions for the FSM.
        transitions.add(new Transition(initializing, closed));
        transitions.add(new Transition(closing, closed));
        transitions.add(new Transition(openingHalfWay, closed));
        transitions.add(new Transition(openingHalfWay, halfOpen));
        transitions.add(new Transition(opening, closed));
        transitions.add(new Transition(opening, open));
        transitions.add(new Transition(modifying, open));
        transitions.add(new Transition(modifying, closing));
        // Initialize transitions needed in case the media gateway
        // goes away.
        transitions.add(new Transition(modifying, closed));
        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the rest of the connection state.
        this.gateway = gateway;
        this.session = session;
        this.agent = agent;
        this.timeout = timeout;
        this.observers = new ArrayList<ActorRef>();
        this.connId = null;
        this.localDesc = null;
        this.remoteDesc = null;
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

    // FSM logic.
    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** Connection Current State: " + state.toString());
            logger.info(" ********** Connection Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (InitializeConnection.class.equals(klass)) {
            fsm.transition(message, initializing);
        } else if (EndpointCredentials.class.equals(klass)) {
            fsm.transition(message, closed);
        } else if (OpenConnection.class.equals(klass)) {
            final OpenConnection request = (OpenConnection) message;
            if (request.descriptor() == null) {
                fsm.transition(message, openingHalfWay);
            } else {
                fsm.transition(message, opening);
            }
        } else if (UpdateConnection.class.equals(klass)) {
            fsm.transition(message, modifying);
        } else if (CloseConnection.class.equals(klass)) {
            fsm.transition(message, closing);
        } else if (InspectConnection.class.equals(klass)) {
            auditConnection(message, sender());
        } else if (message instanceof JainMgcpResponseEvent) {
            final JainMgcpResponseEvent response = (JainMgcpResponseEvent) message;
            final int code = response.getReturnCode().getValue();
            if (code == ReturnCode.TRANSACTION_BEING_EXECUTED) {
                return;
            } else if (code == ReturnCode.TRANSACTION_EXECUTED_NORMALLY) {
                if (openingHalfWay.equals(state)) {
                    fsm.transition(message, halfOpen);
                } else if (opening.equals(state)) {
                    fsm.transition(message, open);
                } else if (closing.equals(state)) {
                    fsm.transition(message, closed);
                } else if (modifying.equals(state)) {
                    fsm.transition(message, open);
                }
            } else {
                if (modifying.equals(state)) {
                    fsm.transition(message, closing);
                } else {
                    fsm.transition(message, closed);
                }
            }
        } else if (message instanceof ReceiveTimeout) {
            fsm.transition(message, closed);
        }
    }

    private void auditConnection(Object message, ActorRef source) {
        InfoCode[] requestedInfo = new InfoCode[] { InfoCode.ConnectionMode };
        AuditConnection aucx = new AuditConnection(source, endpointId, connId, requestedInfo);
        gateway.tell(aucx, source);
        // XXX Temporary workaround because mms blocks during DTLS handshake - hrosa
//        getContext().setReceiveTimeout(Duration.create(0, TimeUnit.MILLISECONDS));
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

        protected void log(final ConnectionStateChanged.State state) {
            final StringBuilder buffer = new StringBuilder();
            // Start printing on a new line.
            buffer.append("\n");
            // Log the message.
            switch (state) {
                case CLOSED: {
                    buffer.append("Closed a connection");
                }
                case HALF_OPEN: {
                    buffer.append("Opened a connection halfway");
                }
                case OPEN: {
                    buffer.append("Opened a connection");
                }
            }
            if (connId != null && endpointId != null) {
                buffer.append(" with ID ").append(connId.toString()).append(" ");
                buffer.append("to an endpoint with ID ").append(endpointId.toString());
            }
            logger.debug(buffer.toString());
        }
    }

    private final class Closed extends AbstractAction {
        public Closed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            /* Stop the timer. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());
            // Notify the observers.
            final ConnectionStateChanged event = new ConnectionStateChanged(ConnectionStateChanged.State.CLOSED);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            // Log the state change.
            log(ConnectionStateChanged.State.CLOSED);
            // If we timed out log it.
            if (message instanceof ReceiveTimeout) {
                logger.error("The media gateway failed to respond in the requested timout period.");
            }
        }
    }

    private abstract class AbstractOpenAction extends AbstractAction {
        public AbstractOpenAction(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final CreateConnectionResponse response = (CreateConnectionResponse) message;
            if (connId == null) {
                connId = response.getConnectionIdentifier();
            }
            localDesc = response.getLocalConnectionDescriptor();
            // If the end point ends with a wild card we should update it.
            if (endpointId.getLocalEndpointName().endsWith("$")) {
                endpointId = response.getSpecificEndpointIdentifier();
                endpoint.tell(new UpdateEndpointId(endpointId), source);
            }
        }
    }

    private final class HalfOpen extends AbstractOpenAction {
        public HalfOpen(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            super.execute(message);
            /* Stop the timer. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());
            // Notify the observers.
            final ConnectionStateChanged event = new ConnectionStateChanged(localDesc, ConnectionStateChanged.State.HALF_OPEN);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            // Log the state change.
            log(ConnectionStateChanged.State.HALF_OPEN);
        }
    }

    private final class Open extends AbstractOpenAction {
        public Open(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            /* Stop the timer. */
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.Undefined());
            // Handle results from opening or modifying states.
            final Class<?> klass = message.getClass();
            if (CreateConnectionResponse.class.equals(klass)) {
                super.execute(message);
            } else if (ModifyConnectionResponse.class.equals(klass)) {
                final ModifyConnectionResponse response = (ModifyConnectionResponse) message;
                localDesc = response.getLocalConnectionDescriptor();
            }
            final ConnectionStateChanged event = new ConnectionStateChanged(localDesc, ConnectionStateChanged.State.OPEN);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            // Log the state change.
            log(ConnectionStateChanged.State.OPEN);
        }
    }

    private final class EnteringInitialization extends AbstractAction {
        public EnteringInitialization(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final InitializeConnection request = (InitializeConnection) message;
            endpoint = request.endpoint();
            if (endpoint != null) {
                final InviteEndpoint invite = new InviteEndpoint();
                endpoint.tell(invite, source);
            }
        }
    }

    private final class ExitingInitialization extends AbstractAction {
        public ExitingInitialization(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final EndpointCredentials response = (EndpointCredentials) message;
            endpointId = response.endpointId();
        }
    }

    private final class Closing extends AbstractAction {
        public Closing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final DeleteConnection dlcx = new DeleteConnection(source, callId, endpointId, connId);
            gateway.tell(dlcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    private abstract class AbstractOpeningAction extends AbstractAction {
        public AbstractOpeningAction(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final OpenConnection request = (OpenConnection) message;
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final CreateConnection crcx = new CreateConnection(source, callId, endpointId, request.mode());
            remoteDesc = request.descriptor();
            if (remoteDesc != null) {
                crcx.setRemoteConnectionDescriptor(remoteDesc);
            }
            crcx.setNotifiedEntity(agent);
            gateway.tell(crcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }

    private final class OpeningHalfWay extends AbstractOpeningAction {
        public OpeningHalfWay(final ActorRef source) {
            super(source);
        }
    }

    private final class Opening extends AbstractOpeningAction {
        public Opening(final ActorRef source) {
            super(source);
        }
    }

    private final class Modifying extends AbstractAction {
        public Modifying(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateConnection request = (UpdateConnection) message;
            final String sessionId = Integer.toString(session.id());
            final CallIdentifier callId = new CallIdentifier(sessionId);
            final ModifyConnection mdcx = new ModifyConnection(source, callId, endpointId, connId);
            final ConnectionMode mode = request.mode();
            if (mode != null) {
                mdcx.setMode(mode);
            }
            final ConnectionDescriptor descriptor = request.descriptor();
            if (descriptor != null) {
                mdcx.setRemoteConnectionDescriptor(descriptor);
            }
            gateway.tell(mdcx, source);
            // Make sure we don't wait for a response indefinitely.
            getContext().setReceiveTimeout(Duration.create(timeout, TimeUnit.MILLISECONDS));
        }
    }
}

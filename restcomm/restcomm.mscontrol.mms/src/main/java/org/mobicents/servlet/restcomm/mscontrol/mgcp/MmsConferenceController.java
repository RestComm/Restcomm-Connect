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

package org.mobicents.servlet.restcomm.mscontrol.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.HashSet;
import java.util.Set;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateConferenceEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController;
import org.mobicents.servlet.restcomm.mscontrol.messages.CloseMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionControllerResponse;
import org.mobicents.servlet.restcomm.patterns.Observe;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
@Immutable
public final class MmsConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;

    // Finite states
    private final State uninitialized;
    private final State active;
    private final State inactive;

    // Intermediate states
    private final State acquiringMediaSession;
    private final State acquiringConferenceEndpoint;
    private final State acquiringConnection;
    private final State initializingConnection;
    private final State openingConnection;
    private final State closingConnection;

    // MGCP runtime stuff.
    private final ActorRef mediaGateway;
    private MediaSession mediaSession;
    private ActorRef cnfEndpoint;
    private ActorRef connection;

    public MmsConferenceController(ActorRef mediaGateway) {
        super();
        final ActorRef source = self();

        // Finite States
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);

        // Intermediate states
        this.acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        this.acquiringConferenceEndpoint = new State("acquiring conferenceEndpoint", new AcquiringCnf(source), null);
        this.acquiringConnection = new State("acquiring connection", new AcquiringConnection(source), null);
        this.initializingConnection = new State("initializing connection", new InitializingConnection(source), null);
        this.openingConnection = new State("opening connection", new OpeningConnection(source), null);
        this.closingConnection = new State("closing connection", new ClosingConnection(source), null);

        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringMediaSession));
        transitions.add(new Transition(acquiringMediaSession, acquiringConferenceEndpoint));
        transitions.add(new Transition(acquiringMediaSession, inactive));
        transitions.add(new Transition(acquiringConferenceEndpoint, acquiringConnection));
        transitions.add(new Transition(acquiringConferenceEndpoint, inactive));
        transitions.add(new Transition(acquiringConnection, initializingConnection));
        transitions.add(new Transition(acquiringConnection, inactive));
        transitions.add(new Transition(initializingConnection, openingConnection));
        transitions.add(new Transition(initializingConnection, inactive));
        transitions.add(new Transition(openingConnection, closingConnection));
        transitions.add(new Transition(openingConnection, inactive));
        transitions.add(new Transition(closingConnection, inactive));
        transitions.add(new Transition(closingConnection, active));

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // MGCP runtime stuff
        this.mediaGateway = mediaGateway;
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private ActorRef createMediaGroup(final Object message) {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MgcpMediaGroup(mediaGateway, mediaSession, cnfEndpoint);
            }
        }));
    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        final ActorRef self = self();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** Conference Controller Current State: " + state.toString());
            logger.info(" ********** Conference Controller Processing Message: " + klass.getName());
        }

        if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (CloseMediaSession.class.equals(klass)) {
            onCloseMediaSession((CloseMediaSession) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (ConnectionStateChanged.class.equals(klass)) {
            onConnectionStateChanged((ConnectionStateChanged) message, self, sender);
        } else if (CreateMediaGroup.class.equals(klass)) {
            onCreateMediaGroup((CreateMediaGroup) message, self, sender);
        } else if (DestroyMediaGroup.class.equals(klass)) {
            onDestroyMediaGroup((DestroyMediaGroup) message, self, sender);
        } else if (org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection.class.equals(klass)) {
            onCloseConnection((org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection) message, self, sender);
        }
    }

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, acquiringMediaSession);
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, closingConnection);
        } else {
            fsm.transition(message, inactive);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        if (is(acquiringMediaSession)) {
            fsm.transition(message, acquiringConferenceEndpoint);
        } else if (is(acquiringConferenceEndpoint)) {
            fsm.transition(message, acquiringConnection);
        } else if (is(acquiringConnection)) {
            fsm.transition(message, initializingConnection);
        }
    }

    private void onConnectionStateChanged(ConnectionStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        ConnectionStateChanged.State connState = message.state();
        switch (connState) {
            case HALF_OPEN:
                if (is(openingConnection)) {
                    fsm.transition(message, closingConnection);
                }
                break;

            case OPEN:
                if (is(closingConnection)) {
                    fsm.transition(message, active);
                }
                break;

            case CLOSED:
                if (is(initializingConnection)) {
                    fsm.transition(message, openingConnection);
                } else if (is(openingConnection) || is(closingConnection)) {
                    fsm.transition(message, inactive);
                }
                break;

            default:
                logger.warning("Received unknown connection state event!");
                break;
        }
    }

    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef group = createMediaGroup(message);
        sender.tell(new MediaSessionControllerResponse<ActorRef>(group), sender);
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        getContext().stop(message.group());
    }

    private void onCloseConnection(org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection message, ActorRef self,
            ActorRef sender) {
        mediaGateway.tell(new DestroyConnection(connection), self);
        connection = null;
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

    private final class AcquiringMediaSession extends AbstractAction {
        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateMediaSession(), source);
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
            mediaSession = response.get();
            mediaGateway.tell(new CreateConferenceEndpoint(mediaSession), source);
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
            cnfEndpoint = response.get();
            mediaGateway.tell(new CreateConnection(mediaSession), source);
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
            connection.tell(new InitializeConnection(cnfEndpoint), source);
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

    private final class Inactive extends AbstractAction {
        public Inactive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Clean up resources
            final ActorRef self = self();
            if (connection != null) {
                mediaGateway.tell(new DestroyConnection(connection), self);
                connection = null;
            }
            if (cnfEndpoint != null) {
                mediaGateway.tell(new DestroyEndpoint(cnfEndpoint), self);
                cnfEndpoint = null;
            }
        }
    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new DestroyConnection(connection), source);
            connection = null;
        }
    }

}

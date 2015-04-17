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
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupCreated;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupDestroyed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionClosed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionInfo;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.messages.EndpointInfo;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.messages.QueryEndpoint;
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
    private final State creatingMediaGroup;
    private final State stopping;

    // MGCP runtime stuff.
    private final ActorRef mediaGateway;
    private MediaSession mediaSession;
    private ActorRef cnfEndpoint;
    private ActorRef connection;

    // Conference runtime stuff
    private ActorRef conference;
    private ActorRef mediaGroup;

    // Runtime media operations
    private Boolean playing;

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
        this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);

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
        transitions.add(new Transition(openingConnection, stopping));
        transitions.add(new Transition(closingConnection, stopping));
        transitions.add(new Transition(closingConnection, active));
        transitions.add(new Transition(active, inactive));
        transitions.add(new Transition(active, creatingMediaGroup));
        transitions.add(new Transition(creatingMediaGroup, active));
        transitions.add(new Transition(stopping, inactive));

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // MGCP runtime stuff
        this.mediaGateway = mediaGateway;

        // Runtime media operations
        this.playing = Boolean.FALSE;
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

        logger.info(" ********** Conference Controller Current State: " + state.toString());
        logger.info(" ********** Conference Controller Processing Message: " + klass.getName());

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
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            onMediaGroupStateChanged((MediaGroupStateChanged) message, self, sender);
        } else if (org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection.class.equals(klass)) {
            onCloseConnection((org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection) message, self, sender);
        } else if (QueryEndpoint.class.equals(klass)) {
            onQueryEndpoint((QueryEndpoint) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        }
    }

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        this.conference = sender;
        fsm.transition(message, acquiringMediaSession);
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, inactive);
        } else {
            fsm.transition(message, stopping);
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
        // switch (connState) {
        // case HALF_OPEN:
        // if (is(openingConnection)) {
        // fsm.transition(message, closingConnection);
        // }
        // break;
        //
        // case OPEN:
        // if (is(closingConnection)) {
        // fsm.transition(message, active);
        // }
        // break;
        //
        // case CLOSED:
        // if (is(initializingConnection)) {
        // fsm.transition(message, openingConnection);
        // } else if (is(openingConnection) || is(closingConnection)) {
        // fsm.transition(message, inactive);
        // }
        // break;
        //
        // default:
        // logger.warning("Received unknown connection state event!");
        // break;
        // }
        if (is(initializingConnection)) {
            fsm.transition(message, openingConnection);
        } else if (is(openingConnection)) {
            if (ConnectionStateChanged.State.HALF_OPEN == connState) {
                fsm.transition(message, closingConnection);
            } else if (ConnectionStateChanged.State.CLOSED == connState) {
                fsm.transition(message, inactive);
            }
        } else if (is(closingConnection)) {
            fsm.transition(message, active);
        } else if (is(stopping)) {
            fsm.transition(message, inactive);
        }
    }

    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, creatingMediaGroup);
        }
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) && this.mediaGroup != null) {
            this.mediaGroup.tell(new StopMediaGroup(), self);
            this.mediaGroup = null;
        }

        final MediaGroupDestroyed mgDestroyed = new MediaGroupDestroyed();
        sender.tell(new MediaServerControllerResponse<MediaGroupDestroyed>(mgDestroyed), self);
    }

    private void onMediaGroupStateChanged(MediaGroupStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if (is(creatingMediaGroup)) {
            fsm.transition(message, active);
        }
    }

    private void onCloseConnection(org.mobicents.servlet.restcomm.mscontrol.messages.CloseConnection message, ActorRef self,
            ActorRef sender) {
        mediaGateway.tell(new DestroyConnection(connection), self);
        connection = null;
    }

    private void onQueryEndpoint(QueryEndpoint message, ActorRef self, ActorRef sender) {
        final EndpointInfo endpointInfo = new EndpointInfo(cnfEndpoint, ConnectionMode.Confrnce);
        sender.tell(new MediaServerControllerResponse<EndpointInfo>(endpointInfo), self);
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            if (message.isBackground()) {
                // if (this.ephemeralMediaGroup == null) {
                // logger.info("%%%%%%%%%%%%%% Creating ephemeral media group");
                // this.ephemeralMediaGroup = this.mediaSession.createMediaGroup(MediaGroup.PLAYER);
                //
                // logger.info("%%%%%%%%%%%%%% Starting ephemeral media group");
                // this.ephemeralMediaGroup.join(Direction.DUPLEX, this.mediaMixer);
                //
                // logger.info("%%%%%%%%%%%%%% Playing background music");
                // List<URI> uris = message.uris();
                // Parameters params = this.ephemeralMediaGroup.createParameters();
                // int repeatCount = message.iterations() <= 0 ? Player.FOREVER : message.iterations() - 1;
                // params.put(Player.REPEAT_COUNT, repeatCount);
                // this.ephemeralMediaGroup.getPlayer().play(uris.toArray(new URI[uris.size()]), RTC.NO_RTC, params);
                // this.playingBackground = Boolean.TRUE;
                // }
            } else {
                logger.info("%%%%%%%%%%%%%% Playing beep [already playing? " + this.playing + "]");
                this.mediaGroup.tell(message, sender);
                this.playing = Boolean.TRUE;
            }
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

    private final class AcquiringMediaSession extends AbstractAction {
        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateMediaSession(), super.source);
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
            mediaGateway.tell(new CreateConferenceEndpoint(mediaSession), super.source);
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
            mediaGateway.tell(new CreateConnection(mediaSession), super.source);
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
            connection.tell(new Observe(super.source), super.source);
            connection.tell(new InitializeConnection(cnfEndpoint), super.source);
        }
    }

    private final class OpeningConnection extends AbstractAction {
        public OpeningConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            connection.tell(new OpenConnection(ConnectionMode.SendRecv), super.source);
        }
    }

    private final class ClosingConnection extends AbstractAction {

        public ClosingConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            connection.tell(new CloseConnection(), super.source);
        }
    }

    private final class CreatingMediaGroup extends AbstractAction {

        public CreatingMediaGroup(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            mediaGroup = createMediaGroup(message);
            mediaGroup.tell(new Observe(super.source), super.source);
            mediaGroup.tell(new StartMediaGroup(), super.source);
        }

    }

    private final class Stopping extends AbstractAction {

        public Stopping(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            if (is(openingConnection)) {
                connection.tell(new CloseConnection(), super.source);
            }
        }
    }

    private final class Inactive extends AbstractAction {

        public Inactive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (connection != null) {
                mediaGateway.tell(new DestroyConnection(connection), super.source);
                connection = null;
            }
            if (cnfEndpoint != null) {
                mediaGateway.tell(new DestroyEndpoint(cnfEndpoint), super.source);
                cnfEndpoint = null;
            }

            // Inform conference that media session has been properly closed
            final MediaSessionClosed response = new MediaSessionClosed();
            conference.tell(new MediaServerControllerResponse<MediaSessionClosed>(response), super.source);
        }
    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            Class<?> klass = message.getClass();
            if (MediaGroupStateChanged.class.equals(klass)) {
                MediaGroupStateChanged.State mgState = ((MediaGroupStateChanged) message).state();
                if (MediaGroupStateChanged.State.ACTIVE.equals(mgState)) {
                    final MediaGroupCreated mgCreated = new MediaGroupCreated();
                    conference.tell(new MediaServerControllerResponse<MediaGroupCreated>(mgCreated), super.source);
                } else if (MediaGroupStateChanged.State.INACTIVE.equals(mgState)) {
                    conference.tell(new MediaServerControllerError(), super.source);
                }
            } else {
                conference.tell(new MediaServerControllerResponse<MediaSessionInfo>(new MediaSessionInfo()), super.source);
            }
        }
    }

}

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
package org.mobicents.servlet.restcomm.mgcp.mscontrol;

import java.util.HashSet;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.fsm.TransitionFailedException;
import org.mobicents.servlet.restcomm.fsm.TransitionNotFoundException;
import org.mobicents.servlet.restcomm.fsm.TransitionRollbackException;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mscontrol.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.MediaSessionController;

import akka.actor.ActorRef;
import akka.actor.UntypedActorContext;

/**
 * Specification of {@link MediaSessionController} that relies on MGCP to manage media sessions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see MediaSessionController
 */
public class MgcpMediaSessionController extends MediaSessionController {

    // FSM.
    private final FiniteStateMachine fsm;

    // States for the FSM
    private final State uninitialized;
    private final State ready;

    // Intermediate States
    private final State acquiringMediaGatewayInfo;
    private final State acquiringMediaSession;
    private final State acquiringBridge;
    private final State acquiringRemoteConnection;
    private final State initializingRemoteConnection;
    private final State openingRemoteConnection;

    // Call runtime
    private ActorRef call;

    // MGCP runtime
    private ActorRef mediaGateway;
    private MediaGatewayInfo gatewayInfo;
    private MediaSession session;
    private ActorRef bridge;
    private ActorRef remoteConn;

    public MgcpMediaSessionController() {
        super();

        // States for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.ready = new State("ready", null, null);

        // Intermediate States
        this.acquiringMediaGatewayInfo = new State("acquiring media gateway info", null, null);
        this.acquiringMediaSession = new State("acquiring media session", null, null);
        this.acquiringBridge = new State("acquiring bridge", null, null);
        this.acquiringRemoteConnection = new State("acquiring remote connection", null, null);
        this.initializingRemoteConnection = new State("initializing remote connection", null, null);
        this.openingRemoteConnection = new State("opening remote connection", null, null);

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.acquiringMediaGatewayInfo));
        transitions.add(new Transition(this.acquiringMediaGatewayInfo, this.acquiringMediaSession));
        transitions.add(new Transition(this.acquiringMediaSession, this.acquiringBridge));
        transitions.add(new Transition(this.acquiringBridge, this.acquiringRemoteConnection));
        transitions.add(new Transition(this.acquiringRemoteConnection, this.initializingRemoteConnection));
        transitions.add(new Transition(this.initializingRemoteConnection, this.openingRemoteConnection));
        transitions.add(new Transition(this.openingRemoteConnection, this.ready));

        // FSM
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();

        if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        }
    }

    @Override
    protected void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender)
            throws TransitionFailedException, TransitionNotFoundException, TransitionRollbackException {
        this.call = sender;
        this.fsm.transition(message, this.acquiringMediaGatewayInfo);
    }

}

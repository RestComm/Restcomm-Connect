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
package org.mobicents.servlet.restcomm.mgcp.mrb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.UpdateConnection;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.JoinComplete;
import org.mobicents.servlet.restcomm.patterns.Observe;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;

public class MRBShunt extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State providingMediaGateway;
    //Get SDP
    private final State creatingBridge;
    private final State acquiringInternalLink;
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    //Pass SDP
    private final State initializingConnectingBridges;
    //update SDP
    private final State updatingHomeBridge;
    private final State active;
    private final State stopping;
    private final State inactive;
    private final State failed;

    private final ActorRef mediaGateway;
    private String msId;

    private final Map<String, ActorRef> mediaGatewayMap;

    private String localSdp;
    private String remoteSdp;

    private ActorRef localBridgeEndpoint;
    private ActorRef remoteBridgeEndpoint;
    private ActorRef remoteConn;

    private final DaoManager storage;
    // Observer pattern
    private final List<ActorRef> observers;

    public MRBShunt(ActorSystem system, Map<String, ActorRef> gateways, Configuration configuration, DaoManager storage){
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.providingMediaGateway = new State("providing Media Gateway", new ProvidingMediaGateway(source), null);
        this.creatingBridge = new State("creating bridge", new CreatingBridge(source), null);
        this.acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        this.initializingInternalLink = new State("acquiring media bridge", new InitializingInternalLink(source), null);
        this.openingInternalLink = new State("creating media group", new OpeningInternalLink(source), null);
        this.updatingInternalLink = new State("acquiring connection", new UpdatingInternalLink(source), null);
        this.initializingConnectingBridges = new State("initializing connection", new InitializingConnectingBridges(source), null);
        this.updatingHomeBridge = new State("opening connection", new UpdatingHomeBridge(source), null);
        this.active = new State("active", new Active(source), null);
        this.stopping = new State("stopping", new Stopping(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.providingMediaGateway));
        transitions.add(new Transition(this.providingMediaGateway, this.creatingBridge));
        transitions.add(new Transition(this.creatingBridge, this.acquiringInternalLink));
        transitions.add(new Transition(this.acquiringInternalLink, this.initializingInternalLink));
        transitions.add(new Transition(this.initializingInternalLink, this.openingInternalLink));
        transitions.add(new Transition(this.openingInternalLink, this.updatingInternalLink));
        transitions.add(new Transition(this.updatingInternalLink, this.initializingConnectingBridges));
        transitions.add(new Transition(this.initializingConnectingBridges, this.updatingHomeBridge));
        transitions.add(new Transition(this.updatingHomeBridge, this.active));
        transitions.add(new Transition(this.active, this.stopping));
        transitions.add(new Transition(this.stopping, this.inactive));
        transitions.add(new Transition(this.stopping, this.failed));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        this.storage = storage;
        this.mediaGatewayMap = gateways;
        this.mediaGateway = mediaGatewayMap.get(this.msId);

        // Observers
        this.observers = new ArrayList<ActorRef>(1);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MediaResourceBroker " + self().path() + " Processing Message: " + klass.getName());
        }
        if (JoinComplete.class.equals(klass)){
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        this.localBridgeEndpoint = (ActorRef) message.get();
        this.localBridgeEndpoint.tell(new Observe(self), self);
        //TODO: update database
        fsm.transition(message, this.initializingConnectingBridges);
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        logger.info("conferenceName: "+message.conferenceName()+" callSid: "+message.callSid()+" conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
        //TODO: update database
        mediaGateway.tell(new CreateBridgeEndpoint(message.mediaSession()), sender);
    }

    /*
     * ACTIONS
     */
    protected abstract class AbstractAction implements Action {

        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringInternalLink extends AbstractAction {

        public AcquiringInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}

    }
    private final class UpdatingHomeBridge extends AbstractAction {

        public UpdatingHomeBridge(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}

    }
    private final class CreatingBridge extends AbstractAction {

        public CreatingBridge(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}

    }
    
    private final class ProvidingMediaGateway extends AbstractAction {

        public ProvidingMediaGateway(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}

    }
    
    private final class InitializingConnectingBridges extends AbstractAction {

        public InitializingConnectingBridges(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            //TODO: daoamanger.getmastermediagateway
        }

    }
    private final class AcquiringRemoteConnection extends AbstractAction {

        public AcquiringRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}
    }

    private final class InitializingRemoteConnection extends AbstractAction {

        public InitializingRemoteConnection(ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            remoteConn = response.get();
            remoteConn.tell(new Observe(source), source);
            remoteConn.tell(new InitializeConnection(localBridgeEndpoint), source);
        }
    }

    private final class OpeningRemoteConnection extends AbstractAction {
        public OpeningRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}
    }

    private final class UpdatingRemoteConnection extends AbstractAction {
        public UpdatingRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final ConnectionDescriptor descriptor = new ConnectionDescriptor(remoteSdp);
            final UpdateConnection update = new UpdateConnection(descriptor);
            remoteConn.tell(update, source);
        }
    }

    private final class InitializingInternalLink extends AbstractAction {

        public InitializingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            //check in DB if bridging is not already under initialization.
            //cancel current operation if it is.
            
        }

    }

    private final class OpeningInternalLink extends AbstractAction {

        public OpeningInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {}

    }

    private final class UpdatingInternalLink extends AbstractAction {

        public UpdatingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}

    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {}
    }
    
    private class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {}

    }

    private abstract class FinalState extends AbstractAction {

        public FinalState(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {}
   }

    private final class Inactive extends FinalState {

        public Inactive(final ActorRef source) {
            super(source);
        }

    }

    private final class Failed extends FinalState {

        public Failed(final ActorRef source) {
            super(source);
        }

    }

    @Override
    public void postStop() {
        // Cleanup resources
        cleanup();

        // Clean observers
        observers.clear();

        // Terminate actor
        getContext().stop(self());
    }

    protected void cleanup() {}

}

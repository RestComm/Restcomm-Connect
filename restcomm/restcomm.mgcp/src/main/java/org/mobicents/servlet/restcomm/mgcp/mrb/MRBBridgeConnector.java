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
import org.mobicents.servlet.restcomm.dao.ConferenceDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.MediaResourceBrokerDao;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecord;
import org.mobicents.servlet.restcomm.entities.MediaResourceBrokerEntity;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.UpdateConnection;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.StartBridgeConnector;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

public class MRBBridgeConnector extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;
    private final State acquiringMediaSession;
    private final State creatingBridgeEndpoint;
    private final State acquiringRemoteConnection;
    private final State initializingRemoteConnection;
    private final State openingRemoteConnection;
    //Get SDP
    private final State pending;
    private final State acquiringInternalLink;
    //connect bridge with conference endpoint
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    private final State active;
    private final State initializingConnectingBridges;
    
    
    
    
    
    
    
    private final State updatingRemoteConnection;


    
    //Pass SDP
    //update SDP
    private final State updatingHomeBridge;
    private final State stopping;
    private final State inactive;
    private final State failed;

    private final Map<String, ActorRef> mediaGatewayMap;
    private final ActorRef mediaGateway;

    private String localMsId;
    private String masterMsId;
    private boolean isThisMasterBridgeConnector = false;
    private String localMediaServerSdp;
    private String remoteMediaServerSdp;
	private MediaSession mediaSession;
    private ActorRef localBridgeEndpoint;
	private ActorRef localConfernceEndpoint;
    private ActorRef connectionWithLocalBridgeEndpoint;
	private ActorRef linkBtwnBridgeAndConfernce;

    private final DaoManager storage;
    private MediaResourceBrokerEntity entity;
    private ConferenceDetailRecord cdr;
	private Sid conferenceSid;
	private String conferenceName;

    // Observer pattern
    private final List<ActorRef> observers;

	private ConnectionMode connectionMode;


    public MRBBridgeConnector(ActorSystem system, Map<String, ActorRef> gateways, Configuration configuration, DaoManager storage){
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.initializing = new State("initializing", new Initializing(source), null);
        this.acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        this.creatingBridgeEndpoint = new State("creating bridge endpoint", new CreatingBridgeEndpoint(source), null);
        this.acquiringRemoteConnection = new State("acquiring connection", new AcquiringRemoteConnection(source), null);
        this.initializingRemoteConnection = new State("initializing connection", new InitializingRemoteConnection(source), null);
        this.openingRemoteConnection = new State("opening connection", new OpeningRemoteConnection(source), null);
        this.pending = new State("pending", new Pending(source), null);
        this.acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        this.initializingInternalLink = new State("acquiring media bridge", new InitializingInternalLink(source), null);
        this.openingInternalLink = new State("creating media group", new OpeningInternalLink(source), null);
        this.updatingInternalLink = new State("acquiring connection", new UpdatingInternalLink(source), null);
        this.active = new State("active", new Active(source), null);
        this.initializingConnectingBridges = new State("initializing connection", new InitializingConnectingBridges(source), null);
        
        
        
        
        
        

        this.updatingRemoteConnection = new State("updating connection", new UpdatingRemoteConnection(source), null);
        this.updatingHomeBridge = new State("opening connection", new UpdatingHomeBridge(source), null);
        this.stopping = new State("stopping", new Stopping(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.initializing));
        transitions.add(new Transition(this.initializing, this.acquiringMediaSession));
        transitions.add(new Transition(this.acquiringMediaSession, this.creatingBridgeEndpoint));
        transitions.add(new Transition(this.creatingBridgeEndpoint, this.acquiringRemoteConnection));
        transitions.add(new Transition(this.acquiringRemoteConnection, this.initializingRemoteConnection));
        transitions.add(new Transition(this.initializingRemoteConnection, this.openingRemoteConnection));
        transitions.add(new Transition(this.openingRemoteConnection, this.failed));
        transitions.add(new Transition(this.openingRemoteConnection, this.pending));
        transitions.add(new Transition(this.pending, this.acquiringInternalLink));
        transitions.add(new Transition(this.acquiringInternalLink, this.initializingInternalLink));
        transitions.add(new Transition(this.initializingInternalLink, this.openingInternalLink));
        transitions.add(new Transition(this.openingInternalLink, this.updatingInternalLink));
        transitions.add(new Transition(this.updatingInternalLink, this.active));
        transitions.add(new Transition(this.updatingInternalLink, this.initializingConnectingBridges));
        
        
        
        
        
        
        
        
        transitions.add(new Transition(this.updatingInternalLink, this.initializingConnectingBridges));
        transitions.add(new Transition(this.initializingConnectingBridges, this.updatingHomeBridge));
        //transitions.add(new Transition(this.updatingHomeBridge, this.bridgeAndConferenceConnected));
        //transitions.add(new Transition(this.bridgeAndConferenceConnected, this.stopping));
        transitions.add(new Transition(this.stopping, this.inactive));
        transitions.add(new Transition(this.stopping, this.failed));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        this.storage = storage;
        this.mediaGatewayMap = gateways;
        this.mediaGateway = mediaGatewayMap.get(this.localMsId);

        // Observers
        this.observers = new ArrayList<ActorRef>(1);
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MRBBridgeConnector " + self().path() + " Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (StartBridgeConnector.class.equals(klass)){
            onStartBridgeConnector((StartBridgeConnector) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        }
    }

    private void onObserve(Observe message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            synchronized (this.observers) {
                this.observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.remove(observer);
        }
    }

    private void onStartBridgeConnector(StartBridgeConnector message, ActorRef self, ActorRef sender) throws Exception{
        if (is(uninitialized)) {
        	logger.info("conferenceName: "+message.conferenceName()+" connectionMode: "+message.connectionMode()+" conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
            this.localConfernceEndpoint = message.cnfEndpoint();
        	conferenceSid = message.conferenceSid();
            conferenceName = message.conferenceName();
            connectionMode = message.connectionMode();
            fsm.transition(message, initializing);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        if(is(acquiringMediaSession)){
            this.mediaSession = (MediaSession) message.get();
            this.fsm.transition(message, creatingBridgeEndpoint);
        } else if (is(creatingBridgeEndpoint)){
            this.localBridgeEndpoint = (ActorRef) message.get();
            this.localBridgeEndpoint.tell(new Observe(self), self);
            fsm.transition(message, acquiringRemoteConnection);
        } else if (is(acquiringRemoteConnection)){
            fsm.transition(message, initializingRemoteConnection);
        } else if (is(acquiringInternalLink)) {
            fsm.transition(message, initializingInternalLink);
        }
    }

    private void onConnectionStateChanged(ConnectionStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case CLOSED:
                if (is(initializingRemoteConnection)) {
                    fsm.transition(message, openingRemoteConnection);
                } else if (is(openingRemoteConnection)) {
                    fsm.transition(message, failed);
                } else if (is(updatingRemoteConnection)) {
                    fsm.transition(message, failed);
                }
                break;

            case HALF_OPEN:
                fsm.transition(message, pending);
                break;

            case OPEN:
            	logger.info("unhandled case");
                //fsm.transition(message, active);
                break;

            default:
                break;
        }
    }

    private void onLinkStateChanged(LinkStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case CLOSED:
                if (is(initializingInternalLink)) {
                    fsm.transition(message, openingInternalLink);
                } else if (is(openingInternalLink)) {
                    fsm.transition(message, stopping);
                } /*else if (is(closingInternalLink)) {
                    if (remoteConn != null) {
                        fsm.transition(message, active);
                    } else {
                        fsm.transition(message, inactive);
                    }
                }*/
                break;

            case OPEN:
                if (is(openingInternalLink)) {
                    fsm.transition(message, updatingInternalLink);
                } else if (is(updatingInternalLink)) {
                	if(isThisMasterBridgeConnector){
                		fsm.transition(message, active);
                	}else{
                		fsm.transition(message, initializingConnectingBridges);
                	}
                }
                break;

            default:
                break;
        }
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

    private final class Initializing extends AbstractAction {

        public Initializing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object msg) throws Exception {

        	//check master MS info from DB
            final ConferenceDetailRecordsDao conferenceDetailRecordsDao = storage.getConferenceDetailRecordsDao();
            cdr = conferenceDetailRecordsDao.getConferenceDetailRecord(conferenceSid);
            if(cdr == null){
            	logger.error("this is no information available in DB to proceed with this bridge connector");
            	fsm.transition(msg, failed);
            }else{
            	//msId in conference record is master msId
                masterMsId = cdr.getMasterMsId();
                if(localMsId == masterMsId){
                	logger.info("first participant Joined on master MS and sent StartBridgeConnector message to BridgeConnector");
                	isThisMasterBridgeConnector = true;
                }else{
                	logger.info("new slave sent StartBridgeConnector message to BridgeConnector");
                	// enter slave record in MRB resource table
                	addNewSlaveRecord();
                }
                fsm.transition(msg, acquiringMediaSession);
            }
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

    private final class CreatingBridgeEndpoint extends AbstractAction {

        public CreatingBridgeEndpoint(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new CreateBridgeEndpoint(mediaSession), source);
        }

    }

    private final class AcquiringRemoteConnection extends AbstractAction {

        public AcquiringRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new CreateConnection(mediaSession), source);
        }
    }

    private final class InitializingRemoteConnection extends AbstractAction {

        public InitializingRemoteConnection(ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            connectionWithLocalBridgeEndpoint = response.get();
            connectionWithLocalBridgeEndpoint.tell(new Observe(source), source);
            connectionWithLocalBridgeEndpoint.tell(new InitializeConnection(localBridgeEndpoint), source);
        }
    }

    private final class OpeningRemoteConnection extends AbstractAction {
        public OpeningRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            OpenConnection open = new OpenConnection(ConnectionMode.SendRecv, false);
            connectionWithLocalBridgeEndpoint.tell(open, source);
        }
    }

    private final class Pending extends AbstractAction {

        public Pending(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            ConnectionStateChanged connState = (ConnectionStateChanged) message;
            localMediaServerSdp = connState.descriptor().toString();
            if(entity != null){
            	if(isThisMasterBridgeConnector){
            		setMasterMediaServerSDP();
            		logger.info("A bridge has been create on master media server");
                }else{
                	setSlaveMediaServerSDP();
                	logger.info("A bridge has been create on slave media server");
                }
            	logger.info("Let's connect this bridge with local conference endpoint");
        		fsm.transition(message, acquiringInternalLink);
            }
        }
    }

    private final class AcquiringInternalLink extends AbstractAction {

        public AcquiringInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new CreateLink(mediaSession), source);
        }

    }

    private final class InitializingInternalLink extends AbstractAction {

        public InitializingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            if(logger.isInfoEnabled()) {
                logger.info("##################### $$ Bridge for Conference " + self().path() + " is terminated: "+ localBridgeEndpoint.isTerminated());
            }
            linkBtwnBridgeAndConfernce = response.get();
            linkBtwnBridgeAndConfernce.tell(new Observe(source), source);
            linkBtwnBridgeAndConfernce.tell(new InitializeLink(localBridgeEndpoint, localConfernceEndpoint), source);
        }

    }

    private final class OpeningInternalLink extends AbstractAction {

        public OpeningInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
        	linkBtwnBridgeAndConfernce.tell(new OpenLink(connectionMode), source);
        }

    }

    private final class UpdatingInternalLink extends AbstractAction {

        public UpdatingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateLink update = new UpdateLink(ConnectionMode.SendRecv, UpdateLink.Type.PRIMARY);
            linkBtwnBridgeAndConfernce.tell(update, source);
        }

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
    
    
    
    
    
    


    
    


    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
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
            final ConnectionDescriptor descriptor = new ConnectionDescriptor(remoteMediaServerSdp);
            final UpdateConnection update = new UpdateConnection(descriptor);
            connectionWithLocalBridgeEndpoint.tell(update, source);
        }
    }
    private final class UpdatingHomeBridge extends AbstractAction {

        public UpdatingHomeBridge(final ActorRef source) {
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

    /*
     * Database Utility Functions
     * 
     */
    private void addNewSlaveRecord() {
        final MediaResourceBrokerDao dao= storage.getMediaResourceBrokerDao();
        final MediaResourceBrokerEntity.Builder builder = MediaResourceBrokerEntity.builder();

        builder.setConferenceSid(conferenceSid);
        builder.setSlaveMsId(localMsId);
        builder.setBridgedTogether(false);

        entity = builder.build();
        dao.addMediaResourceBrokerEntity(entity);
    }

    private void setSlaveMediaServerSDP() {
		final MediaResourceBrokerDao dao= storage.getMediaResourceBrokerDao();
        entity = entity.setSlaveMsSDP(localMediaServerSdp);
        dao.updateMediaResource(entity);
		
	}

    private void setMasterMediaServerSDP() {
		final ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
		cdr = cdr.setMasterMsSDP(localMediaServerSdp);
		dao.updateConferenceDetailRecord(cdr);
	}
}

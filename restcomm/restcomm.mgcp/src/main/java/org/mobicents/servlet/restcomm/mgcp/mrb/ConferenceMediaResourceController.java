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
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.StopLookingForSlaves;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

public class ConferenceMediaResourceController extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State gettingConferenceInfoFromDB;
    private final State creatingBridgeEndpoint;
    private final State acquiringRemoteConnection;
    private final State initializingRemoteConnection;
    private final State openingRemoteConnection;
    private final State acquiringInternalLink;
    //connect bridge with conference endpoint
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    private final State active;
    private final State initializingConnectingBridges;
    ////
    private final State stopping;
    private final State inactive;
    private final State failed;

    private final Map<String, ActorRef> allMediaGateways;
    private final ActorRef mediaGateway;

    private String localMsId;
    private String masterMsId;
    private boolean isThisMaster = false;
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

    public boolean keepLookingForSlaves = true;


    public ConferenceMediaResourceController(final String localMsId, final Map<String, ActorRef> gateways, final Configuration configuration, final DaoManager storage){
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.gettingConferenceInfoFromDB = new State("getting Conference Info From DB", new GettingConferenceInfoFromDB(source), null);
        this.creatingBridgeEndpoint = new State("creating bridge endpoint", new CreatingBridgeEndpoint(source), null);
        this.acquiringRemoteConnection = new State("acquiring connection", new AcquiringRemoteConnection(source), null);
        this.initializingRemoteConnection = new State("initializing connection", new InitializingRemoteConnection(source), null);
        this.openingRemoteConnection = new State("opening connection", new OpeningRemoteConnection(source), null);
        this.acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        this.initializingInternalLink = new State("acquiring media bridge", new InitializingInternalLink(source), null);
        this.openingInternalLink = new State("creating media group", new OpeningInternalLink(source), null);
        this.updatingInternalLink = new State("acquiring connection", new UpdatingInternalLink(source), null);
        this.active = new State("active", new Active(source), null);
        this.initializingConnectingBridges = new State("initializing connection", new InitializingConnectingBridges(source), null);

        //this.updatingRemoteConnection = new State("updating connection", new UpdatingRemoteConnection(source), null);
        //this.updatingHomeBridge = new State("opening connection", new UpdatingHomeBridge(source), null);
        this.stopping = new State("stopping", new Stopping(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, gettingConferenceInfoFromDB));
        transitions.add(new Transition(gettingConferenceInfoFromDB, creatingBridgeEndpoint));
        transitions.add(new Transition(creatingBridgeEndpoint, acquiringRemoteConnection));
        transitions.add(new Transition(acquiringRemoteConnection, initializingRemoteConnection));
        transitions.add(new Transition(initializingRemoteConnection, openingRemoteConnection));
        transitions.add(new Transition(openingRemoteConnection, failed));
        transitions.add(new Transition(openingRemoteConnection, acquiringInternalLink));
        transitions.add(new Transition(acquiringInternalLink, initializingInternalLink));
        transitions.add(new Transition(initializingInternalLink, openingInternalLink));
        transitions.add(new Transition(openingInternalLink, updatingInternalLink));
        transitions.add(new Transition(updatingInternalLink, active));
        transitions.add(new Transition(updatingInternalLink, initializingConnectingBridges));

        /*transitions.add(new Transition(this.updatingInternalLink, this.initializingConnectingBridges));
        transitions.add(new Transition(this.initializingConnectingBridges, this.updatingHomeBridge));
        //transitions.add(new Transition(this.updatingHomeBridge, this.bridgeAndConferenceConnected));
        //transitions.add(new Transition(this.bridgeAndConferenceConnected, this.stopping));
        transitions.add(new Transition(this.stopping, this.inactive));
        transitions.add(new Transition(this.stopping, this.failed));*/

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        this.storage = storage;
        this.allMediaGateways = gateways;
        logger.info("localMsId: "+localMsId);
        this.localMsId = localMsId;
        this.mediaGateway = allMediaGateways.get(this.localMsId);

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
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MRBBridgeConnector " + self().path() + " Processing Message: " + klass.getName());
            logger.info(" ********** MRBBridgeConnector " + self().path() + " Current State: \"" + state.toString());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (StartBridgeConnector.class.equals(klass)){
            onStartBridgeConnector((StartBridgeConnector) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            logger.info("going to call onMediaGatewayResponse");
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (ConnectionStateChanged.class.equals(klass)) {
            onConnectionStateChanged((ConnectionStateChanged) message, self, sender);
        } else if (LinkStateChanged.class.equals(klass)) {
            onLinkStateChanged((LinkStateChanged) message, self, sender);
        } else if(StopLookingForSlaves.class.equals(klass)) {
        	this.keepLookingForSlaves = false;
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
            logger.info("onStartBridgeConnector: conferenceName: "+message.conferenceName()+" connectionMode: "+message.connectionMode()+" conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
            this.localConfernceEndpoint = message.cnfEndpoint();
            conferenceSid = message.conferenceSid();
            conferenceName = message.conferenceName();
            connectionMode = message.connectionMode();
            fsm.transition(message, gettingConferenceInfoFromDB);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        logger.info("inside onMediaGatewayResponse: state = "+fsm.state());
        if(is(gettingConferenceInfoFromDB)){
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGatewayResponse - initializing to acquiringMediaSession ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            this.mediaSession = (MediaSession) message.get();
            this.fsm.transition(message, creatingBridgeEndpoint);
        } else if (is(creatingBridgeEndpoint)){
            this.localBridgeEndpoint = (ActorRef) message.get();
            this.localBridgeEndpoint.tell(new Observe(self), self);
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGatewayResponse - creatingBridgeEndpoint - received localBridgeEndpoint:"+localBridgeEndpoint+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            fsm.transition(message, acquiringRemoteConnection);
        } else if (is(acquiringRemoteConnection)){
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGatewayResponse - acquiringRemoteConnection ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            fsm.transition(message, initializingRemoteConnection);
        } else if (is(acquiringInternalLink)) {
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGatewayResponse - acquiringInternalLink ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            fsm.transition(message, initializingInternalLink);
        }
    }

    private void onConnectionStateChanged(ConnectionStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onConnectionStateChanged - received connection STATE is: "+message.state()+" current fsm STATE is: "+fsm.state()+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        switch (message.state()) {
            case CLOSED:
                if (is(initializingRemoteConnection)) {
                    fsm.transition(message, openingRemoteConnection);
                } else if (is(openingRemoteConnection)) {
                    fsm.transition(message, failed);
                }/*else if (is(updatingRemoteConnection)) {
                    fsm.transition(message, failed);
                }*/
                break;

            case HALF_OPEN:
                ConnectionStateChanged connState = (ConnectionStateChanged) message;
                localMediaServerSdp = connState.descriptor().toString();
                logger.info("localMediaServerSdp: "+localMediaServerSdp);
                if(isThisMaster){
                    setMasterMediaServerSDP();
                    logger.info("A bridge has been create on master media server");
                }else{
                    setSlaveMediaServerSDP();
                    logger.info("A bridge has been create on slave media server");
                }
                logger.info("Let's connect this bridge with local conference endpoint");
                fsm.transition(message, acquiringInternalLink);
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
        logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onLinkStateChanged - received link STATE is: "+message.state()+" current fsm STATE is: "+fsm.state()+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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
                    if(isThisMaster){
                    	
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
     *
     */
    protected abstract class AbstractAction implements Action {

        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class GettingConferenceInfoFromDB extends AbstractAction {

        public GettingConferenceInfoFromDB(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object msg) throws Exception {
            logger.info("current state is: "+fsm.state());
            //check master MS info from DB
            final ConferenceDetailRecordsDao conferenceDetailRecordsDao = storage.getConferenceDetailRecordsDao();
            cdr = conferenceDetailRecordsDao.getConferenceDetailRecord(conferenceSid);
            if(cdr == null){
                logger.error("there is no information available in DB to proceed with this bridge connector");
                fsm.transition(msg, failed);
            }else{
                //msId in conference record is master msId
                masterMsId = cdr.getMasterMsId();
                if(localMsId.equalsIgnoreCase(masterMsId)){
                    logger.info("first participant Joined on master MS and sent StartBridgeConnector message to BridgeConnector");
                    isThisMaster = true;
                }else{
                    logger.info("new slave sent StartBridgeConnector message to BridgeConnector");
                    // enter slave record in MRB resource table
                    addNewSlaveRecord();
                }
                logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ AcquiringMediaSession ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                mediaGateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateMediaSession(), source);
            }
        }

    }

    private final class CreatingBridgeEndpoint extends AbstractAction {

        public CreatingBridgeEndpoint(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ CreatingBridgeEndpoint - current fsm STATE is"+fsm.state()+" media session ="+mediaSession+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ InitializingRemoteConnection -  - received connectionWithLocalBridgeEndpoint:"+connectionWithLocalBridgeEndpoint+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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
            final ActorRef self = self();


            if(isThisMaster){
            	//TODO: create a new actor ask it to do this
                //TODO: yahan pay koi job laga do jo db me dekhti rahay slave bridges ki appearance
                //TODO: as soon as they come modify connection with their sdp and change isbridge to true;
                while(keepLookingForSlaves){
                    logger.info("keep Looking For Slaves");
                    List<MediaResourceBrokerEntity> slaveEntities = searchForUnConnectedSlaves();
                    if(slaveEntities != null && slaveEntities.size() > 0){
                        for(MediaResourceBrokerEntity e : slaveEntities){
                            final ConnectionDescriptor descriptor = new ConnectionDescriptor(e.getSlaveMsSDP());
                            final UpdateConnection update = new UpdateConnection(descriptor);
                            connectionWithLocalBridgeEndpoint.tell(update, source);
                            logger.info("^^^^^^^^^^^^^^^^ Slave Found Breaking the search. ^^^^^^^^^^^^^^^^^^");
                            //TODO this is temporary stuff hope to connect only two participants from 2 RMS.
                            keepLookingForSlaves = false;
                            break;
                        }
                    }
                }
            }else{
                ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
                cdr = dao.getConferenceDetailRecord(cdr.getSid());
                logger.info("cdr.getSid(): "+cdr.getSid()+"cdr.status: "+cdr.getStatus());

                final ConnectionDescriptor descriptor = new ConnectionDescriptor(cdr.getMasterMsSDP());
                final UpdateConnection update = new UpdateConnection(descriptor);
                connectionWithLocalBridgeEndpoint.tell(update, source);
                /*ActorRef slaveBridgeConnector = getSlaveBridgeConnector();
                slaveBridgeConnector.tell(new Observe(self), self);
                slaveBridgeConnector.tell(new StartSlaveBridgeConnector(), self);*/
            }
        }
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

    private ActorRef getSlaveBridgeConnector() {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                //Here Here we can pass Gateway where call is connected
                return new SlaveBridgeConnector(allMediaGateways.get(masterMsId));
            }
        }));
    }

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
        logger.info("inside setMasterMediaServerSDP: localMediaServerSdp="+localMediaServerSdp);
        final ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
        cdr = cdr.setMasterMsSDP(localMediaServerSdp);
        dao.updateConferenceDetailRecord(cdr);
    }

    private List<MediaResourceBrokerEntity> searchForUnConnectedSlaves(){
        final MediaResourceBrokerDao dao= storage.getMediaResourceBrokerDao();
        List<MediaResourceBrokerEntity> slaveEntities = null;
        slaveEntities = dao.getUnConnectedSlaveEntitiesByConfSid(conferenceSid);
        return slaveEntities;
    }
}

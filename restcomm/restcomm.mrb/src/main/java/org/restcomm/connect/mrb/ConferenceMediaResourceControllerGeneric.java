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
package org.restcomm.connect.mrb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.fsm.Action;
import org.restcomm.connect.commons.fsm.FiniteStateMachine;
import org.restcomm.connect.commons.fsm.State;
import org.restcomm.connect.commons.fsm.Transition;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.mgcp.MediaGatewayResponse;
import org.restcomm.connect.mgcp.MediaSession;
import org.restcomm.connect.mrb.api.ConferenceMediaResourceControllerStateChanged;
import org.restcomm.connect.mrb.api.StartConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.StopConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.StopConferenceMediaResourceControllerResponse;
import org.restcomm.connect.mscontrol.api.messages.MediaGroupResponse;
import org.restcomm.connect.mscontrol.api.messages.MediaGroupStateChanged;
import org.restcomm.connect.mscontrol.api.messages.Play;
import org.restcomm.connect.mscontrol.api.messages.Record;
import org.restcomm.connect.mscontrol.api.messages.StartMediaGroup;
import org.restcomm.connect.mscontrol.api.messages.StartRecording;
import org.restcomm.connect.mscontrol.api.messages.Stop;
import org.restcomm.connect.mscontrol.api.messages.StopMediaGroup;
import org.restcomm.connect.mscontrol.api.messages.StopRecording;
import org.restcomm.connect.mscontrol.mms.MgcpMediaGroup;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class ConferenceMediaResourceControllerGeneric extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State acquiringConferenceInfo;
    private final State creatingMediaGroup;

    private final State preActive;
    private final State active;
    private final State stopping;
    private final State inactive;
    private final State failed;

    private final ActorRef localMediaGateway;
    private ActorRef mediaGroup;
    private String masterIVREndpointIdName;
    private MediaSession localMediaSession;
    private ActorRef localConfernceEndpoint;
    private ActorRef connectionWithLocalMS;
    private ActorRef connectionWithMasterMS;

    private final DaoManager storage;
    private final Configuration configuration;
    private ConferenceDetailRecord cdr;
    private Sid conferenceSid;

    // Runtime media operations
    private Boolean playing;
    private Boolean fail;
    private Boolean recording;
    private DateTime recordStarted;

    // Observer pattern
    private final List<ActorRef> observers;
    private final ActorRef mrb;

    public ConferenceMediaResourceControllerGeneric(final String localMsId, ActorRef localMediaGateway, final Configuration configuration, final DaoManager storage, final ActorRef mrb){
    //public ConferenceMediaResourceController(final String localMsId, final Map<String, ActorRef> gateways, final Configuration configuration, final DaoManager storage){
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(source), null);
        this.acquiringConferenceInfo = new State("getting Conference Info From DB", new AcquiringConferenceInfo(source), null);
        this.preActive = new State("pre active", new PreActive(source));
        this.active = new State("active", new Active(source));
        this.stopping = new State("stopping", new Stopping(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        //states for master
        transitions.add(new Transition(uninitialized, acquiringConferenceInfo));
        transitions.add(new Transition(acquiringConferenceInfo, creatingMediaGroup));
        transitions.add(new Transition(creatingMediaGroup, preActive));
        transitions.add(new Transition(preActive, active));
        transitions.add(new Transition(active, stopping));
        transitions.add(new Transition(stopping, inactive));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        this.storage = storage;
        this.configuration = configuration;
        logger.info("localMsId: "+localMsId);
        this.localMediaGateway = localMediaGateway;
        masterIVREndpointIdName = null;

        // Runtime media operations
        this.playing = Boolean.FALSE;
        this.recording = Boolean.FALSE;
        this.fail = Boolean.FALSE;

        this.mrb = mrb;

        // Observers
        this.observers = new ArrayList<ActorRef>(1);
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private void broadcast(Object message) {
        if (!this.observers.isEmpty()) {
            final ActorRef self = self();
            synchronized (this.observers) {
                for (ActorRef observer : observers) {
                    observer.tell(message, self);
                }
            }
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();
        final State state = fsm.state();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** ConferenceMediaResourceController " + self().path() + " Processing Message: " + klass.getName());
            logger.info(" ********** ConferenceMediaResourceController " + self().path() + " Current State: \"" + state.toString());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (StartConferenceMediaResourceController.class.equals(klass)){
            onStartConferenceMediaResourceController((StartConferenceMediaResourceController) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            onMediaGroupStateChanged((MediaGroupStateChanged) message, self, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            onStopMediaGroup((StopMediaGroup) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if(MediaGroupResponse.class.equals(klass)) {
            onMediaGroupResponse((MediaGroupResponse<String>) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecording((StartRecording) message, self, sender);
        } else if (StopRecording.class.equals(klass)) {
            onStopRecording((StopRecording) message, self, sender);
        } else if (StopConferenceMediaResourceController.class.equals(klass)) {
            onStopConferenceMediaResourceController((StopConferenceMediaResourceController) message, self, sender);
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

    private void onStartConferenceMediaResourceController(StartConferenceMediaResourceController message, ActorRef self, ActorRef sender) throws Exception{
        if (is(uninitialized)) {
            if(logger.isInfoEnabled())
                logger.info("onStartConferenceMediaResourceController: conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
            this.localConfernceEndpoint = message.cnfEndpoint();
            this.conferenceSid = message.conferenceSid();
            fsm.transition(message, acquiringConferenceInfo);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        logger.info("inside onMediaGatewayResponse: state = "+fsm.state());
        if (is(acquiringConferenceInfo)){
            this.localMediaSession = (MediaSession) message.get();
            this.fsm.transition(message, creatingMediaGroup);
        }
    }

    private void onStopConferenceMediaResourceController(StopConferenceMediaResourceController message, ActorRef self,
            ActorRef sender) throws Exception {
        if(logger.isInfoEnabled())
            logger.info("onStopConferenceMediaResourceController");
        sender.tell(new StopConferenceMediaResourceControllerResponse(true), sender);
        fsm.transition(message, stopping);
    }

    private void onMediaGroupStateChanged(MediaGroupStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isInfoEnabled())
            logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGroupStateChanged - received STATE is: "+message.state()+" current fsm STATE is: "+fsm.state()+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        switch (message.state()) {
            case ACTIVE:
                if (is(creatingMediaGroup)) {
                    fsm.transition(message, preActive);
                }
                break;

            case INACTIVE:
                if (is(creatingMediaGroup)) {
                    this.fail = Boolean.TRUE;
                    fsm.transition(message, failed);
                } else if (is(stopping)) {
                    // Stop media group actor
                    this.mediaGroup.tell(new StopObserving(self), self);
                    context().stop(mediaGroup);
                    this.mediaGroup = null;

                    // Move to next state
                    if (this.mediaGroup == null && this.localConfernceEndpoint == null) {
                        this.fsm.transition(message, fail ? failed : inactive);
                    }
                }
                break;

            default:
                break;
        }
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (!playing) {
            this.playing = Boolean.TRUE;
            this.mediaGroup.tell(message, self);
        }
    }

    private void onStartRecording(StartRecording message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) && !recording) {
            String finishOnKey = "1234567890*#";
            int maxLength = 3600;
            int timeout = 5;

            this.recording = Boolean.TRUE;
            this.recordStarted = DateTime.now();

            // Tell media group to start recording
            Record record = new Record(message.getRecordingUri(), timeout, maxLength, finishOnKey);
            this.mediaGroup.tell(record, null);
        }
    }

    private void onStopRecording(StopRecording message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) && recording) {
            this.recording = Boolean.FALSE;
            mediaGroup.tell(new Stop(), null);
        }
    }

    private void onMediaGroupResponse(MediaGroupResponse<String> message, ActorRef self, ActorRef sender) throws Exception {
        if (this.playing) {
            this.playing = Boolean.FALSE;
        }
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
        //if (is(active)) {
            // Stop the primary media group
            this.mediaGroup.tell(new Stop(), self);
            this.playing = Boolean.FALSE;
        //}
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

    private final class AcquiringConferenceInfo extends AbstractAction {

        public AcquiringConferenceInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object msg) throws Exception {
            logger.info("current state is: "+fsm.state());
            //check master MS info from DB
            final ConferenceDetailRecordsDao conferenceDetailRecordsDao = storage.getConferenceDetailRecordsDao();
            cdr = conferenceDetailRecordsDao.getConferenceDetailRecord(conferenceSid);
            if(cdr == null){
                logger.error("there is no information available in DB to proceed with this CMRC");
                fsm.transition(msg, failed);
            }else{
                if(logger.isInfoEnabled()){
                    logger.info("first participant Joined on master MS and sent message to CMRC");
                }
                localMediaGateway.tell(new org.restcomm.connect.mgcp.CreateMediaSession(), source);
            }
        }

    }

    private final class CreatingMediaGroup extends AbstractAction {

        public CreatingMediaGroup(ActorRef source) {
            super(source);
        }

        private ActorRef createMediaGroup(final Object message) {
            return getContext().actorOf(new Props(new UntypedActorFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public UntypedActor create() throws Exception {
                    return new MgcpMediaGroup(localMediaGateway, localMediaSession, localConfernceEndpoint, masterIVREndpointIdName);
                }
            }));
        }

        @Override
        public void execute(Object message) throws Exception {
            mediaGroup = createMediaGroup(message);
            mediaGroup.tell(new Observe(super.source), super.source);
            mediaGroup.tell(new StartMediaGroup(), super.source);
        }
    }

    private final class PreActive extends AbstractAction {

        public PreActive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("CMRC is in pre ACTIVE NOW...");
            // later Conference will update the status as per informed by VI as per RCML
            updateConferenceStatus("RUNNING_MODERATOR_ABSENT");
            broadcast(new ConferenceMediaResourceControllerStateChanged(ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.ACTIVE, cdr.getStatus()));
            fsm.transition(message, active);
        }
    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("CMRC is ACTIVE NOW...");
        }
    }

    private class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("CMRC is STOPPING NOW...");
            updateConferenceStatus("COMPLETED");
            // Destroy Media Group
            mediaGroup.tell(new StopMediaGroup(), super.source);
        }
    }

    private abstract class FinalState extends AbstractAction {

        private final ConferenceMediaResourceControllerStateChanged.MediaServerControllerState state;

        public FinalState(ActorRef source, final ConferenceMediaResourceControllerStateChanged.MediaServerControllerState state) {
            super(source);
            this.state = state;
        }

        @Override
        public void execute(Object message) throws Exception {
            // Notify observers the controller has stopped
            broadcast(new ConferenceMediaResourceControllerStateChanged(state));
        }

    }

    private final class Inactive extends FinalState {

        public Inactive(final ActorRef source) {
            super(source, ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.INACTIVE);
        }

    }

    private final class Failed extends FinalState {

        public Failed(final ActorRef source) {
            super(source, ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.FAILED);
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

    protected void cleanup() {
        if (connectionWithLocalMS != null) {
            context().stop(connectionWithLocalMS);
            connectionWithLocalMS = null;
        }

        if (connectionWithMasterMS != null) {
            context().stop(connectionWithMasterMS);
            connectionWithMasterMS = null;
        }
    }
    /*
     * Database Utility Functions
     *
     */

    private void updateConferenceStatus(String status){
        if(cdr != null){
            final ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
            cdr = dao.getConferenceDetailRecord(conferenceSid);
            cdr = cdr.setStatus(status);
            dao.updateConferenceDetailRecordStatus(cdr);
        }
    }
}

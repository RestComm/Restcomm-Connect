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

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.fsm.Action;
import org.restcomm.connect.commons.fsm.FiniteStateMachine;
import org.restcomm.connect.commons.fsm.State;
import org.restcomm.connect.commons.fsm.Transition;
import org.restcomm.connect.commons.fsm.TransitionNotFoundException;
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
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class ConferenceMediaResourceControllerGeneric extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    protected State uninitialized;
    protected State acquiringConferenceInfo;
    protected State creatingMediaGroup;

    protected State preActive;
    protected State active;
    protected State stopping;
    protected State inactive;
    protected State failed;

    protected ActorRef localMediaGateway;
    protected ActorRef mediaGroup;
    protected MediaSession localMediaSession;
    protected ActorRef localConfernceEndpoint;

    protected DaoManager storage;
    protected Configuration configuration;
    protected ConferenceDetailRecord cdr;
    protected Sid conferenceSid;

    // Runtime media operations
    protected Boolean playing;
    protected Boolean fail;
    protected Boolean recording;
    protected DateTime recordStarted;

    // Observer pattern
    protected final List<ActorRef> observers;
    protected ActorRef mrb;

    public ConferenceMediaResourceControllerGeneric(ActorRef localMediaGateway, final Configuration configuration, final DaoManager storage, final ActorRef mrb){
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
        this.localMediaGateway = localMediaGateway;

        // Runtime media operations
        this.playing = Boolean.FALSE;
        this.recording = Boolean.FALSE;
        this.fail = Boolean.FALSE;

        this.mrb = mrb;

        // Observers
        this.observers = new ArrayList<ActorRef>(1);
    }

    protected boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    protected void broadcast(Object message) {
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

        try{
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
        }catch(Exception e){
            logger.error("Exception in onReceive of CMRC: {}", e);
            try{
                fsm.transition(message, failed);
            }catch(TransitionNotFoundException tfe){
                /* some state might not allow direct transition to failed state:
                 * in that case catch the TransitionFailedException and print error.
                 */
                logger.error("CMRC failed at a state which does not allow direct transition to FAILED state. Current state is: " + state.toString());
            }
        }
    }

    protected void onObserve(Observe message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            synchronized (this.observers) {
                this.observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    protected void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.remove(observer);
        }
    }

    protected void onStartConferenceMediaResourceController(StartConferenceMediaResourceController message, ActorRef self, ActorRef sender) throws Exception{
        if (is(uninitialized)) {
            if(logger.isDebugEnabled())
                logger.debug("onStartConferenceMediaResourceController: conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
            this.localConfernceEndpoint = message.cnfEndpoint();
            this.conferenceSid = message.conferenceSid();
            fsm.transition(message, acquiringConferenceInfo);
        }
    }

    protected void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        logger.info("inside onMediaGatewayResponse: state = "+fsm.state());
        if (is(acquiringConferenceInfo)){
            this.localMediaSession = (MediaSession) message.get();
            this.fsm.transition(message, creatingMediaGroup);
        }
    }

    protected void onStopConferenceMediaResourceController(StopConferenceMediaResourceController message, ActorRef self,
            ActorRef sender) throws Exception {
        fsm.transition(message, stopping);
    }

    protected void onMediaGroupStateChanged(MediaGroupStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isDebugEnabled())
            logger.debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ onMediaGroupStateChanged - received STATE is: "+message.state()+" current fsm STATE is: "+fsm.state()+" ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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
                    this.fsm.transition(message, fail ? failed : inactive);
                }
                break;

            default:
                break;
        }
    }

    protected void onPlay(Play message, ActorRef self, ActorRef sender) {
        /*
         * https://github.com/RestComm/Restcomm-Connect/issues/2024
         * We actually dont care about running beeps or MOH, we will send new play
         * it will stop exiting playing audio and will play new one
         * (unless both are exactly same)
         */
        //if (!playing) {
            //this.playing = Boolean.TRUE;
            this.mediaGroup.tell(message, self);
        //}
    }

    protected void onStartRecording(StartRecording message, ActorRef self, ActorRef sender) throws Exception {
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

    protected void onStopRecording(StopRecording message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) && recording) {
            this.recording = Boolean.FALSE;
            mediaGroup.tell(new Stop(), null);
        }
    }

    protected void onMediaGroupResponse(MediaGroupResponse<String> message, ActorRef self, ActorRef sender) throws Exception {
        if (this.playing) {
            this.playing = Boolean.FALSE;
        }
    }

    protected void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
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

    protected final class AcquiringConferenceInfo extends AbstractAction {

        public AcquiringConferenceInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object msg) throws Exception {
            if(logger.isDebugEnabled())
                logger.debug("current state is: "+fsm.state());
            //check master MS info from DB
            final ConferenceDetailRecordsDao conferenceDetailRecordsDao = storage.getConferenceDetailRecordsDao();
            cdr = conferenceDetailRecordsDao.getConferenceDetailRecord(conferenceSid);
            if(cdr == null){
                logger.error("there is no information available in DB to proceed with this CMRC");
                fsm.transition(msg, failed);
            }else{
                if(logger.isDebugEnabled()){
                    logger.debug("first participant Joined on master MS and sent message to CMRC");
                }
                localMediaGateway.tell(new org.restcomm.connect.mgcp.CreateMediaSession(), source);
            }
        }

    }

    protected final class CreatingMediaGroup extends AbstractAction {

        public CreatingMediaGroup(ActorRef source) {
            super(source);
        }
        protected ActorRef createMediaGroup(final Object message) {
            final Props props = new Props(new UntypedActorFactory() {
                protected static final long serialVersionUID = 1L;

                @Override
                public UntypedActor create() throws Exception {
                    return new MgcpMediaGroup(localMediaGateway, localMediaSession, localConfernceEndpoint);
                }
            });
            return getContext().actorOf(props);
        }

        @Override
        public void execute(Object message) throws Exception {
            mediaGroup = createMediaGroup(message);
            mediaGroup.tell(new Observe(super.source), super.source);
            mediaGroup.tell(new StartMediaGroup(), super.source);
        }
    }

    protected final class PreActive extends AbstractAction {

        public PreActive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isDebugEnabled())
                logger.debug("CMRC is in pre ACTIVE NOW...");
            // later Conference will update the status as per informed by VI as per RCML
            updateConferenceStatus(ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT+"");
            broadcast(new ConferenceMediaResourceControllerStateChanged(ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.ACTIVE, cdr.getStatus()));
            fsm.transition(message, active);
        }
    }

    protected final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("CMRC is ACTIVE NOW...");
        }
    }

    protected class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("CMRC is STOPPING NOW...");
            updateConferenceStatus(ConferenceStateChanged.State.COMPLETED+"");
            // Destroy Media Group
            mediaGroup.tell(new StopMediaGroup(), super.source);
        }
    }

    protected abstract class FinalState extends AbstractAction {

        protected final ConferenceMediaResourceControllerStateChanged.MediaServerControllerState state;

        public FinalState(ActorRef source, final ConferenceMediaResourceControllerStateChanged.MediaServerControllerState state) {
            super(source);
            this.state = state;
        }

        @Override
        public void execute(Object message) throws Exception {
            // Notify observers the controller has stopped
            broadcast(new ConferenceMediaResourceControllerStateChanged(state, true));
        }

    }

    protected final class Inactive extends FinalState {

        public Inactive(final ActorRef source) {
            super(source, ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.INACTIVE);
        }

    }

    protected final class Failed extends FinalState {

        public Failed(final ActorRef source) {
            super(source, ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.FAILED);
        }

    }

    @Override
    public void postStop() {
        if(logger.isDebugEnabled())
            logger.debug("postStop called for: "+self());
        // Cleanup resources
        cleanup();

        // Clean observers
        observers.clear();

        // Terminate actor
        if(self() != null && !self().isTerminated()){
            getContext().stop(self());
        }
    }

    protected void cleanup() {
    }
    /*
     * Database Utility Functions
     *
     */

    protected void updateConferenceStatus(String status){
        if(cdr != null){
            final ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
            cdr = dao.getConferenceDetailRecord(conferenceSid);
            cdr = cdr.setStatus(status);
            dao.updateConferenceDetailRecordStatus(cdr);
        }
    }
}

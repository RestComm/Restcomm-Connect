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

package org.restcomm.connect.mscontrol.mms;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerConferenceControllerStateChanged;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.fsm.Action;
import org.restcomm.connect.commons.fsm.FiniteStateMachine;
import org.restcomm.connect.commons.fsm.State;
import org.restcomm.connect.commons.fsm.Transition;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.mgcp.CreateConferenceEndpoint;
import org.restcomm.connect.mgcp.DestroyEndpoint;
import org.restcomm.connect.mgcp.EndpointState;
import org.restcomm.connect.mgcp.EndpointStateChanged;
import org.restcomm.connect.mgcp.MediaGatewayResponse;
import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mgcp.MediaSession;
import org.restcomm.connect.mrb.api.ConferenceMediaResourceControllerStateChanged;
import org.restcomm.connect.mrb.api.GetConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.MediaGatewayForConference;
import org.restcomm.connect.mrb.api.StartConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.StopConferenceMediaResourceController;
import org.restcomm.connect.mscontrol.api.MediaServerController;
import org.restcomm.connect.mscontrol.api.messages.CloseMediaSession;
import org.restcomm.connect.mscontrol.api.messages.CreateMediaSession;
import org.restcomm.connect.mscontrol.api.messages.JoinCall;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.JoinConference;
import org.restcomm.connect.mscontrol.api.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.restcomm.connect.mscontrol.api.messages.Play;
import org.restcomm.connect.mscontrol.api.messages.StartRecording;
import org.restcomm.connect.mscontrol.api.messages.Stop;
import org.restcomm.connect.mscontrol.api.messages.StopMediaGroup;
import org.restcomm.connect.mscontrol.api.messages.StopRecording;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Maria Farooq (maria.farooq@telestax.com)
 */
@Immutable
public final class MmsConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State acquiringMediaGateway;
    private final State acquiringCnfMediaResourceController;
    private final State active;
    private final State inactive;
    private final State failed;
    private final State acquiringMediaSession;
    private final State acquiringEndpoint;
    //private final State creatingMediaGroup;
    private final State stopping;
    private final State stoppingCMRC;
    //private Boolean fail;

    // MGCP runtime stuff.
    private ActorRef mediaGateway;
    private MediaSession mediaSession;
    private ActorRef cnfEndpoint;

    // Conference runtime stuff
    //private ActorRef conference;
    //private ActorRef mediaGroup;
    private ActorRef conferenceMediaResourceController;
    private boolean firstJoinSent = false;

    // Runtime media operations
    //private Boolean playing;
    //private Boolean recording;
    //private DateTime recordStarted;

    // Observers
    private final List<ActorRef> observers;

    private final ActorRef mrb;
    private String conferenceName;
    private Sid conferenceSid;
    private String conferenceEndpointIdName;

    private ConnectionMode connectionMode;

    //public MmsConferenceController(final List<ActorRef> mediaGateways, final Configuration configuration) {
    //public MmsConferenceController(final ActorRef mediaGateway) {
    public MmsConferenceController(final ActorRef mrb) {
        super();
        final ActorRef source = self();

        // Finite States
        this.uninitialized = new State("uninitialized", null, null);
        this.acquiringMediaGateway = new State("acquiring media gateway from mrb", new AcquiringMediaGateway(source), null);
        this.acquiringCnfMediaResourceController = new State("acquiring Cnf Media Resource Controller", new AcquiringCnfMediaResourceController(source), null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        this.acquiringEndpoint = new State("acquiring endpoint", new AcquiringEndpoint(source), null);
        //this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(source), null);
        this.stoppingCMRC = new State("stopping HA Conference Media Resource Controller", new StoppingCMRC(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);

        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringMediaGateway));
        transitions.add(new Transition(acquiringMediaGateway, acquiringMediaSession));
        transitions.add(new Transition(acquiringMediaSession, acquiringEndpoint));
        transitions.add(new Transition(acquiringMediaSession, inactive));
        transitions.add(new Transition(acquiringEndpoint, acquiringCnfMediaResourceController));
        transitions.add(new Transition(acquiringEndpoint, inactive));
        //transitions.add(new Transition(creatingMediaGroup, gettingCnfMediaResourceController));
        transitions.add(new Transition(acquiringCnfMediaResourceController, active));
        transitions.add(new Transition(acquiringCnfMediaResourceController, stoppingCMRC));
        transitions.add(new Transition(acquiringCnfMediaResourceController, failed));
        transitions.add(new Transition(active, stoppingCMRC));
        transitions.add(new Transition(stoppingCMRC, stopping));
        transitions.add(new Transition(stoppingCMRC, inactive));
        transitions.add(new Transition(stopping, inactive));
        transitions.add(new Transition(stopping, failed));

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        //this.fail = Boolean.FALSE;

        // MGCP runtime stuff
        //this.mediaGateway = mrb.getNextMediaServerKey();
        //this.mediaGateways = new MediaGateways(mediaGateways , configuration);
        this.mrb = mrb;

        // Runtime media operations
        //this.playing = Boolean.FALSE;
        //this.recording = Boolean.FALSE;

        // Observers
        this.observers = new ArrayList<ActorRef>(2);
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

    /*
     * EVENTS
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        final ActorRef self = self();
        final State state = fsm.state();

        if(logger.isInfoEnabled()) {
            logger.info(" ********** Conference Controller Current State: " + state.toString());
            logger.info(" ********** Conference Controller Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (CloseMediaSession.class.equals(klass)) {
            onCloseMediaSession((CloseMediaSession) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (Stop.class.equals(klass)) {
            onStop((Stop) message, self, sender);
        } /*else if (MediaGroupStateChanged.class.equals(klass)) {
            onMediaGroupStateChanged((MediaGroupStateChanged) message, self, sender);
        }*/  else if (JoinCall.class.equals(klass)) {
            onJoinCall((JoinCall) message, self, sender);
        } else if (Play.class.equals(klass) || StartRecording.class.equals(klass) || StopRecording.class.equals(klass)) {
            conferenceMediaResourceController.tell(message, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            StopMediaGroup msg = (StopMediaGroup)message;
            /* to media-server as media-server will automatically stop beep when it will receive
             * play command for beep. If a beep wont be played, then conference need to send
             * EndSignal(StopMediaGroup) to media-server to stop ongoing music-on-hold.
             * https://github.com/RestComm/Restcomm-Connect/issues/2024
             */
            if(!msg.beep()){
                conferenceMediaResourceController.tell(message, sender);
            }
        }else if(EndpointStateChanged.class.equals(klass)) {
            onEndpointStateChanged((EndpointStateChanged) message, self, sender);
        } else if (MediaResourceBrokerResponse.class.equals(klass)) {
            onMediaResourceBrokerResponse((MediaResourceBrokerResponse<?>) message, self, sender);
        } else if(JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if(ConferenceMediaResourceControllerStateChanged.class.equals(klass)) {
            onConferenceMediaResourceControllerStateChanged((ConferenceMediaResourceControllerStateChanged) message, self, sender);
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

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        if(logger.isInfoEnabled())
            logger.info("got JoinComplete in conference controller");
        if(!firstJoinSent){
            firstJoinSent = true;
            conferenceMediaResourceController.tell(message, self);
        }
    }

    private void onMediaResourceBrokerResponse(MediaResourceBrokerResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isInfoEnabled())
            logger.info("got MRB response in conference controller");
        if(is(acquiringMediaGateway)){
            MediaGatewayForConference mgc = (MediaGatewayForConference) message.get();
            mediaGateway = mgc.mediaGateway();
            this.conferenceSid = mgc.conferenceSid();
            // if this is master we would like to connect to master conference ep
            // for master's first join it would be null but if master left and rejoin it should join on same ep as received by mrb.
            // if this is not master conference ep should be null, so RMS would give us next available from pool.
            this.conferenceEndpointIdName = mgc.isThisMaster() ? mgc.masterConfernceEndpointIdName() : null;
            if(logger.isDebugEnabled())
                logger.debug("onMediaResourceBrokerResponse: "+mgc.toString());
            fsm.transition(message, acquiringMediaSession);
        }else if(is(acquiringCnfMediaResourceController)){
            conferenceMediaResourceController = (ActorRef) message.get();
            conferenceMediaResourceController.tell(new Observe(self), self);
            conferenceMediaResourceController.tell(new StartConferenceMediaResourceController(this.cnfEndpoint, this.conferenceSid), self);
        }
    }

    private void onConferenceMediaResourceControllerStateChanged(ConferenceMediaResourceControllerStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isDebugEnabled())
            logger.debug("onConferenceMediaResourceControllerStateChanged: "+message.state());
        switch (message.state()) {

            case ACTIVE:
                if (is(acquiringCnfMediaResourceController)) {
                    fsm.transition(message, active);
                }
                break;

            case FAILED:
                fsm.transition(message, failed);
                break;

            case INACTIVE:
                fsm.transition(message, stopping);
                break;

            default:
                break;
        }
    }

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            //this.conference = sender;
            fsm.transition(message, acquiringMediaGateway);
        }
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, inactive);
        } else {
            fsm.transition(message, stoppingCMRC);
        }
    }

    private void onStop(Stop message, ActorRef self, ActorRef sender) throws Exception {
        if (is(acquiringMediaSession) || is(acquiringEndpoint)) {
            this.fsm.transition(message, inactive);
        } else if (is(acquiringCnfMediaResourceController) || is(active)) {
            this.fsm.transition(message, stoppingCMRC);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        // XXX Check if message successful
        if (is(acquiringMediaSession)) {
            this.mediaSession = (MediaSession) message.get();
            this.fsm.transition(message, acquiringEndpoint);
        } else if (is(acquiringEndpoint)) {
            this.cnfEndpoint = (ActorRef) message.get();
            this.cnfEndpoint.tell(new Observe(self), self);
            this.fsm.transition(message, acquiringCnfMediaResourceController);
        }
    }

    /*private void onMediaGroupStateChanged(MediaGroupStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case ACTIVE:
                if (is(creatingMediaGroup)) {
                    fsm.transition(message, gettingCnfMediaResourceController);
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
                    if (this.mediaGroup == null && this.cnfEndpoint == null) {
                        this.fsm.transition(message, fail ? failed : inactive);
                    }
                }
                break;

            default:
                break;
        }
    }*/

    private void onJoinCall(JoinCall message, ActorRef self, ActorRef sender) {
        connectionMode = message.getConnectionMode();
        // Tell call to join conference by passing reference to the media mixer
        final JoinConference join = new JoinConference(this.cnfEndpoint, connectionMode, message.getSid());
        message.getCall().tell(join, sender);
    }

    /*private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            // Stop the primary media group
            this.mediaGroup.tell(new Stop(), self);
            this.playing = Boolean.FALSE;
        }
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (is(active) && !playing) {
            if (logger.isInfoEnabled()) {
                logger.info("Received Play message, isActive: "+is(active)+" , is playing: "+playing);
            }
            this.playing = Boolean.TRUE;
            this.mediaGroup.tell(message, self);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Received Play message but wont process it, isActive: "+is(active)+" , is playing: "+playing);
            }
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
        if (is(active) && this.playing) {
            this.playing = Boolean.FALSE;
        }
    }*/

    private void onEndpointStateChanged(EndpointStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if (is(stopping)) {
            if (sender.equals(this.cnfEndpoint) && (EndpointState.DESTROYED.equals(message.getState()) || EndpointState.FAILED.equals(message.getState()))) {
                if(EndpointState.FAILED.equals(message.getState()))
                    logger.error("Could not destroy endpoint on media server. corresponding actor path is: " + this.cnfEndpoint.path());
                this.cnfEndpoint.tell(new StopObserving(self), self);
                context().stop(cnfEndpoint);
                cnfEndpoint = null;

                if(this.cnfEndpoint == null) {
                    this.fsm.transition(message, inactive);
                }
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

    private final class AcquiringMediaGateway extends AbstractAction {

        public AcquiringMediaGateway(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            CreateMediaSession createMediaSession = (CreateMediaSession) message;
            String conferenceName = createMediaSession.conferenceName();
            mrb.tell(new GetMediaGateway(createMediaSession.callSid(), conferenceName, null), self());
        }
    }

    private final class AcquiringCnfMediaResourceController extends AbstractAction {

        public AcquiringCnfMediaResourceController(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("MMSConferenceController: GettingCnfMediaResourceController: conferenceName = "+conferenceName+" conferenceSid: "+conferenceSid+" cnfenpointID: "+cnfEndpoint);
            mrb.tell(new GetConferenceMediaResourceController(conferenceName), self());
        }
    }

    private final class AcquiringMediaSession extends AbstractAction {

        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new org.restcomm.connect.mgcp.CreateMediaSession(), super.source);
        }
    }

    private final class AcquiringEndpoint extends AbstractAction {

        public AcquiringEndpoint(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new CreateConferenceEndpoint(mediaSession, conferenceEndpointIdName), super.source);
        }
    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            ConferenceMediaResourceControllerStateChanged msg = (ConferenceMediaResourceControllerStateChanged)message;
            broadcast(new MediaServerConferenceControllerStateChanged(MediaServerControllerState.ACTIVE, conferenceSid, msg.conferenceState(), msg.moderatorPresent()));
        }
    }

    private final class StoppingCMRC extends AbstractAction {

        public StoppingCMRC(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled())
                logger.info("StoppingCMRC");
            conferenceMediaResourceController.tell(new StopConferenceMediaResourceController(), super.source);
        }
    }

    private final class Stopping extends AbstractAction {

        public Stopping(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            ConferenceMediaResourceControllerStateChanged response = (ConferenceMediaResourceControllerStateChanged) message;
            // CMRC might ask you not to destroy endpoint bcz master have left firt and other slaves are still connected to this conference endpoint.
            if(response.destroyEndpoint()){
                // Destroy Bridge Endpoint and its connections
                cnfEndpoint.tell(new DestroyEndpoint(), super.source);
            }else{
                if(logger.isInfoEnabled())
                    logger.info("CMRC have ask you not to destroy endpoint bcz master have left firt and other slaves are still connected to this conference endpoint");
                cnfEndpoint.tell(new StopObserving(self()), self());
                context().stop(cnfEndpoint);
                cnfEndpoint = null;

                if(cnfEndpoint == null) {
                    fsm.transition(message, inactive);
                }
            }
        }
    }

    private abstract class FinalState extends AbstractAction {

        private final MediaServerControllerState state;

        public FinalState(ActorRef source, final MediaServerControllerState state) {
            super(source);
            this.state = state;
        }

        @Override
        public void execute(Object message) throws Exception {
            // Cleanup resources
            if (cnfEndpoint != null) {
                mediaGateway.tell(new DestroyEndpoint(cnfEndpoint), super.source);
                cnfEndpoint = null;
            }

            if(conferenceMediaResourceController != null){
                context().stop(conferenceMediaResourceController);
                conferenceMediaResourceController = null;
            }

            // Notify observers the controller has stopped
            broadcast(new MediaServerConferenceControllerStateChanged(state, conferenceSid));

            // Clean observers
            observers.clear();

            // Terminate actor
            getContext().stop(super.source);
        }

    }

    private final class Inactive extends FinalState {

        public Inactive(final ActorRef source) {
            super(source, MediaServerControllerState.INACTIVE);
        }

    }

    private final class Failed extends FinalState {

        public Failed(final ActorRef source) {
            super(source, MediaServerControllerState.FAILED);
        }

    }

}

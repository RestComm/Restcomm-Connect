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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CreateConferenceEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinBridge;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Record;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartRecording;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.util.WavUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MmsBridgeController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State active;
    private final State acquiringMediaSession;
    private final State acquiringEndpoint;
    private final State creatingMediaGroup;
    private final State destroyingMediaGroup;
    private final State inactive;
    private final State failed;
    private Boolean fail;

    // MGCP runtime stuff
    private final ActorRef mediaGateway;
    private MediaSession mediaSession;
    private ActorRef endpoint;

    // Conference runtime stuff
    private ActorRef bridge;
    private ActorRef mediaGroup;

    // Call Recording
    private Boolean recording;
    private DateTime recordStarted;
    private StartRecording recordingRequest;

    // Observers
    private final List<ActorRef> observers;

    public MmsBridgeController(ActorRef mediaGateway) {
        final ActorRef self = self();

        // Finite states
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(self), null);
        this.acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(self), null);
        this.acquiringEndpoint = new State("acquiring endpoint", new AcquiringEndpoint(self), null);
        this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(self), null);
        this.destroyingMediaGroup = new State("destroying media group", new DestroyingMediaGroup(self), null);
        this.inactive = new State("inactive", new Inactive(self), null);
        this.failed = new State("failed", new Failed(self), null);

        // Finite State Machine
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringMediaSession));
        transitions.add(new Transition(acquiringMediaSession, acquiringEndpoint));
        transitions.add(new Transition(acquiringMediaSession, inactive));
        transitions.add(new Transition(acquiringEndpoint, creatingMediaGroup));
        transitions.add(new Transition(acquiringEndpoint, inactive));
        transitions.add(new Transition(creatingMediaGroup, active));
        transitions.add(new Transition(creatingMediaGroup, destroyingMediaGroup));
        transitions.add(new Transition(creatingMediaGroup, failed));
        transitions.add(new Transition(active, destroyingMediaGroup));
        transitions.add(new Transition(destroyingMediaGroup, inactive));
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        this.fail = Boolean.FALSE;

        // Media Components
        this.mediaGateway = mediaGateway;

        // Media Operations
        this.recording = Boolean.FALSE;

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

    private void saveRecording() {
        final Sid accountId = recordingRequest.getAccountId();
        final Sid callId = recordingRequest.getCallId();
        final DaoManager daoManager = recordingRequest.getDaoManager();
        final Sid recordingSid = recordingRequest.getRecordingSid();
        final URI recordingUri = recordingRequest.getRecordingUri();
        final Configuration runtimeSettings = recordingRequest.getRuntimeSetting();
        Double duration;

        try {
            duration = WavUtils.getAudioDuration(recordingUri);
        } catch (UnsupportedAudioFileException | IOException e) {
            logger.error("Could not measure recording duration: " + e.getMessage(), e);
            duration = 0.0;
        }

        if (duration.equals(0.0)) {
            logger.info("Call wraping up recording. File doesn't exist since duration is 0");
            final DateTime end = DateTime.now();
            duration = new Double((end.getMillis() - recordStarted.getMillis()) / 1000);
        } else {
            logger.info("Call wraping up recording. File already exists, length: " + (new File(recordingUri).length()));
        }

        final Recording.Builder builder = Recording.builder();
        builder.setSid(recordingSid);
        builder.setAccountSid(accountId);
        builder.setCallSid(callId);
        builder.setDuration(duration);
        builder.setApiVersion(runtimeSettings.getString("api-version"));
        StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(runtimeSettings.getString("api-version")).append("/Accounts/").append(accountId.toString());
        buffer.append("/Recordings/").append(recordingSid.toString());
        builder.setUri(URI.create(buffer.toString()));
        final Recording recording = builder.build();
        RecordingsDao recordsDao = daoManager.getRecordingsDao();
        recordsDao.addRecording(recording);
    }

    /*
     * Events
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();

        logger.info("********** Bridge Controller " + self().path() + " State: \"" + state.toString());
        logger.info("********** Bridge Controller " + self().path() + " Processing: \"" + klass.getName() + " Sender: "
                + sender.getClass());

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (JoinCall.class.equals(klass)) {
            onJoinCall((JoinCall) message, self, sender);
        } else if (Stop.class.equals(klass)) {
            onStop((Stop) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            onMediaGroupStateChanged((MediaGroupStateChanged) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecording((StartRecording) message, self, sender);
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

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.bridge = sender;
            this.fsm.transition(message, acquiringMediaSession);
        }
    }

    private void onJoinCall(JoinCall message, ActorRef self, ActorRef sender) {
        // Tell call to join bridge by passing reference to the media mixer
        final JoinBridge join = new JoinBridge(this.endpoint, message.getConnectionMode());
        message.getCall().tell(join, sender);
    }

    private void onStop(Stop message, ActorRef self, ActorRef sender) throws Exception {
        if (is(acquiringMediaSession) || is(acquiringEndpoint)) {
            this.fsm.transition(message, inactive);
        } else if (is(creatingMediaGroup) || is(active)) {
            this.fsm.transition(message, destroyingMediaGroup);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        // XXX Check if message successful
        if (is(acquiringMediaSession)) {
            this.mediaSession = (MediaSession) message.get();
            this.fsm.transition(message, acquiringEndpoint);
        } else if (is(acquiringEndpoint)) {
            this.endpoint = (ActorRef) message.get();
            this.fsm.transition(message, creatingMediaGroup);
        }
    }

    private void onMediaGroupStateChanged(MediaGroupStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case ACTIVE:
                if (is(creatingMediaGroup)) {
                    fsm.transition(message, active);
                }
                break;

            case INACTIVE:
                if (is(creatingMediaGroup)) {
                    this.fail = Boolean.TRUE;
                    fsm.transition(message, failed);
                } else if (is(destroyingMediaGroup)) {
                    if (fail) {
                        this.fsm.transition(message, failed);
                    } else {
                        // Save record info in the database
                        if (recordStarted != null) {
                            saveRecording();
                            recordStarted = null;
                            recordingRequest = null;
                        }
                        // Move to inactive state
                        this.fsm.transition(message, inactive);
                    }
                }
                break;

            default:
                break;
        }
    }

    private void onStartRecording(StartRecording message, ActorRef self, ActorRef sender) {
        if (is(active) && !recording) {
            logger.info("Start recording bridged call");
            String finishOnKey = "1234567890*#";
            int maxLength = 3600;
            int timeout = 5;

            this.recording = Boolean.TRUE;
            this.recordStarted = DateTime.now();
            this.recordingRequest = message;

            // Tell media group to start recording
            Record record = new Record(message.getRecordingUri(), timeout, maxLength, finishOnKey);
            this.mediaGroup.tell(record, self);
        }
    }

    /*
     * Actions
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

    private final class AcquiringEndpoint extends AbstractAction {

        public AcquiringEndpoint(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final CreateConferenceEndpoint createEndpoint = new CreateConferenceEndpoint(mediaSession);
            mediaGateway.tell(createEndpoint, super.source);
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
                    return new MgcpMediaGroup(mediaGateway, mediaSession, endpoint);
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

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            broadcast(new MediaServerControllerStateChanged(MediaServerControllerState.ACTIVE));
        }
    }

    private final class DestroyingMediaGroup extends AbstractAction {

        public DestroyingMediaGroup(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Stop Media Group
            // Note: Recording will be added to DB after getting response from MG
            if (recording) {
                mediaGroup.tell(new Stop(), super.source);
                recording = Boolean.FALSE;
            }

            // Destroy Media Group
            mediaGroup.tell(new StopMediaGroup(), super.source);
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
            if (endpoint != null) {
                mediaGateway.tell(new DestroyEndpoint(endpoint), super.source);
                endpoint = null;
            }

            // Notify observers the controller has stopped
            broadcast(new MediaServerControllerStateChanged(state));

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

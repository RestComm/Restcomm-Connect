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

package org.mobicents.servlet.restcomm.mscontrol.xms;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaEvent;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.mediagroup.SpeechDetectorConstants;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.AllocationEvent;
import javax.media.mscontrol.resource.AllocationEventListener;
import javax.media.mscontrol.resource.RTC;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.fsm.TransitionFailedException;
import org.mobicents.servlet.restcomm.fsm.TransitionNotFoundException;
import org.mobicents.servlet.restcomm.fsm.TransitionRollbackException;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerInfo;
import org.mobicents.servlet.restcomm.mscontrol.exceptions.MediaServerControllerException;
import org.mobicents.servlet.restcomm.mscontrol.messages.CloseMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.Collect;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.Join;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.Leave;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionClosed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionInfo;
import org.mobicents.servlet.restcomm.mscontrol.messages.Mute;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.QueryMediaMixer;
import org.mobicents.servlet.restcomm.mscontrol.messages.Record;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartRecordingCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopRecordingCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.Unmute;
import org.mobicents.servlet.restcomm.mscontrol.messages.UpdateMediaSession;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.util.WavUtils;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmsCallController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // FSM.
    private final FiniteStateMachine fsm;

    // FSM states
    private final State uninitialized;
    private final State active;
    private final State inactive;
    private final State failed;

    // Intermediate FSM states
    private final State openingMediaSession;
    private final State updatingMediaSession;

    // JSR-309 runtime stuff
    private final MsControlFactory msControlFactory;
    private final MediaServerInfo mediaServerInfo;
    private MediaSession mediaSession;
    private NetworkConnection networkConnection;
    private MediaGroup mediaGroup;
    private MediaMixer mediaMixer;

    private final SdpListener sdpListener;
    private final PlayerListener playerListener;
    private final DtmfListener dtmfListener;
    private final RecorderListener recorderListener;
    private final MixerAllocationListener mixerAllocationListener;

    // Call runtime stuff
    private ActorRef call;
    private Sid callId;
    private String localSdp;
    private String remoteSdp;
    private String connectionMode;
    private boolean callOutbound;

    // Conference runtime stuff
    private ActorRef conference;
    private ActorRef outboundController;
    private NetworkConnection outboundConnection;

    // Call Media Operations
    private Sid accountId;
    private Sid recordingSid;
    private URI recordingUri;
    private Boolean recording;
    private Boolean playing;
    private Boolean collecting;
    private DateTime recordStarted;
    private DaoManager daoManager;

    // Runtime Setting
    private Configuration runtimeSettings;

    // Observers
    private final List<ActorRef> observers;

    public XmsCallController(MsControlFactory msControlFactory, MediaServerInfo mediaServerInfo) {
        super();
        final ActorRef source = self();

        // JSR-309 runtime stuff
        this.msControlFactory = msControlFactory;
        this.mediaServerInfo = mediaServerInfo;
        this.sdpListener = new SdpListener();
        this.playerListener = new PlayerListener();
        this.dtmfListener = new DtmfListener();
        this.recorderListener = new RecorderListener();
        this.mixerAllocationListener = new MixerAllocationListener();

        // Initialize the states for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);
        this.failed = new State("failed", new Failed(source), null);

        // Intermediate FSM states
        this.openingMediaSession = new State("opening media session", new OpeningMediaSession(source), null);
        this.updatingMediaSession = new State("updating media session", new UpdatingMediaSession(source), null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, openingMediaSession));
        transitions.add(new Transition(openingMediaSession, failed));
        transitions.add(new Transition(openingMediaSession, active));
        transitions.add(new Transition(openingMediaSession, inactive));
        transitions.add(new Transition(active, updatingMediaSession));
        transitions.add(new Transition(active, inactive));
        transitions.add(new Transition(updatingMediaSession, active));
        transitions.add(new Transition(updatingMediaSession, inactive));
        transitions.add(new Transition(updatingMediaSession, failed));

        // Finite state machine
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);

        // Observers
        this.observers = new ArrayList<ActorRef>();

        // Call runtime stuff
        this.localSdp = "";
        this.remoteSdp = "";
        this.callOutbound = false;
        this.connectionMode = "inactive";
        this.recording = Boolean.FALSE;
        this.playing = Boolean.FALSE;
        this.collecting = Boolean.FALSE;
    }

    private boolean is(State state) {
        return fsm.state().equals(state);
    }

    private void notifyObservers(Object message, ActorRef self) {
        for (final ActorRef observer : observers) {
            observer.tell(message, self);
        }
    }

    /*
     * LISTENERS - MSCONTROL
     */
    private abstract class MediaListener<T extends MediaEvent<?>> implements MediaEventListener<T>, Serializable {

        private static final long serialVersionUID = 7103112381914312776L;

        protected ActorRef remote;

        public void setRemote(ActorRef sender) {
            this.remote = sender;
        }

    }

    private final class SdpListener extends MediaListener<SdpPortManagerEvent> {

        private static final long serialVersionUID = 1578203803932778931L;

        @Override
        public void onEvent(SdpPortManagerEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"SdpPortManagerEvent\" (type = " + eventType + ")");

            try {
                if (event.getError() == SdpPortManagerEvent.NO_ERROR) {
                    if (SdpPortManagerEvent.ANSWER_GENERATED.equals(eventType)
                            || SdpPortManagerEvent.OFFER_GENERATED.equals(eventType)) {
                        if (event.isSuccessful()) {
                            localSdp = new String(event.getMediaServerSdp());
                            fsm.transition(event, active);
                        } else {
                            fsm.transition(event, failed);
                        }
                    } else if (SdpPortManagerEvent.ANSWER_PROCESSED.equals(eventType)) {
                        fsm.transition(event, active);
                    } else if (SdpPortManagerEvent.NETWORK_STREAM_FAILURE.equals(eventType)) {
                        fsm.transition(event, failed);
                    }
                } else {
                    fsm.transition(event, failed);
                }
            } catch (TransitionFailedException | TransitionNotFoundException | TransitionRollbackException e) {
                logger.error(e, e.getMessage());
            }
        }

    }

    private final class PlayerListener extends MediaListener<PlayerEvent> {

        private static final long serialVersionUID = -1814168664061905439L;

        @Override
        public void onEvent(PlayerEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"PlayerEvent\" (type = " + eventType + ")");

            if (PlayerEvent.PLAY_COMPLETED.equals(eventType)) {
                MediaGroupResponse<String> response;
                if (event.isSuccessful()) {
                    response = new MediaGroupResponse<String>(eventType.toString());
                } else {
                    String reason = event.getErrorText();
                    MediaServerControllerException error = new MediaServerControllerException(reason);
                    response = new MediaGroupResponse<String>(error, reason);
                }
                playing = Boolean.FALSE;
                super.remote.tell(response, self());
            }
        }

    }

    private final class DtmfListener extends MediaListener<SignalDetectorEvent> {

        private static final long serialVersionUID = -96652040901361098L;

        @Override
        public void onEvent(SignalDetectorEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"SignalDetectorEvent\" (type = " + eventType + ")");

            if (SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED.equals(eventType)) {
                MediaGroupResponse<String> response;
                if (event.isSuccessful()) {
                    response = new MediaGroupResponse<String>(event.getSignalString());
                } else {
                    String reason = event.getErrorText();
                    MediaServerControllerException error = new MediaServerControllerException(reason);
                    response = new MediaGroupResponse<String>(error, reason);
                }
                collecting = Boolean.FALSE;
                super.remote.tell(response, self());
            }
        }

    }

    private final class RecorderListener extends MediaListener<RecorderEvent> {

        private static final long serialVersionUID = -8952464412809110917L;

        private String endOnKey = "";

        public void setEndOnKey(String endOnKey) {
            this.endOnKey = endOnKey;
        }

        @Override
        public void onEvent(RecorderEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"RecorderEvent\" (type = " + eventType + ")");

            if (RecorderEvent.RECORD_COMPLETED.equals(eventType)) {
                MediaGroupResponse<String> response = null;
                if (event.isSuccessful()) {
                    String digits = "";
                    if (RecorderEvent.STOPPED.equals(event.getQualifier())) {
                        digits = endOnKey;
                    }
                    response = new MediaGroupResponse<String>(digits);
                } else {
                    String reason = event.getErrorText();
                    MediaServerControllerException error = new MediaServerControllerException(reason);
                    logger.error("Recording event failed: " + reason);
                    response = new MediaGroupResponse<String>(error, reason);
                }
                recording = Boolean.FALSE;
                super.remote.tell(response, self());
            }
        }

    }

    private class MixerAllocationListener implements AllocationEventListener, Serializable {

        private static final long serialVersionUID = 6579306945384115627L;

        @Override
        public void onEvent(AllocationEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"AllocationEventListener - Mixer\" (type = " + eventType
                    + ")");

            if (AllocationEvent.ALLOCATION_CONFIRMED.equals(eventType)) {
                try {
                    // Can join resources safely
                    networkConnection.join(Direction.DUPLEX, mediaMixer);

                    // Notify remote peer that call can be bridged
                    // final JoinComplete response = new JoinComplete(mediaMixer);
                    // outboundController.tell(response, self());

                    // Ask the outbound controller its network connection
                    final QueryNetworkConnection query = new QueryNetworkConnection();
                    outboundController.tell(query, self());
                } catch (MsControlException e) {
                    // Notify observers that bridging failed
                    logger.error("Call bridging failed: " + e.getMessage());
                    final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                    notifyObservers(response, self());
                } finally {
                    // No need to be notified anymore
                    mediaMixer.removeListener(this);
                }
            } else if (AllocationEvent.IRRECOVERABLE_FAILURE.equals(eventType)) {
                logger.error("Can't enter conference...IRRECOVERABLE_FAILURE");

                // Media Mixer was not created
                mediaMixer.removeListener(this);
                mediaMixer = null;

                // Terminate Call
                call.tell(new MediaServerControllerError(), self());
            }
        }

    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();

        logger.info("********** Call Controller Current State: \"" + state.toString());
        logger.info("********** Call Controller Processing Message: \"" + klass.getName() + " sender : " + sender.getClass());

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (CloseMediaSession.class.equals(klass)) {
            onCloseMediaSession((CloseMediaSession) message, self, sender);
        } else if (UpdateMediaSession.class.equals(klass)) {
            onUpdateMediaSession((UpdateMediaSession) message, self, sender);
        } else if (CreateMediaGroup.class.equals(klass)) {
            onCreateMediaGroup((CreateMediaGroup) message, self, sender);
        } else if (DestroyMediaGroup.class.equals(klass)) {
            onDestroyMediaGroup((DestroyMediaGroup) message, self, sender);
        } else if (StartMediaGroup.class.equals(klass)) {
            onStartMediaGroup((StartMediaGroup) message, self, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            onStopMediaGroup((StopMediaGroup) message, self, sender);
        } else if (Mute.class.equals(klass)) {
            onMute((Mute) message, self, sender);
        } else if (Unmute.class.equals(klass)) {
            onUnmute((Unmute) message, self, sender);
        } else if (StartRecordingCall.class.equals(klass)) {
            onStartRecordingCall((StartRecordingCall) message, self, sender);
        } else if (StopRecordingCall.class.equals(klass)) {
            onStopRecordingCall((StopRecordingCall) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if (Collect.class.equals(klass)) {
            onCollect((Collect) message, self, sender);
        } else if (Record.class.equals(klass)) {
            onRecord((Record) message, self, sender);
        } else if (Join.class.equals(klass)) {
            onJoin((Join) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (Leave.class.equals(klass)) {
            onLeave((Leave) message, self, sender);
        } else if (MediaServerControllerResponse.class.equals(klass)) {
            onMediaServerControllerResponse((MediaServerControllerResponse<?>) message, self, sender);
        } else if (QueryNetworkConnection.class.equals(klass)) {
            onQueryNetworkConnection((QueryNetworkConnection) message, self, sender);
        } else if (Stop.class.equals(klass)) {
            onStop((Stop) message, self, sender);
        }
    }

    private void onObserve(Observe message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = message.observer();
        if (observer != null) {
            synchronized (this.observers) {
                this.observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.remove(observer);
        } else {
            this.observers.clear();
        }
    }

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.call = sender;
            this.callOutbound = message.isOutbound();
            this.connectionMode = message.getConnectionMode();
            this.remoteSdp = message.getSessionDescription();

            fsm.transition(message, openingMediaSession);
        }
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) || is(openingMediaSession) || is(updatingMediaSession)) {
            fsm.transition(message, inactive);
        }
    }

    private void onUpdateMediaSession(UpdateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            this.remoteSdp = message.getSessionDescription();
            fsm.transition(message, updatingMediaSession);
        }
    }

    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        // Always reuse current media group if active
        if (this.mediaGroup == null) {
            // Create new media group
            this.mediaGroup = this.mediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);

            // Prepare the Media Group resources
            this.mediaGroup.getPlayer().addListener(this.playerListener);
            this.mediaGroup.getSignalDetector().addListener(this.dtmfListener);
            this.mediaGroup.getRecorder().addListener(this.recorderListener);
        }

        // XXX should send a MediaGroupCreated message, not the ActorRef (part of VI refactoring)
        sender.tell(new MediaServerControllerResponse<ActorRef>(self), self);
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) {
        if (this.mediaGroup != null) {
            this.mediaGroup.release();
            this.mediaGroup = null;
        }
    }

    private void onStartMediaGroup(StartMediaGroup message, ActorRef self, ActorRef sender) throws MsControlException {
        // Join network connection to audio media group
        if (this.mediaGroup != null) {
            this.networkConnection.join(Direction.DUPLEX, this.mediaGroup);
        }

        // Tell observers the media group has been created
        final MediaGroupStateChanged response = new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE);
        notifyObservers(response, self);
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) throws MsControlException {
        try {
            if (this.mediaGroup != null) {
                // XXX mediaGroup.stop() not implemented on dialogic connector
                if (this.playing) {
                    this.mediaGroup.getPlayer().stop(true);
                    this.playing = Boolean.FALSE;
                }

                if (this.recording) {
                    this.mediaGroup.getRecorder().stop();
                    this.recording = Boolean.FALSE;
                }

                if (this.collecting) {
                    this.mediaGroup.getSignalDetector().stop();
                    this.collecting = Boolean.FALSE;
                }

                // Disconnect from connection
                this.mediaGroup.unjoin(this.networkConnection);
            }

            // Tell observers the media group has been created
            final MediaGroupStateChanged response = new MediaGroupStateChanged(MediaGroupStateChanged.State.INACTIVE);
            notifyObservers(response, self);
        } catch (MsControlException e) {
            call.tell(new MediaServerControllerError(e), self);
        }
    }

    private void onMute(Mute message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            try {
                if (this.mediaMixer != null) {
                    this.networkConnection.join(Direction.RECV, this.mediaMixer);
                }
            } catch (MsControlException e) {
                logger.error("Could not mute call: " + e.getMessage(), e);
            }
        }
    }

    private void onUnmute(Unmute message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            try {
                if (this.mediaMixer != null) {
                    this.networkConnection.join(Direction.DUPLEX, this.mediaMixer);
                }
            } catch (MsControlException e) {
                logger.error("Could not unmute call: " + e.getMessage(), e);
            }
        }
    }

    private void onStartRecordingCall(StartRecordingCall message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            if (runtimeSettings == null) {
                this.runtimeSettings = message.getRuntimeSetting();
            }

            if (daoManager == null) {
                daoManager = message.getDaoManager();
            }

            if (accountId == null) {
                accountId = message.getAccountId();
            }

            this.callId = message.getCallId();
            this.recordingSid = message.getRecordingSid();
            this.recordingUri = message.getRecordingUri();
            this.recording = true;

            logger.info("Start recording call");
            this.recordStarted = DateTime.now();

            // Tell media group to start recording
            final Record record = new Record(recordingUri, 5, 3600, "1234567890*#");
            onRecord(record, self, sender);
        }
    }

    private void onStopRecordingCall(StopRecordingCall message, ActorRef self, ActorRef sender) {
        if (is(active) && recording) {
            if (runtimeSettings == null) {
                this.runtimeSettings = message.getRuntimeSetting();
            }

            if (daoManager == null) {
                this.daoManager = message.getDaoManager();
            }

            if (accountId == null) {
                this.accountId = message.getAccountId();
            }

            // Tell media group to stop recording
            logger.info("Stop recording call");
            onStop(new Stop(false), self, sender);
        }
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            try {
                List<URI> uris = message.uris();
                Parameters params = this.mediaGroup.createParameters();
                int repeatCount = message.iterations() <= 0 ? Player.FOREVER : message.iterations() - 1;
                params.put(Player.REPEAT_COUNT, repeatCount);
                this.playerListener.setRemote(sender);
                this.mediaGroup.getPlayer().play(uris.toArray(new URI[uris.size()]), RTC.NO_RTC, params);
                this.playing = Boolean.TRUE;
            } catch (MsControlException e) {
                logger.error("Play failed: " + e.getMessage());
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                notifyObservers(response, self);
            }
        }
    }

    private void onCollect(Collect message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            try {
                Parameters optargs = this.mediaGroup.createParameters();

                // Add patterns to the detector
                List<Parameter> patterns = new ArrayList<Parameter>(2);
                if (message.hasEndInputKey()) {
                    optargs.put(SignalDetector.PATTERN[0], message.endInputKey());
                    patterns.add(SignalDetector.PATTERN[0]);
                }

                if (message.hasPattern()) {
                    optargs.put(SignalDetector.PATTERN[1], message.pattern());
                    patterns.add(SignalDetector.PATTERN[1]);
                }

                Parameter[] patternArray = null;
                if (!patterns.isEmpty()) {
                    patternArray = patterns.toArray(new Parameter[patterns.size()]);
                }

                // Setup enabled events
                EventType[] enabledEvents = { SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED };
                optargs.put(SignalDetector.ENABLED_EVENTS, enabledEvents);

                // Setup prompts
                if (message.hasPrompts()) {
                    List<URI> prompts = message.prompts();
                    optargs.put(SignalDetector.PROMPT, prompts.toArray(new URI[prompts.size()]));
                }

                // Setup time out interval
                int timeout = message.timeout();
                optargs.put(SignalDetector.INITIAL_TIMEOUT, timeout);
                optargs.put(SignalDetector.INTER_SIG_TIMEOUT, timeout);

                // Disable buffering for performance gain
                optargs.put(SignalDetector.BUFFERING, false);

                this.dtmfListener.setRemote(sender);
                this.mediaGroup.getSignalDetector().flushBuffer();
                this.mediaGroup.getSignalDetector().receiveSignals(message.numberOfDigits(), patternArray, RTC.NO_RTC, optargs);
                this.collecting = Boolean.TRUE;
            } catch (MsControlException e) {
                logger.error("DTMF recognition failed: " + e.getMessage());
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                notifyObservers(response, self);
            }
        }
    }

    private void onRecord(Record message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            try {
                Parameters params = this.mediaGroup.createParameters();

                // Add prompts
                if (message.hasPrompts()) {
                    List<URI> prompts = message.prompts();
                    // TODO JSR-309 connector still does not support multiple prompts
                    // params.put(Recorder.PROMPT, prompts.toArray(new URI[prompts.size()]));
                    params.put(Recorder.PROMPT, prompts.get(0));
                }

                // Finish on key
                RTC[] rtcs;
                if (message.hasEndInputKey()) {
                    params.put(SignalDetector.PATTERN[0], message.endInputKey());
                    params.put(SignalDetector.INTER_SIG_TIMEOUT, new Integer(10000));
                    rtcs = new RTC[] { MediaGroup.SIGDET_STOPPLAY };
                } else {
                    rtcs = RTC.NO_RTC;
                }

                // Recording length
                params.put(Recorder.MAX_DURATION, message.length() * 1000);

                // Recording timeout
                int timeout = message.timeout();
                params.put(SpeechDetectorConstants.INITIAL_TIMEOUT, timeout);
                params.put(SpeechDetectorConstants.FINAL_TIMEOUT, timeout);

                // Other parameters
                params.put(Recorder.APPEND, Boolean.FALSE);
                // TODO set as definitive media group parameter - handled by RestComm
                params.put(Recorder.START_BEEP, Boolean.FALSE);

                this.recorderListener.setEndOnKey(message.endInputKey());
                this.recorderListener.setRemote(sender);
                this.mediaGroup.getRecorder().record(message.destination(), rtcs, params);
                this.recording = Boolean.TRUE;
            } catch (MsControlException e) {
                logger.error("Recording failed: " + e.getMessage());
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                notifyObservers(response, self);
            }
        }
    }

    private void onJoin(Join message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            // Ask the remote media session controller for the bridge endpoint
            this.conference = message.endpoint();
            this.outboundController = message.mscontroller();

            try {
                // Prepare for new call configuration
                if (this.mediaGroup != null) {
                    this.mediaGroup.release();
                    this.mediaGroup = null;
                }

                if (this.mediaMixer != null) {
                    this.mediaMixer.release();
                }

                if (ConnectionMode.Confrnce.equals(message.mode())) {
                    /* CONFERENCING - conference already owns media mixer */
                    // Ask conference controller what is the media mixer so the call can join
                    QueryMediaMixer query = new QueryMediaMixer();
                    outboundController.tell(query, self);
                } else {
                    /* CALL BRIDGING - no media mixer has been created yet */
                    // Create Mixer and join connection to it
                    Parameters mixerParams = this.mediaSession.createParameters();
                    // Limit number of ports for the two bridged participants and possible media group
                    // TODO Check whether recording=true so max_ports is 2 or 3
                    mixerParams.put(MediaMixer.MAX_PORTS, 3);
                    this.mediaMixer = this.mediaSession.createMediaMixer(MediaMixer.AUDIO, mixerParams);
                    this.mediaMixer.addListener(this.mixerAllocationListener);

                    // Wait for Media Mixer to initialize
                    // Connection will join mixer on allocation event
                    this.mediaMixer.confirm();
                }
            } catch (MsControlException e) {
                logger.error("Call bridging failed: " + e.getMessage());
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                notifyObservers(response, self);
            }
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            // Get the media mixer of the bridge
            this.mediaMixer = (MediaMixer) message.endpoint();

            // attach the media group to the media mixer
            MediaGroupStateChanged.State mediaGroupState;
            try {
                // this.mediaGroup.unjoin(this.networkConnection);
                this.mediaGroup.join(Direction.DUPLEX, this.mediaMixer);
                mediaGroupState = MediaGroupStateChanged.State.ACTIVE;
            } catch (MsControlException e) {
                logger.error("Could not join media group to media mixer: " + e.getMessage(), e);
                this.mediaGroup.release();
                this.mediaGroup = null;
                mediaGroupState = MediaGroupStateChanged.State.INACTIVE;
            }

            // Warn observers that media group state changed
            final MediaGroupStateChanged response = new MediaGroupStateChanged(mediaGroupState);
            notifyObservers(response, self);
        }
    }

    private void onLeave(Leave message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, inactive);
        }
    }

    private void onMediaServerControllerResponse(MediaServerControllerResponse<?> message, ActorRef self, ActorRef sender) {
        Object obj = message.get();

        if (obj instanceof NetworkConnection) {
            try {
                // Complete bridging process
                this.outboundConnection = (NetworkConnection) obj;
                this.outboundConnection.join(Direction.DUPLEX, this.mediaMixer);

                // Warn call that bridging process completed
                final JoinComplete joinComplete = new JoinComplete(this.mediaMixer);
                this.call.tell(new MediaServerControllerResponse<JoinComplete>(joinComplete), self);
            } catch (MsControlException e) {
                logger.error("Call bridging failed: " + e.getMessage());
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                notifyObservers(response, self);
            }
        } else if (obj instanceof MediaMixer) {
            try {
                // Complete joining process
                this.mediaMixer = (MediaMixer) obj;
                this.networkConnection.join(Direction.DUPLEX, this.mediaMixer);

                // alert conference call has joined successfully
                final JoinComplete joinComplete = new JoinComplete(this.networkConnection);
                this.call.tell(new MediaServerControllerResponse<JoinComplete>(joinComplete), self);
            } catch (MsControlException e) {
                logger.error("Could not join call to conference: " + e.getMessage());
                MediaServerControllerError error = new MediaServerControllerError(e);
                call.tell(error, self);
            }
        }

    }

    private void onQueryNetworkConnection(QueryNetworkConnection message, ActorRef self, ActorRef sender) {
        sender.tell(new MediaServerControllerResponse<NetworkConnection>(this.networkConnection), self);
    }

    private void onStop(Stop message, ActorRef self, ActorRef sender) {
        try {
            // XXX mediaGroup.stop() not implemented on dialogic connector
            if (this.playing) {
                this.mediaGroup.getPlayer().stop(true);
                this.playing = Boolean.FALSE;
            }

            if (this.recording) {
                this.mediaGroup.getRecorder().stop();
                this.recording = Boolean.FALSE;

                if (message.createRecord() && recordingUri != null) {
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
                        logger.info("Call wraping up recording. File already exists, length: "
                                + (new File(recordingUri).length()));
                    }
                    final Recording.Builder builder = Recording.builder();
                    builder.setSid(recordingSid);
                    builder.setAccountSid(accountId);
                    builder.setCallSid(callId);
                    builder.setDuration(duration);
                    builder.setApiVersion(runtimeSettings.getString("api-version"));
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("/").append(runtimeSettings.getString("api-version")).append("/Accounts/")
                            .append(accountId.toString());
                    buffer.append("/Recordings/").append(recordingSid.toString());
                    builder.setUri(URI.create(buffer.toString()));
                    final Recording recording = builder.build();
                    RecordingsDao recordsDao = daoManager.getRecordingsDao();
                    recordsDao.addRecording(recording);
                }
            }

            if (this.collecting) {
                this.mediaGroup.getSignalDetector().stop();
                this.collecting = Boolean.FALSE;
            }
        } catch (MsControlException e) {
            call.tell(new MediaServerControllerError(e), self);
        }
    }

    /*
     * ACTIONS
     */
    private final class OpeningMediaSession extends AbstractAction {

        public OpeningMediaSession(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            try {
                // Create media session
                mediaSession = msControlFactory.createMediaSession();

                // Create network connection
                networkConnection = mediaSession.createNetworkConnection(NetworkConnection.BASIC);
                networkConnection.getSdpPortManager().addListener(sdpListener);
                if (callOutbound) {
                    networkConnection.getSdpPortManager().generateSdpOffer();
                } else {
                    networkConnection.getSdpPortManager().processSdpOffer(remoteSdp.getBytes());
                }
            } catch (MsControlException e) {
                // XXX Move to failing state
                final MediaServerControllerError response = new MediaServerControllerError(e);
                sender().tell(new MediaServerControllerResponse<MediaServerControllerError>(response), super.source);
            }
        }

    }

    private final class UpdatingMediaSession extends AbstractAction {

        public UpdatingMediaSession(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            try {
                networkConnection.getSdpPortManager().processSdpAnswer(remoteSdp.getBytes());
            } catch (MsControlException e) {
                // XXX Move to failing state
                final MediaServerControllerError response = new MediaServerControllerError(e);
                sender().tell(new MediaServerControllerResponse<MediaServerControllerError>(response), super.source);
            }
        }

    }

    private final class Active extends AbstractAction {

        public Active(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final MediaSessionInfo info = new MediaSessionInfo(true, mediaServerInfo.getAddress(), localSdp, remoteSdp);
            call.tell(new MediaServerControllerResponse<MediaSessionInfo>(info), super.source);
        }

    }

    private final class Inactive extends AbstractAction {

        public Inactive(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
                mediaGroup = null;
            }

            // Inform call that media session has been properly closed
            final MediaSessionClosed response = new MediaSessionClosed();
            call.tell(new MediaServerControllerResponse<MediaSessionClosed>(response), super.source);
        }

    }

    private final class Failed extends AbstractAction {

        public Failed(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {

            if (message instanceof SdpPortManagerEvent) {
                SdpPortManagerEvent event = (SdpPortManagerEvent) message;
                logger.warning("XMS returned error: " + event.getErrorText() + ". Failing call...");
            }

            // Inform call the media session could not be set up
            final MediaServerControllerError error = new MediaServerControllerError();
            call.tell(error, super.source);
        }

    }

}

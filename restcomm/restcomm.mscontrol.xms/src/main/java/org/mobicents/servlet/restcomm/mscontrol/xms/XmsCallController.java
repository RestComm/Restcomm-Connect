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
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.RTC;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
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
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionClosed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionInfo;
import org.mobicents.servlet.restcomm.mscontrol.messages.Mute;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.Unmute;
import org.mobicents.servlet.restcomm.mscontrol.messages.UpdateMediaSession;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

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
    private final State muting;
    private final State unmuting;

    // JSR-309 runtime stuff
    private final MsControlFactory msControlFactory;
    private final MediaServerInfo mediaServerInfo;
    private MediaSession mediaSession;
    private NetworkConnection networkConnection;
    private MediaGroup mediaGroup;
    private final SdpListener sdpListener;
    private final PlayerListener playerListener;

    // Call runtime stuff
    private ActorRef call;
    private Sid callId;
    private String localSdp;
    private String remoteSdp;
    private String connectionMode;
    private boolean callOutbound;

    // Conference runtime stuff
    private ActorRef conference;
    private ActorRef conferenceController;
    private ActorRef outboundCallBridgeEndpoint;

    // Call Recording
    private Sid accountId;
    private Sid recordingSid;
    private URI recordingUri;
    private Boolean recording;
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

        // Initialize the states for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);
        this.failed = new State("failed", new Failed(source), null);

        // Intermediate FSM states
        this.openingMediaSession = new State("opening media session", new OpeningMediaSession(source), null);
        this.updatingMediaSession = new State("updating media session", new UpdatingMediaSession(source), null);
        // TODO initialize muting/unmuting states
        this.muting = new State("muting", null, null);
        this.unmuting = new State("unmuting", null, null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, openingMediaSession));
        transitions.add(new Transition(openingMediaSession, failed));
        transitions.add(new Transition(openingMediaSession, active));
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
            logger.info("********** Call Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Call Controller Processing Event: \"" + SdpPortManagerEvent.class.getName() + "\"");

            try {
                if (event.getError() == SdpPortManagerEvent.NO_ERROR) {
                    EventType eventType = event.getEventType();
                    if (SdpPortManagerEvent.ANSWER_GENERATED.equals(eventType)) {
                        if (event.isSuccessful()) {
                            localSdp = new String(event.getMediaServerSdp());
                            fsm.transition(event, active);
                        } else {
                            fsm.transition(event, failed);
                        }
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

            if (PlayerEvent.PLAY_COMPLETED.equals(eventType)) {
                MediaGroupResponse<String> response;
                if (event.isSuccessful()) {
                    response = new MediaGroupResponse<String>("play_completed");
                } else {
                    String reason = event.getErrorText();
                    MediaServerControllerException error = new MediaServerControllerException(reason);
                    response = new MediaGroupResponse<String>(error, reason);
                }
                super.remote.tell(response, self());
            }
        }

    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        Class<?> klass = message.getClass();
        ActorRef self = getSelf();
        ActorRef sender = getSender();

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
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
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
        // Release existing media group if any
        if (this.mediaGroup != null) {
            this.mediaGroup.release();
        }

        // Create new media group
        this.mediaGroup = this.mediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);

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
            this.networkConnection.join(Direction.DUPLEX, mediaGroup);
        }

        // Tell observers the media group has been created
        final MediaGroupStateChanged response = new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE);
        notifyObservers(response, self);
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) throws MsControlException {
        // Disconnect network connection from audio media group
        if (this.mediaGroup != null) {
            this.networkConnection.unjoin(mediaGroup);
        }

        // Tell observers the media group has been created
        final MediaGroupStateChanged response = new MediaGroupStateChanged(MediaGroupStateChanged.State.INACTIVE);
        notifyObservers(response, self);
    }

    private void onMute(Mute message, ActorRef self, ActorRef sender) throws Exception {
        // TODO ask JSR driver to mute
    }

    private void onUnmute(Unmute message, ActorRef self, ActorRef sender) throws Exception {
        // TODO ask JSR driver to unmute
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) throws MsControlException {
        if (is(active)) {
            List<URI> uris = message.uris();
            Parameters params = this.mediaGroup.createParameters();
            params.put(Player.REPEAT_COUNT, message.iterations());
            this.playerListener.setRemote(sender);
            this.mediaGroup.getPlayer().play(uris.toArray(new URI[uris.size()]), RTC.NO_RTC, Parameters.NO_PARAMETER);
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
                networkConnection.getSdpPortManager().processSdpOffer(remoteSdp.getBytes());
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
            if (mediaGroup != null) {
                mediaGroup.release();
                mediaGroup = null;
            }

            if (networkConnection != null) {
                networkConnection.release();
                networkConnection = null;
            }

            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
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

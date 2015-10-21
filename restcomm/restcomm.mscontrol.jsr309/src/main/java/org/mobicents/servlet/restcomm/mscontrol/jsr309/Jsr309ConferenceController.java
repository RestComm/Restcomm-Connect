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

package org.mobicents.servlet.restcomm.mscontrol.jsr309;

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
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.resource.AllocationEvent;
import javax.media.mscontrol.resource.AllocationEventListener;
import javax.media.mscontrol.resource.RTC;

import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.fsm.TransitionFailedException;
import org.mobicents.servlet.restcomm.fsm.TransitionNotFoundException;
import org.mobicents.servlet.restcomm.fsm.TransitionRollbackException;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerInfo;
import org.mobicents.servlet.restcomm.mscontrol.exceptions.MediaServerControllerException;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinConference;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
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
public class Jsr309ConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;

    // Finite states
    private final State uninitialized;
    private final State initializing;
    private final State active;
    private final State inactive;
    private final State failed;

    // Conference runtime stuff
    private ActorRef conference;

    // JSR-309 runtime stuff
    private final MsControlFactory msControlFactory;
    private final MediaServerInfo mediaServerInfo;
    private MediaSession mediaSession;
    private MediaGroup mediaGroup;
    private MediaMixer mediaMixer;

    private final PlayerListener playerListener;
    private final MixerAllocationListener mixerAllocationListener;

    // Media Operations
    private Boolean playing;

    // Observers
    private final List<ActorRef> observers;

    public Jsr309ConferenceController(MsControlFactory msControlFactory, MediaServerInfo mediaServerInfo) {
        super();
        final ActorRef source = self();

        // JSR-309 runtime stuff
        this.msControlFactory = msControlFactory;
        this.mediaServerInfo = mediaServerInfo;

        this.playerListener = new PlayerListener();
        this.mixerAllocationListener = new MixerAllocationListener();

        // Media Operations
        this.playing = Boolean.FALSE;

        // Initialize the states for the FSM
        this.uninitialized = new State("uninitialized", null);
        this.initializing = new State("initializing", new Initializing(source));
        this.active = new State("active", new Active(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializing));
        transitions.add(new Transition(initializing, failed));
        transitions.add(new Transition(initializing, active));
        transitions.add(new Transition(initializing, inactive));
        transitions.add(new Transition(active, inactive));

        // Finite state machine
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);

        // Observers
        this.observers = new ArrayList<ActorRef>();
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

    private void stopMediaOperations() throws MsControlException {
        // Stop ongoing recording
        if (playing) {
            mediaGroup.getPlayer().stop(true);
            this.playing = Boolean.FALSE;
        }
    }

    private void cleanMediaResources() {
        // Release media resources
        mediaSession.release();
        mediaSession = null;
        mediaGroup = null;
        mediaMixer = null;
    }

    /*
     * JSR-309 - EVENT LISTENERS
     */
    private abstract class MediaListener<T extends MediaEvent<?>> implements MediaEventListener<T>, Serializable {

        private static final long serialVersionUID = 4712964810787577487L;

        protected ActorRef remote;

        public void setRemote(ActorRef sender) {
            this.remote = sender;
        }

    }

    private final class PlayerListener extends MediaListener<PlayerEvent> {

        private static final long serialVersionUID = 391225908543565230L;

        @Override
        public void onEvent(PlayerEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Conference Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Conference Controller Processing Event: \"PlayerEvent\" (type = " + eventType + ")");

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

    private class MixerAllocationListener implements AllocationEventListener, Serializable {

        private static final long serialVersionUID = 6579306945384115627L;

        @Override
        public void onEvent(AllocationEvent event) {
            EventType eventType = event.getEventType();

            logger.info("********** Conference Controller Current State: \"" + fsm.state().toString() + "\"");
            logger.info("********** Conference Controller Processing Event: \"AllocationEventListener - Mixer\" (type = "
                    + eventType + ")");

            try {
                if (AllocationEvent.ALLOCATION_CONFIRMED.equals(eventType)) {
                    // No need to be notified anymore
                    mediaMixer.removeListener(this);

                    // join the media group to the mixer
                    try {
                        mediaGroup.join(Direction.DUPLEX, mediaMixer);
                    } catch (MsControlException e) {
                        fsm.transition(e, failed);
                    }

                    // Conference room has been properly activated and is now ready to receive connections
                    fsm.transition(event, active);
                } else if (AllocationEvent.IRRECOVERABLE_FAILURE.equals(eventType)) {
                    // Failed to activate media session
                    fsm.transition(event, failed);
                }
            } catch (TransitionFailedException | TransitionNotFoundException | TransitionRollbackException e) {
                logger.error(e.getMessage(), e);
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

        logger.info("********** Conference Controller Current State: \"" + state.toString());
        logger.info("********** Conference Controller Processing Message: \"" + klass.getName() + " sender : "
                + sender.getClass());

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (Stop.class.equals(klass)) {
            onStop((Stop) message, self, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            onStopMediaGroup((StopMediaGroup) message, self, sender);
        } else if (JoinCall.class.equals(klass)) {
            onJoinCall((JoinCall) message, self, sender);
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
            this.conference = sender;
            fsm.transition(message, initializing);
        }
    }

    private void onStop(Stop message, ActorRef self, ActorRef sender) throws Exception {
        if (is(initializing) || is(active)) {
            this.fsm.transition(message, inactive);
        }
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            try {
                if (this.playing) {
                    this.mediaGroup.getPlayer().stop(true);
                    this.playing = Boolean.FALSE;
                }
            } catch (MsControlException e) {
                conference.tell(new MediaServerControllerError(e), self);
            }
        }
    }

    private void onJoinCall(JoinCall message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            // Tell call to join conference by passing reference of the media mixer
            final JoinConference join = new JoinConference(this.mediaMixer, message.getConnectionMode());
            message.getCall().tell(join, sender);
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
                this.playing = Boolean.FALSE;
                final MediaGroupResponse<String> response = new MediaGroupResponse<String>(e);
                broadcast(response);
            }
        }
    }

    /*
     * ACTIONS
     */
    private final class Initializing extends AbstractAction {

        public Initializing(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            try {
                // Create media session
                mediaSession = msControlFactory.createMediaSession();

                // Create the media group with recording capabilities
                mediaGroup = mediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);

                // Set default conference video resolution to 720p
                // mediaSession.setAttribute("CONFERENCE_VIDEO_SIZE", "720p");

                // Set number of ports for the available participants and possible media group
                Parameters mixerParams = mediaSession.createParameters();
                mixerParams.put(MediaMixer.MAX_PORTS, 10);

                // Create the conference room also
                mediaMixer = mediaSession.createMediaMixer(MediaMixer.AUDIO_VIDEO, mixerParams);
                mediaMixer.addListener(mixerAllocationListener);
                mediaMixer.confirm();
                // Wait for event confirmation before sending response to the conference
            } catch (MsControlException e) {
                // Move to a failed state, cleaning all resources and closing media session
                fsm.transition(e, failed);
            }
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

    private final class Inactive extends AbstractAction {

        public Inactive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            try {
                stopMediaOperations();
            } catch (MsControlException e) {
                logger.error(e, "Could not stop ongoing media operations in an elegant manner");
            }
            cleanMediaResources();

            // Broadcast state changed
            broadcast(new MediaServerControllerStateChanged(MediaServerControllerState.INACTIVE));

            // Clear observers
            observers.clear();

            // Terminate actor
            getContext().stop(super.source);
        }
    }

    private final class Failed extends AbstractAction {

        public Failed(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Clean resources
            cleanMediaResources();

            // Broadcast state changed
            broadcast(new MediaServerControllerStateChanged(MediaServerControllerState.FAILED));

            // Clear observers
            observers.clear();

            // Terminate actor
            getContext().stop(super.source);
        }

    }

}

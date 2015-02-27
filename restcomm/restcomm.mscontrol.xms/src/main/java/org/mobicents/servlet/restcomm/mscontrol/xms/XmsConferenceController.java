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
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.AllocationEvent;
import javax.media.mscontrol.resource.AllocationEventListener;

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
import org.mobicents.servlet.restcomm.mscontrol.messages.QueryMediaMixer;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
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
public class XmsConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;

    // Finite states
    private final State uninitialized;
    private final State active;
    private final State inactive;
    private final State failed;

    // Intermediate states
    private final State openingMediaSession;

    // Conference runtime stuff
    private ActorRef conference;

    // JSR-309 runtime stuff
    private final MsControlFactory msControlFactory;
    private final MediaServerInfo mediaServerInfo;
    private MediaSession mediaSession;
    private NetworkConnection networkConnection;
    private MediaGroup mediaGroup;
    private MediaMixer mediaMixer;

    private final PlayerListener playerListener;
    private final MixerAllocationListener mixerAllocationListener;

    // Observers
    private final List<ActorRef> observers;

    public XmsConferenceController(MsControlFactory msControlFactory, MediaServerInfo mediaServerInfo) {
        super();
        final ActorRef source = self();

        // JSR-309 runtime stuff
        this.msControlFactory = msControlFactory;
        this.mediaServerInfo = mediaServerInfo;

        this.playerListener = new PlayerListener();
        this.mixerAllocationListener = new MixerAllocationListener();

        // Initialize the states for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);
        this.failed = new State("failed", new Failed(source), null);

        // Intermediate FSM states
        this.openingMediaSession = new State("opening media session", new OpeningMediaSession(source), null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, openingMediaSession));
        transitions.add(new Transition(openingMediaSession, failed));
        transitions.add(new Transition(openingMediaSession, active));
        transitions.add(new Transition(openingMediaSession, inactive));
        transitions.add(new Transition(active, inactive));

        // Finite state machine
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);

        // Observers
        this.observers = new ArrayList<ActorRef>();
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private void notifyObservers(Object message, ActorRef self) {
        for (final ActorRef observer : observers) {
            observer.tell(message, self);
        }
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
        } else if (CreateMediaGroup.class.equals(klass)) {
            onCreateMediaGroup((CreateMediaGroup) message, self, sender);
        } else if (StartMediaGroup.class.equals(klass)) {
            onStartMediaGroup((StartMediaGroup) message, self, sender);
        } else if (DestroyMediaGroup.class.equals(klass)) {
            onDestroyMediaGroup((DestroyMediaGroup) message, self, sender);
        } else if (QueryMediaMixer.class.equals(klass)) {
            onQueryMediaMixer((QueryMediaMixer) message, self, sender);
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
        this.conference = sender;
        fsm.transition(message, openingMediaSession);
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) || is(openingMediaSession)) {
            fsm.transition(message, inactive);
        }
    }

    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) {
        // Release existing media group if any
        if (this.mediaGroup != null) {
            this.mediaGroup.release();
        }

        try {
            // Create new media group
            this.mediaGroup = this.mediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);

            // Prepare the Media Group resources
            this.mediaGroup.getPlayer().addListener(this.playerListener);
            // this.mediaGroup.getSignalDetector().addListener(this.dtmfListener);
            // this.mediaGroup.getRecorder().addListener(this.recorderListener);

            // join the media group to the mixer
            this.mediaGroup.join(Direction.DUPLEX, this.mediaMixer);
            sender.tell(new MediaServerControllerResponse<ActorRef>(self), self);
        } catch (MsControlException e) {
            // TODO Auto-generated catch block
            logger.error(e.getMessage(), e);
        }

    }

    private void onStartMediaGroup(StartMediaGroup message, ActorRef self, ActorRef sender) throws MsControlException {
        if (is(active)) {
            // Join network connection to audio media group
            if (this.mediaGroup != null) {
                this.networkConnection.join(Direction.DUPLEX, this.mediaGroup);
            }

            // Tell observers the media group has been created
            final MediaGroupStateChanged response = new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE);
            notifyObservers(response, self);
        }
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active) && this.mediaGroup != null) {
            this.mediaGroup.release();
        }
    }

    private void onQueryMediaMixer(QueryMediaMixer message, ActorRef self, ActorRef sender) {
        MediaServerControllerResponse<MediaMixer> response = new MediaServerControllerResponse<MediaMixer>(this.mediaMixer);
        sender.tell(response, self);
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

                // Create the conference room also
                mediaMixer = mediaSession.createMediaMixer(MediaMixer.AUDIO);
                mediaMixer.addListener(mixerAllocationListener);
                mediaMixer.confirm();
                // Wait for event confirmation before sending response to the conference
            } catch (MsControlException e) {
                final MediaServerControllerError response = new MediaServerControllerError(e);
                sender().tell(new MediaServerControllerResponse<MediaServerControllerError>(response), super.source);
            }
        }

    }

    private final class Active extends AbstractAction {

        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            conference.tell(new MediaServerControllerResponse<MediaSessionInfo>(new MediaSessionInfo()), super.source);
        }
    }

    private final class Inactive extends AbstractAction {

        public Inactive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }

            // Inform conference that media session has been properly closed
            final MediaSessionClosed response = new MediaSessionClosed();
            conference.tell(new MediaServerControllerResponse<MediaSessionClosed>(response), super.source);
        }
    }

    private final class Failed extends AbstractAction {

        public Failed(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Inform call the media session could not be set up
            final MediaServerControllerError error = new MediaServerControllerError();
            conference.tell(error, super.source);
        }

    }

}

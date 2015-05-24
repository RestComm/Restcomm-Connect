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

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CreateConferenceEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController;
import org.mobicents.servlet.restcomm.mscontrol.messages.CloseMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.Record;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartRecording;
import org.mobicents.servlet.restcomm.mscontrol.messages.Stop;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopRecording;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.messages.EndpointInfo;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.messages.QueryEndpoint;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
@Immutable
public final class MmsConferenceController extends MediaServerController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State active;
    private final State inactive;
    private final State failed;
    private final State acquiringMediaSession;
    private final State acquiringEndpoint;
    private final State creatingMediaGroup;
    private final State destroyingMediaGroup;
    private Boolean fail;

    // MGCP runtime stuff.
    private final ActorRef mediaGateway;
    private MediaSession mediaSession;
    private ActorRef cnfEndpoint;

    // Conference runtime stuff
    private ActorRef conference;
    private ActorRef mediaGroup;

    // Runtime media operations
    private Boolean playing;
    private Boolean recording;
    private DateTime recordStarted;

    // Observers
    private final List<ActorRef> observers;

    public MmsConferenceController(ActorRef mediaGateway) {
        super();
        final ActorRef source = self();

        // Finite States
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", new Active(source), null);
        this.inactive = new State("inactive", new Inactive(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        this.acquiringEndpoint = new State("acquiring endpoint", new AcquiringEndpoint(source), null);
        this.creatingMediaGroup = new State("creating media group", new CreatingMediaGroup(source), null);
        this.destroyingMediaGroup = new State("destroying media group", new DestroyingMediaGroup(source), null);

        // Initialize the transitions for the FSM.
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

        // Finite State Machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        this.fail = Boolean.FALSE;

        // MGCP runtime stuff
        this.mediaGateway = mediaGateway;

        // Runtime media operations
        this.playing = Boolean.FALSE;
        this.recording = Boolean.FALSE;

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
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        final ActorRef self = self();
        final State state = fsm.state();

        logger.info(" ********** Conference Controller Current State: " + state.toString());
        logger.info(" ********** Conference Controller Processing Message: " + klass.getName());

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
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            onMediaGroupStateChanged((MediaGroupStateChanged) message, self, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            onStopMediaGroup((StopMediaGroup) message, self, sender);
        } else if (QueryEndpoint.class.equals(klass)) {
            onQueryEndpoint((QueryEndpoint) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecording((StartRecording) message, self, sender);
        } else if (StopRecording.class.equals(klass)) {
            onStopRecording((StopRecording) message, self, sender);
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
            this.conference = sender;
            fsm.transition(message, acquiringMediaSession);
        }
    }

    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        if (is(active)) {
            fsm.transition(message, inactive);
        } else {
            fsm.transition(message, destroyingMediaGroup);
        }
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
            this.cnfEndpoint = (ActorRef) message.get();
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
                        this.fsm.transition(message, inactive);
                    }
                }
                break;

            default:
                break;
        }
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
        if (is(active)) {
            // Stop the primary media group
            this.mediaGroup.tell(new Stop(), self);
            this.playing = Boolean.FALSE;
        }
    }

    private void onQueryEndpoint(QueryEndpoint message, ActorRef self, ActorRef sender) {
        final EndpointInfo endpointInfo = new EndpointInfo(cnfEndpoint, ConnectionMode.Confrnce);
        sender.tell(new MediaServerControllerResponse<EndpointInfo>(endpointInfo), self);
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (is(active) && !playing) {
            this.playing = Boolean.TRUE;
            this.mediaGroup.tell(message, sender);
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
            mediaGateway.tell(new CreateConferenceEndpoint(mediaSession), super.source);
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
                    return new MgcpMediaGroup(mediaGateway, mediaSession, cnfEndpoint);
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

    private final class DestroyingMediaGroup extends AbstractAction {

        public DestroyingMediaGroup(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            mediaGroup.tell(new StopMediaGroup(), super.source);
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

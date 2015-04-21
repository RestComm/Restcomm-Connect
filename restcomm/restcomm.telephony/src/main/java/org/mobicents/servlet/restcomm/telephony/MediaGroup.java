/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.telephony;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CloseLink;
import org.mobicents.servlet.restcomm.mgcp.CreateIvrEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyLink;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.IvrEndpointResponse;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.PlayCollect;
import org.mobicents.servlet.restcomm.mgcp.PlayRecord;
import org.mobicents.servlet.restcomm.mgcp.StopEndpoint;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MediaGroup extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite state machine stuff.
    private final State uninitialized;
    private final State active;
    private final State inactive;
    // Special intermediate states.
    private final State acquiringIvr;
    private final State acquiringLink;
    private final State initializingLink;
    private final State openingLink;
    private final State updatingLink;
    private final State deactivating;
    //Join Outboundcall Bridge endpoint to the IVR
    private final State acquiringInternalLink;
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;

    // FSM.
    private final FiniteStateMachine fsm;
    // MGCP runtime stuff.
    private final ActorRef gateway;
    private final ActorRef endpoint;
    private final MediaSession session;
    private ActorRef link;
    private ActorRef ivr;
    private boolean ivrInUse;
    // Runtime stuff.
    private final List<ActorRef> observers;

    private ActorRef internalLinkEndpoint;
    private ActorRef internalLink;
    private ConnectionMode internalLinkMode;

    public MediaGroup(final ActorRef gateway, final MediaSession session, final ActorRef endpoint) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        active = new State("active", new Active(source), null);
        inactive = new State("inactive", new Inactive(source), null);
        acquiringIvr = new State("acquiring ivr", new AcquiringIvr(source), null);
        acquiringLink = new State("acquiring link", new AcquiringLink(source), null);
        initializingLink = new State("initializing link", new InitializingLink(source), null);
        openingLink = new State("opening link", new OpeningLink(source), null);
        updatingLink = new State("updating link", new UpdatingLink(source), null);
        deactivating = new State("deactivating", new Deactivating(source), null);
        acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        initializingInternalLink = new State("initializing internal link", new InitializingInternalLink(source), null);
        openingInternalLink = new State("opening internal link", new OpeningInternalLink(source), null);
        updatingInternalLink = new State("updating internal link", new UpdatingInternalLink(source), null);
        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringIvr));
        transitions.add(new Transition(acquiringIvr, inactive));
        transitions.add(new Transition(acquiringIvr, acquiringLink));
        transitions.add(new Transition(acquiringLink, inactive));
        transitions.add(new Transition(acquiringLink, initializingLink));
        transitions.add(new Transition(initializingLink, inactive));
        transitions.add(new Transition(initializingLink, openingLink));
        transitions.add(new Transition(openingLink, inactive));
        transitions.add(new Transition(openingLink, deactivating));
        transitions.add(new Transition(openingLink, updatingLink));
        transitions.add(new Transition(updatingLink, active));
        transitions.add(new Transition(updatingLink, inactive));
        transitions.add(new Transition(updatingLink, deactivating));
        transitions.add(new Transition(active, deactivating));
        transitions.add(new Transition(deactivating, inactive));
        transitions.add(new Transition(active, acquiringIvr));
        //Join Outbound call Bridge endpoint to IVR endpoint
        transitions.add(new Transition(active, acquiringInternalLink));
        transitions.add(new Transition(acquiringInternalLink, initializingInternalLink));
        transitions.add(new Transition(initializingInternalLink, openingInternalLink));
        transitions.add(new Transition(openingInternalLink, updatingInternalLink));
        transitions.add(new Transition(updatingInternalLink, active));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the MGCP state.
        this.gateway = gateway;
        this.session = session;
        this.endpoint = endpoint;
        this.ivrInUse = false;
        // Initialize the rest of the media group state.
        this.observers = new ArrayList<ActorRef>();
    }

    private void collect(final Object message) {
        final ActorRef self = self();
        final Collect request = (Collect) message;
        final PlayCollect.Builder builder = PlayCollect.builder();
        for (final URI prompt : request.prompts()) {
            builder.addPrompt(prompt);
        }
        builder.setClearDigitBuffer(true);
        builder.setDigitPattern(request.pattern());
        builder.setFirstDigitTimer(request.timeout());
        builder.setInterDigitTimer(request.timeout());
        builder.setEndInputKey(request.endInputKey());
        builder.setMaxNumberOfDigits(request.numberOfDigits());
        stop();
        ivr.tell(builder.build(), self);
        ivrInUse = true;
    }

    private void play(final Object message) {
        final ActorRef self = self();
        final Play request = (Play) message;
        final List<URI> uris = request.uris();
        final int iterations = request.iterations();
        final org.mobicents.servlet.restcomm.mgcp.Play play = new org.mobicents.servlet.restcomm.mgcp.Play(uris, iterations);
        stop();
        ivr.tell(play, self);
        ivrInUse = true;
    }

    @SuppressWarnings("unchecked")
    private void notification(final Object message) {
        final IvrEndpointResponse<String> response = (IvrEndpointResponse<String>) message;
        final ActorRef self = self();
        MediaGroupResponse<String> event = null;
        if (response.succeeded()) {
            event = new MediaGroupResponse<String>(response.get());
        } else {
            event = new MediaGroupResponse<String>(response.cause(), response.error());
        }
        for (final ActorRef observer : observers) {
            observer.tell(event, self);
        }
        ivrInUse = false;
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    // FSM logic.
    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();
        final ActorRef sender = sender();
        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (MediaGroupStatus.class.equals(klass)) {
            if (active.equals(state)) {
                sender().tell(new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE), self());
            } else {
                sender().tell(new MediaGroupStateChanged(MediaGroupStateChanged.State.INACTIVE), self());
            }
        } else if (StartMediaGroup.class.equals(klass)) {
            logger.info("MediaGroup: "+self().path()+" got StartMediaGroup from: "+sender().path()+" endpoint: "+endpoint.path()+" isTerminated: "+endpoint.isTerminated());
            fsm.transition(message, acquiringIvr);
        } else if (Join.class.equals(klass)) {
            fsm.transition(message, acquiringInternalLink);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            if (acquiringIvr.equals(state)) {
                fsm.transition(message, acquiringLink);
            } else if (acquiringLink.equals(state)) {
                fsm.transition(message, initializingLink);
            } else if (acquiringInternalLink.equals(state)) {
                fsm.transition(message, initializingInternalLink);
            }
        } else if (LinkStateChanged.class.equals(klass)) {
            final LinkStateChanged response = (LinkStateChanged) message;
            if (LinkStateChanged.State.CLOSED == response.state()) {
                if (initializingLink.equals(state)) {
                    fsm.transition(message, openingLink);
                } else if (openingLink.equals(state) || deactivating.equals(state) || updatingLink.equals(state)) {
                    fsm.transition(message, inactive);
                } if (initializingInternalLink.equals(state)) {
                    fsm.transition(message, openingInternalLink);
                }
            } else if (LinkStateChanged.State.OPEN == response.state()) {
                if (openingLink.equals(state)) {
                    fsm.transition(message, updatingLink);
                } else if (updatingLink.equals(state)) {
                    fsm.transition(message, active);
                } if (openingInternalLink.equals(state)) {
                    fsm.transition(message, updatingInternalLink);
                } if (updatingInternalLink.equals(state)) {
                    fsm.transition(message, active);
                }
            }
        } else if (StopMediaGroup.class.equals(klass)) {
            if (acquiringLink.equals(state) || initializingLink.equals(state)) {
                fsm.transition(message, inactive);
            } else {
                fsm.transition(message, deactivating);
            }
        } else if (active.equals(state)) {
            if (Play.class.equals(klass)) {
                play(message);
            } else if (Collect.class.equals(klass)) {
                collect(message);
            } else if (Record.class.equals(klass)) {
                record(message);
            } else if (Stop.class.equals(klass)) {
                stop();
            } else if (IvrEndpointResponse.class.equals(klass)) {
                notification(message);
            }
        } else if (ivrInUse) {
            if (Stop.class.equals(klass)) {
                stop();
            }
        }
    }

    private void record(final Object message) {
        final ActorRef self = self();
        final Record request = (Record) message;
        final PlayRecord.Builder builder = PlayRecord.builder();
        for (final URI prompt : request.prompts()) {
            builder.addPrompt(prompt);
        }
        builder.setClearDigitBuffer(true);
        builder.setPreSpeechTimer(request.timeout());
        builder.setPostSpeechTimer(request.timeout());
        builder.setRecordingLength(request.length());
        builder.setEndInputKey(request.endInputKey());
        builder.setRecordingId(request.destination());
        stop();
        ivr.tell(builder.build(), self);
        ivrInUse = true;
    }

    private void stop() {
        if (ivrInUse) {
            final ActorRef self = self();
            ivr.tell(new StopEndpoint(), self);
            ivrInUse = false;
        }
    }

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringIvr extends AbstractAction {
        public AcquiringIvr(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (ivr != null && !ivr.isTerminated()) {
                logger.info("MediaGroup :"+self().path()+" got request to create ivr endpoint, will stop the existing one first: "+ivr.path());
                gateway.tell(new DestroyEndpoint(ivr), null);
                getContext().stop(ivr);
                ivr = null;
            }
            logger.info("MediaGroup :"+self().path()+" state: "+fsm.state().toString()+" session: "+session.id()+" will ask to get IvrEndpoint");
            gateway.tell(new CreateIvrEndpoint(session), source);
        }
    }

    private final class AcquiringLink extends AbstractAction {
        public AcquiringLink(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            ivr = response.get();
            ivr.tell(new Observe(source), source);
            if (link != null && !link.isTerminated()) {
                logger.info("MediaGroup :"+self().path()+" got request to create link endpoint, will stop the existing one first: "+link.path());
                gateway.tell(new DestroyLink(link), null);
                getContext().stop(link);
            }
            logger.info("MediaGroup :"+self().path()+" state: "+fsm.state().toString()+" session: "+session.id()+" ivr endpoint: "+ivr.path()+" will ask to get Link");
            gateway.tell(new CreateLink(session), source);
        }
    }

    private final class InitializingLink extends AbstractAction {
        public InitializingLink(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            link = response.get();
            if(endpoint == null)
                logger.info("MediaGroup :"+self().path()+" state: "+fsm.state().toString()+" session: "+session.id()+" link: "+link.path()+" endpoint is null will have exception");
            link.tell(new Observe(source), source);
            link.tell(new InitializeLink(endpoint, ivr), source);
            logger.info("MediaGroup :"+self().path()+" state: "+fsm.state().toString()+" session: "+session.id()+" link: "+link.path()+" endpoint: "+endpoint.path()+" initializeLink sent, endpoint isTerminated: "+endpoint.isTerminated());
        }
    }

    private final class OpeningLink extends AbstractAction {
        public OpeningLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            logger.info("MediaGroup :"+self().path()+" state: "+fsm.state().toString()+" session: "+session.id()+" link: "+link.path()+" will ask to open Link");
            link.tell(new OpenLink(ConnectionMode.SendRecv), source);
        }
    }

    private final class UpdatingLink extends AbstractAction {
        public UpdatingLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateLink update = new UpdateLink(ConnectionMode.SendRecv, UpdateLink.Type.PRIMARY);
            link.tell(update, source);
        }
    }

    //Join OutboundCall Bridge endpoint to the IVR endpoint for recording - START
    private final class AcquiringInternalLink extends AbstractAction {
        public AcquiringInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Join.class.equals(klass)) {
                final Join request = (Join) message;
                internalLinkEndpoint = request.endpoint();
                internalLinkMode = request.mode();
            }
            gateway.tell(new CreateLink(session), source);
        }
    }

    private final class InitializingInternalLink extends AbstractAction {
        public InitializingInternalLink(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            internalLink = response.get();
            internalLink.tell(new Observe(source), source);
            internalLink.tell(new InitializeLink(internalLinkEndpoint, ivr), source);
        }
    }

    private final class OpeningInternalLink extends AbstractAction {
        public OpeningInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            internalLink.tell(new OpenLink(internalLinkMode), source);
        }
    }

    private final class UpdatingInternalLink extends AbstractAction {
        public UpdatingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateLink update = new UpdateLink(ConnectionMode.SendRecv, UpdateLink.Type.PRIMARY);
            internalLink.tell(update, source);
        }
    }
    //Join OutboundCall Bridge endpoint to the IVR endpoint for recording - END

    private final class Active extends AbstractAction {
        public Active(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Notify the observers.
            final MediaGroupStateChanged event = new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

    private final class Inactive extends AbstractAction {
        public Inactive(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (link != null) {
                gateway.tell(new DestroyLink(link), source);
                link = null;
            }
            if (internalLink != null) {
                gateway.tell(new DestroyLink(internalLink), source);
                internalLink = null;
            }
            // Notify the observers.
            final MediaGroupStateChanged event = new MediaGroupStateChanged(MediaGroupStateChanged.State.INACTIVE);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

    private final class Deactivating extends AbstractAction {
        public Deactivating(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (link != null)
                link.tell(new CloseLink(), source);
            if(internalLink != null)
                internalLink.tell(new CloseLink(), source);
        }
    }

    @Override
    public void postStop() {
        if (internalLinkEndpoint != null) {
            logger.info("MediaGroup: "+self().path()+" at postStop, about to stop intenalLinkEndpoint: "+internalLinkEndpoint.path()+" sender: "+sender().path());
            gateway.tell(new DestroyEndpoint(internalLinkEndpoint), null);
            getContext().stop(internalLinkEndpoint);
            internalLinkEndpoint = null;
        }
        if (ivr != null) {
            logger.info("MediaGroup :"+self().path()+" at postStop, about to stop ivr endpoint :"+ivr.path());
            gateway.tell(new DestroyEndpoint(ivr), null);
            getContext().stop(ivr);
            ivr = null;
        }
    }
}

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
package org.restcomm.connect.telephony;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
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
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.mscontrol.api.messages.CreateMediaSession;
import org.restcomm.connect.mscontrol.api.messages.JoinCall;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.Leave;
import org.restcomm.connect.mscontrol.api.messages.Left;
import org.restcomm.connect.mscontrol.api.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.restcomm.connect.mscontrol.api.messages.Play;
import org.restcomm.connect.mscontrol.api.messages.StartRecording;
import org.restcomm.connect.mscontrol.api.messages.Stop;
import org.restcomm.connect.mscontrol.api.messages.StopMediaGroup;
import org.restcomm.connect.mscontrol.api.messages.StopRecording;
import org.restcomm.connect.telephony.api.AddParticipant;
import org.restcomm.connect.telephony.api.ConferenceInfo;
import org.restcomm.connect.telephony.api.ConferenceModeratorPresent;
import org.restcomm.connect.telephony.api.ConferenceResponse;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;
import org.restcomm.connect.telephony.api.GetConferenceInfo;
import org.restcomm.connect.telephony.api.RemoveParticipant;
import org.restcomm.connect.telephony.api.StartConference;
import org.restcomm.connect.telephony.api.StopConference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author henrique.rosa@telestax.com (Henrique Rosa)
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class Conference extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite state machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;
    private final State waiting;
    private final State running;
    private final State evicting;
    private final State stopping;
    private final State stopped;
    private final State failed;

    // Runtime stuff
    private final String name;
    private final String accountSid;
    private final String friendlyName;
    private Sid sid;
    private final List<ActorRef> calls;
    private final List<ActorRef> observers;

    private boolean moderatorPresent = false;

    // Media Session Controller
    private final ActorRef mscontroller;

    private final DaoManager storage;
    private int globalNoOfParticipants;

    private ConferenceStateChanged.State waitingState;

    public Conference(final String name, final ActorRef msController, final DaoManager storage) {
        super();
        final ActorRef source = self();

        // Finite states
        this.uninitialized = new State("uninitialized", null, null);
        this.initializing = new State("initializing", new Initializing(source));
        this.waiting = new State("waiting", new Waiting(source));
        this.running = new State("running", new Running(source));
        this.evicting = new State("evicting", new Evicting(source));
        this.stopping = new State("stopping", new Stopping(source));
        this.stopped = new State("stopped", new Stopped(source));
        this.failed = new State("failed", new Failed(source));

        // State transitions
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, initializing));
        transitions.add(new Transition(initializing, waiting));
        transitions.add(new Transition(initializing, stopping));
        transitions.add(new Transition(initializing, failed));
        transitions.add(new Transition(waiting, running));
        transitions.add(new Transition(waiting, evicting));
        transitions.add(new Transition(waiting, stopping));
        transitions.add(new Transition(running, evicting));
        transitions.add(new Transition(running, stopping));
        transitions.add(new Transition(evicting, stopping));
        transitions.add(new Transition(stopping, stopped));
        transitions.add(new Transition(stopping, failed));

        // Finite state machine
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // Runtime stuff
        this.name = name;
        final String[] cnfNameAndAccount = name.split(":");
        accountSid = cnfNameAndAccount[0];
        friendlyName = cnfNameAndAccount[1];

        this.storage = storage;

        //generate it later at MRB level, by watching if same conference is running on another RC instance.
        //this.sid = Sid.generate(Sid.Type.CONFERENCE);
        this.mscontroller = msController;
        this.calls = new ArrayList<ActorRef>();
        this.observers = new ArrayList<ActorRef>();
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private boolean isRunning() {
        return is(waiting) || is(running);
    }

    private void broadcast(final Object message) {
        if (!this.observers.isEmpty()) {
            final ActorRef self = self();
            for (ActorRef observer : observers) {
                if (!observer.isTerminated()) {
                    observer.tell(message, self);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Conference broadcase, Observer is terminated: "+observer.path());
                    }
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
            logger.info(" ********** Conference " + self().path() + " Current State: " + state.toString());
            logger.info(" ********** Conference " + self().path() + " Processing Message: " + klass.getName());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (GetConferenceInfo.class.equals(klass)) {
            onGetConferenceInfo(self, sender);
        } else if (StartConference.class.equals(klass)) {
            onStartConference((StartConference) message, self, sender);
        } else if (StopConference.class.equals(klass)) {
            onStopConference((StopConference) message, self, sender);
        } else if (ConferenceModeratorPresent.class.equals(klass)) {
            onConferenceModeratorPresent((ConferenceModeratorPresent) message, self, sender);
        } else if (AddParticipant.class.equals(klass)) {
            onAddParticipant((AddParticipant) message, self, sender);
        } else if (RemoveParticipant.class.equals(klass)) {
            onRemoveParticipant((RemoveParticipant) message, self, sender);
        } else if (Left.class.equals(klass)) {
            onLeft((Left) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (MediaServerConferenceControllerStateChanged.class.equals(klass)) {
            onMediaServerControllerStateChanged((MediaServerConferenceControllerStateChanged) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecording((StartRecording) message, self, sender);
        } else if (StopRecording.class.equals(klass)) {
            onStopRecording((StopRecording) message, self, sender);
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

    private class Initializing extends AbstractAction {

        public Initializing(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            StartConference startConference = (StartConference) message;
            // Start observing state changes in the MSController
            final Observe observe = new Observe(super.source);
            mscontroller.tell(observe, super.source);

            ConferenceInfo information = createConferenceInfo();
            // Initialize the MS Controller
            final CreateMediaSession createMediaSession = new CreateMediaSession(startConference.callSid(), information.name());
            mscontroller.tell(createMediaSession, super.source);
        }

    }

    private final class Waiting extends AbstractAction {
        public Waiting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            //As MRB have generated Sid for this conference and saved in db.
            MediaServerConferenceControllerStateChanged mediaServerConferenceControllerStateChanged = (MediaServerConferenceControllerStateChanged) message;
            sid = mediaServerConferenceControllerStateChanged.conferenceSid();
            String stateStr= mediaServerConferenceControllerStateChanged.conferenceState();

            //this is to cover the scenario where initial state is not moderatorAbsent and maybe moderator is present on another node.
            waitingState = ConferenceStateChanged.translateState(stateStr, ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT);
            moderatorPresent = mediaServerConferenceControllerStateChanged.moderatorPresent();
            if(logger.isInfoEnabled()) {
                logger.info("################################## Conference " + name + " has sid: "+sid +" stateStr: "+stateStr+" initial state: "+waitingState);
            }
            broadcast(new ConferenceStateChanged(name, waitingState));
        }
    }

    private final class Running extends AbstractAction {
        public Running(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            ConferenceModeratorPresent msg = (ConferenceModeratorPresent)message;
            // Stop the background music if present
            mscontroller.tell(new StopMediaGroup(msg.beep()), super.source);
            updateConferenceStatus(ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT);
            // Notify the observers
            broadcast(new ConferenceStateChanged(name, ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT));
        }
    }

    private class Evicting extends AbstractAction {

        public Evicting(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Tell every participant to leave the conference room
            // NOTE: calls list will only be update in the onLeft() event!
            for (final ActorRef call : calls) {
                final Leave leave = new Leave();
                call.tell(leave, super.source);
            }
        }

    }

    private class Stopping extends AbstractAction {

        public Stopping(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Ask the MS Controller to stop
            // This will stop any current media operations and clean media resources
            mscontroller.tell(new Stop(), super.source);
        }
    }

    private abstract class FinalizingAction extends AbstractAction {

        protected final ConferenceStateChanged.State finalState;

        public FinalizingAction(ActorRef source, ConferenceStateChanged.State state) {
            super(source);
            finalState = state;
        }

        @Override
        public void execute(Object message) throws Exception {
            // Notify the observers.
            broadcast(new ConferenceStateChanged(name, this.finalState));
            observers.clear();
        }

    }

    private final class Stopped extends FinalizingAction {

        public Stopped(final ActorRef source) {
            super(source, ConferenceStateChanged.State.COMPLETED);
        }

    }

    private final class Failed extends FinalizingAction {

        public Failed(final ActorRef source) {
            super(source, ConferenceStateChanged.State.FAILED);
        }

    }

    /*
     * EVENTS
     */
    private void onObserve(Observe message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        final ActorRef observer = message.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private void onGetConferenceInfo(ActorRef self, ActorRef sender) throws Exception {
        sender.tell(new ConferenceResponse<ConferenceInfo>(createConferenceInfo()), self);
    }

    private ConferenceInfo createConferenceInfo() throws Exception{
        ConferenceInfo information = null;
        int globalNoOfParticipants = getGlobalNoOfParticipants();
        if (is(waiting)) {
            information = new ConferenceInfo(sid, calls, waitingState, name, moderatorPresent, globalNoOfParticipants);
        } else if (is(running)) {
            information = new ConferenceInfo(sid, calls, ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT, name, moderatorPresent, globalNoOfParticipants);
        } else if (is(stopped)) {
            information = new ConferenceInfo(sid, calls, ConferenceStateChanged.State.COMPLETED, name, moderatorPresent, globalNoOfParticipants);
        } else {
            information = new ConferenceInfo(sid, calls, null, name, moderatorPresent, globalNoOfParticipants);
        }
        return information;
    }

    private void onStartConference(StartConference message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            this.fsm.transition(message, initializing);
        }
    }

    private void onStopConference(StopConference message, ActorRef self, ActorRef sender) throws Exception {
        if (is(initializing)) {
            this.fsm.transition(message, stopped);
        } else if (is(waiting) || is(running)) {
            this.fsm.transition(message, evicting);
        }
    }

    private void onConferenceModeratorPresent(ConferenceModeratorPresent message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(waiting)) {
            this.fsm.transition(message, running);
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            final JoinCall joinCall = new JoinCall(message.call(), ConnectionMode.Confrnce, this.sid);
            this.mscontroller.tell(joinCall, self);
        }
    }

    private void onRemoveParticipant(RemoveParticipant message, ActorRef self, ActorRef sender) throws Exception {
        if (isRunning()) {
            if (logger.isInfoEnabled()) {
                logger.info("Received RemoveParticipants for Call: "+message.call().path());
            }
            // Kindly ask participant to leave
            final ActorRef call = message.call();
            final Leave leave = new Leave();
            call.tell(leave, self);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Received RemoveParticipants for Call: "+message.call().path()+" but the state is: "+fsm.state().toString());
            }
        }
    }

    private void onLeft(Left message, ActorRef self, ActorRef sender) throws Exception {
        if (is(running) || is(waiting) || is(evicting)) {
            // Participant successfully left the conference.
            boolean removed = calls.remove(sender);
            int participantsNr = calls.size();
            if(logger.isInfoEnabled()) {
                logger.info("################################## Conference " + name + " has " + participantsNr + " participants");
            }
            ConferenceResponse conferenceResponse = new ConferenceResponse(message);
            broadcast(conferenceResponse);
            if (logger.isDebugEnabled()) {
                logger.debug("Call left conference room and notification sent to observers.");
            }

            // Stop the conference when ALL participants have been evicted
            if (removed && calls.isEmpty()) {
                fsm.transition(message, stopping);
            }
        }
    }

    private void onMediaServerControllerStateChanged(MediaServerConferenceControllerStateChanged message, ActorRef self, ActorRef sender)
            throws Exception {
        MediaServerControllerState state = message.getState();
        switch (state) {
            case ACTIVE:
                if (is(initializing)) {
                    this.fsm.transition(message, waiting);
                }
                break;
            case INACTIVE:
                if (is(stopping)) {
                    this.fsm.transition(message, stopped);
                }
                break;
            case FAILED:
                if (is(initializing)) {
                    this.fsm.transition(message, failed);
                }
                break;
            default:
                // ignore unknown state
                break;
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) throws Exception {
        this.mscontroller.tell(message, sender);
        this.calls.add(sender);
        if (logger.isInfoEnabled()) {
            logger.info("Conference name: "+name+", path: "+self().path()+", received JoinComplete from Call: "+sender.path()+", number of participants currently: "+calls.size()+", will send conference info to observers");
        }
        if (observers != null && observers.size() > 0) {
            Iterator<ActorRef> iter = observers.iterator();
            ConferenceInfo ci = createConferenceInfo();
            sender.tell(new ConferenceResponse<ConferenceInfo>(ci), self());
            while (iter.hasNext()) {
                ActorRef observer = iter.next();
                //First send conferenceInfo
                observer.tell(new ConferenceResponse<ConferenceInfo>(ci), self());
                //Next send the JoinComplete message
                observer.tell(message, self());
            }
        }
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            moderatorPresent = message.isConfModeratorPresent();
            if (logger.isInfoEnabled()) {
                logger.info("Received Play message for conference: "+this.name+" , number of local participants: "+this.calls.size()+ " globalNoOfParticipants: "+globalNoOfParticipants+", isRunning: true, isModeratorPresent: "+this.moderatorPresent + " iterations: "+message.iterations());
            }
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Play will not be processed for conference: "+this.name+" , number of local participants: "+this.calls.size()+ " globalNoOfParticipants: "+globalNoOfParticipants+" , isRunning: false, isModeratorPresent: "+this.moderatorPresent+ " iterations: "+message.iterations());
            }
        }
    }

    private void onStartRecording(StartRecording message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        }
    }

    private void onStopRecording(StopRecording message, ActorRef self, ActorRef sender) {
        if (isRunning()) {
            // Forward message to media server controller
            this.mscontroller.tell(message, sender);
        }
    }

    /**
     * get global total no of participants from db
     * @throws Exception
     */
    private int getGlobalNoOfParticipants() throws Exception{
        if(sid == null){
            globalNoOfParticipants = calls.size();
        }else{
            CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
            globalNoOfParticipants = dao.getTotalRunningCallDetailRecordsByConferenceSid(sid);
        }
        if(logger.isInfoEnabled())
            logger.info("sid: "+sid+"globalNoOfParticipants: "+globalNoOfParticipants);
        return globalNoOfParticipants;
    }

    private void updateConferenceStatus(ConferenceStateChanged.State state){
        if(sid != null){
            final ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
            ConferenceDetailRecord cdr = dao.getConferenceDetailRecord(sid);
            cdr = cdr.setStatus(state.name());
            dao.updateConferenceDetailRecordStatus(cdr);
        }
    }
}

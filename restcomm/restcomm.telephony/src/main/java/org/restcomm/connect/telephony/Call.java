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
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.javax.servlet.sip.SipSessionExt;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.fsm.Action;
import org.restcomm.connect.commons.fsm.FiniteStateMachine;
import org.restcomm.connect.commons.fsm.State;
import org.restcomm.connect.commons.fsm.Transition;
import org.restcomm.connect.commons.fsm.TransitionFailedException;
import org.restcomm.connect.commons.fsm.TransitionNotFoundException;
import org.restcomm.connect.commons.fsm.TransitionRollbackException;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.commons.util.SdpUtils;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.mscontrol.api.messages.CloseMediaSession;
import org.restcomm.connect.mscontrol.api.messages.Collect;
import org.restcomm.connect.mscontrol.api.messages.CreateMediaSession;
import org.restcomm.connect.mscontrol.api.messages.JoinBridge;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.JoinConference;
import org.restcomm.connect.mscontrol.api.messages.Leave;
import org.restcomm.connect.mscontrol.api.messages.Left;
import org.restcomm.connect.mscontrol.api.messages.MediaGroupResponse;
import org.restcomm.connect.mscontrol.api.messages.MediaServerControllerStateChanged;
import org.restcomm.connect.mscontrol.api.messages.MediaSessionInfo;
import org.restcomm.connect.mscontrol.api.messages.Mute;
import org.restcomm.connect.mscontrol.api.messages.Play;
import org.restcomm.connect.mscontrol.api.messages.Record;
import org.restcomm.connect.mscontrol.api.messages.StartRecording;
import org.restcomm.connect.mscontrol.api.messages.Stop;
import org.restcomm.connect.mscontrol.api.messages.StopMediaGroup;
import org.restcomm.connect.mscontrol.api.messages.StopRecording;
import org.restcomm.connect.mscontrol.api.messages.Unmute;
import org.restcomm.connect.mscontrol.api.messages.UpdateMediaSession;
import org.restcomm.connect.telephony.api.Answer;
import org.restcomm.connect.telephony.api.BridgeStateChanged;
import org.restcomm.connect.telephony.api.CallFail;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CallStateChanged;
import org.restcomm.connect.telephony.api.Cancel;
import org.restcomm.connect.telephony.api.ChangeCallDirection;
import org.restcomm.connect.telephony.api.ConferenceInfo;
import org.restcomm.connect.telephony.api.ConferenceResponse;
import org.restcomm.connect.telephony.api.CreateCall;
import org.restcomm.connect.telephony.api.Dial;
import org.restcomm.connect.telephony.api.GetCallInfo;
import org.restcomm.connect.telephony.api.GetCallObservers;
import org.restcomm.connect.telephony.api.Hangup;
import org.restcomm.connect.telephony.api.InitializeOutbound;
import org.restcomm.connect.telephony.api.Reject;
import org.restcomm.connect.telephony.api.RemoveParticipant;
import scala.concurrent.duration.Duration;

import javax.sdp.SdpException;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.message.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com (Jean Deruelle)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author gvagenas@telestax.com (George Vagenas)
 * @author henrique.rosa@telestax.com (Henrique Rosa)
 *
 */
@Immutable
public final class Call extends UntypedActor {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Define possible directions.
    private static final String INBOUND = "inbound";
    private static final String OUTBOUND_API = "outbound-api";
    private static final String OUTBOUND_DIAL = "outbound-dial";

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State initializing;
    private final State waitingForAnswer;
    private final State queued;
    private final State failingBusy;
    private final State ringing;
    private final State busy;
    private final State notFound;
    private final State canceling;
    private final State canceled;
    private final State failingNoAnswer;
    private final State noAnswer;
    private final State dialing;
    private final State updatingMediaSession;
    private final State inProgress;
    private final State joining;
    private final State leaving;
    private final State stopping;
    private final State completed;
    private final State failed;
    private final State inDialogRequest;
    private boolean fail;

    // SIP runtime stuff
    private final SipFactory factory;
    private String apiVersion;
    private Sid accountId;
    private String name;
    private SipURI from;
    private SipURI to;
    // custom headers for SIP Out https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    private Map<String, String> headers;
    private String username;
    private String password;
    private CreateCall.Type type;
    private long timeout;
    private SipServletRequest invite;
    private SipServletRequest inDialogInvite;
    private SipServletResponse lastResponse;
    private boolean isFromApi;

    // Call runtime stuff.
    private final Sid id;
    private final String instanceId;
    private CallStateChanged.State external;
    private String direction;
    private String forwardedFrom;
    private DateTime created;
    private DateTime callUpdatedTime;
    private final List<ActorRef> observers;
    private boolean receivedBye;
    private boolean sentBye;
    private boolean muted;
    private boolean webrtc;
    private boolean initialInviteOkSent;

    // Conferencing
    private ActorRef conference;
    private boolean conferencing;
    private Sid conferenceSid;

    // Call Bridging
    private ActorRef bridge;

    // Media Session Control runtime stuff
    private final ActorRef msController;
    private MediaSessionInfo mediaSessionInfo;

    // Media Group runtime stuff
    private CallDetailRecord outgoingCallRecord;
    private CallDetailRecordsDao recordsDao;
    private DaoManager daoManager;
    private boolean liveCallModification;
    private boolean recording;
    private Sid parentCallSid;

    // Runtime Setting
    private Configuration runtimeSettings;
    private Configuration configuration;
    private boolean disableSdpPatchingOnUpdatingMediaSession;

    private Sid inboundCallSid;
    private boolean inboundConfirmCall;
    private int collectTimeout;
    private String collectFinishKey;
    private boolean collectSipInfoDtmf = false;

    private boolean enable200OkDelay;

    public Call(final SipFactory factory, final ActorRef mediaSessionController, final Configuration configuration) {
        super();
        final ActorRef source = self();

        // States for the FSM
        this.uninitialized = new State("uninitialized", null, null);
        this.initializing = new State("initializing", new Initializing(source), null);
        this.waitingForAnswer = new State("waiting for answer", new WaitingForAnswer(source), null);
        this.queued = new State("queued", new Queued(source), null);
        this.ringing = new State("ringing", new Ringing(source), null);
        this.failingBusy = new State("failing busy", new FailingBusy(source), null);
        this.busy = new State("busy", new Busy(source), null);
        this.notFound = new State("not found", new NotFound(source), null);
        //This time the --new Canceling(source)-- is an ActionOnState. Overloaded constructor is used here
        this.canceling = new State("canceling", new Canceling(source));
        this.canceled = new State("canceled", new Canceled(source), null);
        this.failingNoAnswer = new State("failing no answer", new FailingNoAnswer(source), null);
        this.noAnswer = new State("no answer", new NoAnswer(source), null);
        this.dialing = new State("dialing", new Dialing(source), null);
        this.updatingMediaSession = new State("updating media session", new UpdatingMediaSession(source), null);
        this.inProgress = new State("in progress", new InProgress(source), null);
        this.joining = new State("joining", new Joining(source), null);
        this.leaving = new State("leaving", new Leaving(source), null);
        this.stopping = new State("stopping", new Stopping(source), null);
        this.completed = new State("completed", new Completed(source), null);
        this.failed = new State("failed", new Failed(source), null);
        this.inDialogRequest = new State("InDialogRequest", new InDialogRequest(source), null);

        // Transitions for the FSM
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.ringing));
        transitions.add(new Transition(this.uninitialized, this.queued));
        transitions.add(new Transition(this.uninitialized, this.canceled));
        transitions.add(new Transition(this.uninitialized, this.completed));
        transitions.add(new Transition(this.queued, this.canceled));
        transitions.add(new Transition(this.queued, this.initializing));
        transitions.add(new Transition(this.ringing, this.busy));
        transitions.add(new Transition(this.ringing, this.notFound));
        transitions.add(new Transition(this.ringing, this.canceling));
        transitions.add(new Transition(this.ringing, this.canceled));
        transitions.add(new Transition(this.ringing, this.failingNoAnswer));
        transitions.add(new Transition(this.ringing, this.failingBusy));
        transitions.add(new Transition(this.ringing, this.noAnswer));
        transitions.add(new Transition(this.ringing, this.initializing));
        transitions.add(new Transition(this.ringing, this.updatingMediaSession));
        transitions.add(new Transition(this.ringing, this.completed));
        transitions.add(new Transition(this.ringing, this.stopping));
        transitions.add(new Transition(this.ringing, this.failed));
        transitions.add(new Transition(this.initializing, this.canceling));
        transitions.add(new Transition(this.initializing, this.dialing));
        transitions.add(new Transition(this.initializing, this.failed));
        transitions.add(new Transition(this.initializing, this.inProgress));
        transitions.add(new Transition(this.initializing, this.waitingForAnswer));
        transitions.add(new Transition(this.initializing, this.stopping));
        transitions.add(new Transition(this.waitingForAnswer, this.inProgress));
        transitions.add(new Transition(this.waitingForAnswer, this.joining));
        transitions.add(new Transition(this.waitingForAnswer, this.canceling));
        transitions.add(new Transition(this.waitingForAnswer, this.completed));
        transitions.add(new Transition(this.waitingForAnswer, this.stopping));
        transitions.add(new Transition(this.dialing, this.canceling));
        transitions.add(new Transition(this.dialing, this.stopping));
        transitions.add(new Transition(this.dialing, this.failingBusy));
        transitions.add(new Transition(this.dialing, this.ringing));
        transitions.add(new Transition(this.dialing, this.failed));
        transitions.add(new Transition(this.dialing, this.failingNoAnswer));
        transitions.add(new Transition(this.dialing, this.noAnswer));
        transitions.add(new Transition(this.dialing, this.updatingMediaSession));
        transitions.add(new Transition(this.inProgress, this.stopping));
        transitions.add(new Transition(this.inProgress, this.joining));
        transitions.add(new Transition(this.inProgress, this.leaving));
        transitions.add(new Transition(this.inProgress, this.failed));
        transitions.add(new Transition(this.inProgress, this.inDialogRequest));
        transitions.add(new Transition(this.joining, this.inProgress));
        transitions.add(new Transition(this.joining, this.stopping));
        transitions.add(new Transition(this.joining, this.failed));
        transitions.add(new Transition(this.leaving, this.inProgress));
        transitions.add(new Transition(this.leaving, this.stopping));
        transitions.add(new Transition(this.leaving, this.failed));
        transitions.add(new Transition(this.leaving, this.completed));
        transitions.add(new Transition(this.canceling, this.canceled));
        transitions.add(new Transition(this.canceling, this.completed));
        transitions.add(new Transition(this.failingBusy, this.busy));
        transitions.add(new Transition(this.failingNoAnswer, this.noAnswer));
        transitions.add(new Transition(this.failingNoAnswer, this.canceling));
        transitions.add(new Transition(this.updatingMediaSession, this.inProgress));
        transitions.add(new Transition(this.updatingMediaSession, this.failed));
        transitions.add(new Transition(this.stopping, this.completed));
        transitions.add(new Transition(this.stopping, this.failed));
        transitions.add(new Transition(this.failed, this.completed));
        transitions.add(new Transition(this.completed, this.stopping));
        transitions.add(new Transition(this.completed, this.failed));

        // FSM
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);

        // SIP runtime stuff.
        this.factory = factory;

        // Conferencing
        this.conferencing = false;

        // Media Session Control runtime stuff.
        this.msController = mediaSessionController;
        this.fail = false;

        // Initialize the runtime stuff.
        this.id = Sid.generate(Sid.Type.CALL);
        this.instanceId = RestcommConfiguration.getInstance().getMain().getInstanceId();
        this.created = DateTime.now();
        this.observers = Collections.synchronizedList(new ArrayList<ActorRef>());
        this.receivedBye = false;

        // Media Group runtime stuff
        this.liveCallModification = false;
        this.recording = false;
        this.configuration = configuration;
        this.disableSdpPatchingOnUpdatingMediaSession = this.configuration.subset("runtime-settings").getBoolean("disable-sdp-patching-on-updating-mediasession", false);
        this.enable200OkDelay = this.configuration.subset("runtime-settings").getBoolean("enable-200-ok-delay",false);
    }

    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    private boolean isInbound() {
        return INBOUND.equals(this.direction);
    }

    private boolean isOutbound() {
        return !isInbound();
    }

    private CallResponse<CallInfo> info() {
        final String from = this.from.getUser();
        final String to = this.to.getUser();
        final CallInfo info = new CallInfo(id, external, type, direction, created, forwardedFrom, name, from, to, invite, lastResponse, webrtc, muted, isFromApi, callUpdatedTime);
        return new CallResponse<CallInfo>(info);
    }

    private void forwarding(final Object message) {
        // XXX does nothing
    }

    private SipURI getInitialIpAddressPort(SipServletMessage message) throws ServletParseException, UnknownHostException {
        // Issue #268 - https://bitbucket.org/telestax/telscale-restcomm/issue/268
        // First get the Initial Remote Address (real address that the request came from)
        // Then check the following:
        // 1. If contact header address is private network address
        // 2. If there are no "Record-Route" headers (there is no proxy in the call)
        // 3. If contact header address != real ip address
        // Finally, if all of the above are true, create a SIP URI using the realIP address and the SIP port
        // and store it to the sip session to be used as request uri later
        SipURI uri = null;
        try {
            String realIP = message.getInitialRemoteAddr();
            Integer realPort = message.getInitialRemotePort();
            if (realPort == null || realPort == -1) {
                realPort = 5060;
            }

            if (realPort == 0) {
                realPort = message.getRemotePort();
            }

            final ListIterator<String> recordRouteHeaders = message.getHeaders("Record-Route");
            final Address contactAddr = factory.createAddress(message.getHeader("Contact"));

            InetAddress contactInetAddress = InetAddress.getByName(((SipURI) contactAddr.getURI()).getHost());
            InetAddress inetAddress = InetAddress.getByName(realIP);

            int remotePort = message.getRemotePort();
            int contactPort = ((SipURI) contactAddr.getURI()).getPort();
            String remoteAddress = message.getRemoteAddr();

            // Issue #332: https://telestax.atlassian.net/browse/RESTCOMM-332
            final String initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
            String initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");
            String contactAddress = ((SipURI) contactAddr.getURI()).getHost();

            if (initialIpBeforeLB != null) {
                if (initialPortBeforeLB == null)
                    initialPortBeforeLB = "5060";
                if(logger.isInfoEnabled()) {
                    logger.info("We are behind load balancer, storing Initial Remote Address " + initialIpBeforeLB + ":"
                        + initialPortBeforeLB + " to the session for later use");
                }
                realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                uri = factory.createSipURI(null, realIP);
            } else if (contactInetAddress.isSiteLocalAddress() && !recordRouteHeaders.hasNext()
                    && !contactInetAddress.toString().equalsIgnoreCase(inetAddress.toString())) {
                if(logger.isInfoEnabled()) {
                    logger.info("Contact header address " + contactAddr.toString()
                        + " is a private network ip address, storing Initial Remote Address " + realIP + ":" + realPort
                        + " to the session for later use");
                }
                realIP = realIP + ":" + realPort;
                uri = factory.createSipURI(null, realIP);
            }
        } catch (Exception e) {
            logger.warning("Exception while trying to get the Initial IP Address and Port: "+e);

        }
        return uri;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();
        if(logger.isInfoEnabled()) {
            logger.info("********** Call's " + self().path() + " Current State: \"" + state.toString()+" direction: "+direction);
            logger.info("********** Call " + self().path() + " Processing Message: \"" + klass.getName() + " sender : "
                + sender.path().toString());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (GetCallObservers.class.equals(klass)) {
            onGetCallObservers((GetCallObservers) message, self, sender);
        } else if (GetCallInfo.class.equals(klass)) {
            onGetCallInfo((GetCallInfo) message, sender);
        } else if (InitializeOutbound.class.equals(klass)) {
            onInitializeOutbound((InitializeOutbound) message, self, sender);
        } else if (ChangeCallDirection.class.equals(klass)) {
            onChangeCallDirection((ChangeCallDirection) message, self, sender);
        } else if (Answer.class.equals(klass)) {
            onAnswer((Answer) message, self, sender);
        } else if (Dial.class.equals(klass)) {
            onDial((Dial) message, self, sender);
        } else if (Reject.class.equals(klass)) {
            onReject((Reject) message, self, sender);
        } else if (CallFail.class.equals(klass)) {
            fsm.transition(message, failed);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (StartRecording.class.equals(klass)) {
            onStartRecordingCall((StartRecording) message, self, sender);
        } else if (StopRecording.class.equals(klass)) {
            onStopRecordingCall((StopRecording) message, self, sender);
        } else if (Cancel.class.equals(klass)) {
            onCancel((Cancel) message, self, sender);
        } else if (message instanceof ReceiveTimeout) {
            onReceiveTimeout((ReceiveTimeout) message, self, sender);
        } else if (message instanceof SipServletRequest) {
            onSipServletRequest((SipServletRequest) message, self, sender);
        } else if (message instanceof SipServletResponse) {
            onSipServletResponse((SipServletResponse) message, self, sender);
        } else if (Hangup.class.equals(klass)) {
            onHangup((Hangup) message, self, sender);
        } else if (org.restcomm.connect.telephony.api.NotFound.class.equals(klass)) {
            onNotFound((org.restcomm.connect.telephony.api.NotFound) message, self, sender);
        } else if (MediaServerControllerStateChanged.class.equals(klass)) {
            onMediaServerControllerStateChanged((MediaServerControllerStateChanged) message, self, sender);
        } else if (JoinConference.class.equals(klass)) {
            onJoinConference((JoinConference) message, self, sender);
        } else if (JoinBridge.class.equals(klass)) {
            onJoinBridge((JoinBridge) message, self, sender);
        } else if (Leave.class.equals(klass)) {
            onLeave((Leave) message, self, sender);
        } else if (Left.class.equals(klass)) {
            onLeft((Left) message, self, sender);
        } else if (Record.class.equals(klass)) {
            onRecord((Record) message, self, sender);
        } else if (Play.class.equals(klass)) {
            onPlay((Play) message, self, sender);
        } else if (Collect.class.equals(klass)) {
            onCollect((Collect) message, self, sender);
        } else if (StopMediaGroup.class.equals(klass)) {
            onStopMediaGroup((StopMediaGroup) message, self, sender);
        } else if (Mute.class.equals(klass)) {
            onMute((Mute) message, self, sender);
        } else if (Unmute.class.equals(klass)) {
            onUnmute((Unmute) message, self, sender);
        } else if (ConferenceResponse.class.equals(klass)) {
            onConferenceResponse((ConferenceResponse) message);
        } else if (BridgeStateChanged.class.equals(klass)) {
            onBridgeStateChanged((BridgeStateChanged) message, self, sender);
        }
    }

    private void onConferenceResponse(ConferenceResponse conferenceResponse) {
        //ConferenceResponse received
        ConferenceInfo ci = (ConferenceInfo) conferenceResponse.get();
        if (logger.isInfoEnabled()) {
            String infoMsg = String.format("Conference response, name %s, state %s, participants %d", ci.name(), ci.state(), ci.globalParticipants());
            logger.info(infoMsg);
        }
    }

    private void addCustomHeaders(SipServletMessage message) {
        if (apiVersion != null)
            message.addHeader("X-RestComm-ApiVersion", apiVersion);
        if (accountId != null)
            message.addHeader("X-RestComm-AccountSid", accountId.toString());
        message.addHeader("X-RestComm-CallSid", instanceId+"-"+id.toString());
    }

    // Allow updating of the callInfo at the VoiceInterpreter so that we can do Dial SIP Screening
    // (https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out) accurately from latest response
    // received
    private void sendCallInfoToObservers() {
        for (final ActorRef observer : this.observers) {
            observer.tell(info(), self());
        }
    }

    private void processInfo(final SipServletRequest request) throws IOException {
        //Seems we will receive DTMF over SIP INFO, we should start timeout timer
        //to simulate the collect timeout when using the RMS
        collectSipInfoDtmf = true;
        context().setReceiveTimeout(Duration.create(collectTimeout, TimeUnit.SECONDS));
        final SipServletResponse okay = request.createResponse(SipServletResponse.SC_OK);
        addCustomHeaders(okay);
        okay.send();
        String digits = null;
        if (request.getContentType().equalsIgnoreCase("application/dtmf-relay")) {
            final String content = new String(request.getRawContent());
            digits = content.split("\n")[0].replaceFirst("Signal=", "").trim();
        } else {
            digits = new String(request.getRawContent());
        }
        if (digits != null) {
            MediaGroupResponse<String> infoResponse = new MediaGroupResponse<String>(digits);
            for (final ActorRef observer : observers) {
                observer.tell(infoResponse, self());
            }
            this.msController.tell(new Stop(), self());
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

    private final class Queued extends AbstractAction {

        public Queued(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final InitializeOutbound request = (InitializeOutbound) message;
            name = request.name();
            from = request.from();
            to = request.to();
            apiVersion = request.apiVersion();
            accountId = request.accountId();
            username = request.username();
            password = request.password();
            type = request.type();
            parentCallSid = request.getParentCallSid();
            recordsDao = request.getDaoManager().getCallDetailRecordsDao();
            isFromApi = request.isFromApi();
            String toHeaderString = to.toString();
            if (toHeaderString.indexOf('?') != -1) {
                // custom headers parsing for SIP Out
                // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                headers = new HashMap<String, String>();
                // we keep only the to URI without the headers
                to = (SipURI) factory.createURI(toHeaderString.substring(0, toHeaderString.lastIndexOf('?')));
                String headersString = toHeaderString.substring(toHeaderString.lastIndexOf('?') + 1);
                StringTokenizer tokenizer = new StringTokenizer(headersString, "&");
                while (tokenizer.hasMoreTokens()) {
                    String headerNameValue = tokenizer.nextToken();
                    String headerName = headerNameValue.substring(0, headerNameValue.lastIndexOf('='));
                    String headerValue = headerNameValue.substring(headerNameValue.lastIndexOf('=') + 1);
                    headers.put(headerName, headerValue);
                }
            }
            timeout = request.timeout();
            direction = request.isFromApi() ? OUTBOUND_API : OUTBOUND_DIAL;
            webrtc = request.isWebrtc();

            // Notify the observers.
            external = CallStateChanged.State.QUEUED;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            if (recordsDao != null) {
                CallDetailRecord cdr = recordsDao.getCallDetailRecord(id);
                if (cdr == null) {
                    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                    builder.setSid(id);
                    builder.setInstanceId(RestcommConfiguration.getInstance().getMain().getInstanceId());
                    builder.setDateCreated(created);
                    builder.setAccountSid(accountId);
                    builder.setTo(to.getUser());
                    builder.setCallerName(name);
                    builder.setStartTime(new DateTime());
                    String fromString = (from.getUser() != null ? from.getUser() : "CALLS REST API");
                    builder.setFrom(fromString);
                    // builder.setForwardedFrom(callInfo.forwardedFrom());
                    // builder.setPhoneNumberSid(phoneId);
                    builder.setStatus(external.name());
                    builder.setDirection("outbound-api");
                    builder.setApiVersion(apiVersion);
                    builder.setPrice(new BigDecimal("0.00"));
                    // TODO implement currency property to be read from Configuration
                    builder.setPriceUnit(Currency.getInstance("USD"));
                    final StringBuilder buffer = new StringBuilder();
                    buffer.append("/").append(apiVersion).append("/Accounts/");
                    buffer.append(accountId.toString()).append("/Calls/");
                    buffer.append(id.toString());
                    final URI uri = URI.create(buffer.toString());
                    builder.setUri(uri);
                    builder.setCallPath(self().path().toString());
                    builder.setParentCallSid(parentCallSid);
                    outgoingCallRecord = builder.build();
                    recordsDao.addCallDetailRecord(outgoingCallRecord);
                } else {
                    cdr.setStatus(external.name());
                }
            }
        }
    }

    private final class Dialing extends AbstractAction {

        public Dialing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final MediaServerControllerStateChanged response = (MediaServerControllerStateChanged) message;
            final ActorRef self = self();

            mediaSessionInfo = response.getMediaSession();

            // Create a SIP invite to initiate a new session.
            final StringBuilder buffer = new StringBuilder();
            buffer.append(to.getHost());
            if (to.getPort() > -1) {
                buffer.append(":").append(to.getPort());
            }
            String transport = to.getTransportParam();
            if (transport != null) {
                buffer.append(";transport=").append(to.getTransportParam());
            }
            final SipURI uri = factory.createSipURI(null, buffer.toString());
            final SipApplicationSession application = factory.createApplicationSession();
            application.setAttribute(Call.class.getName(), self);
            if (name != null && !name.isEmpty()) {
                // Create the from address using the inital user displayed name
                // Example: From: "Alice" <sip:userpart@host:port>
                final Address fromAddress = factory.createAddress(from, name);
                final Address toAddress = factory.createAddress(to);
                invite = factory.createRequest(application, "INVITE", fromAddress, toAddress);
            } else {
                invite = factory.createRequest(application, "INVITE", from, to);
            }
            invite.pushRoute(uri);

            if (headers != null) {
                // adding custom headers for SIP Out
                // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                Set<Map.Entry<String, String>> entrySet = headers.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {
                    invite.addHeader("X-" + entry.getKey(), entry.getValue());
                }
            }
            addCustomHeaders(invite);
//            invite.addHeader("X-RestComm-ApiVersion", apiVersion);
//            invite.addHeader("X-RestComm-AccountSid", accountId.toString());
//            invite.addHeader("X-RestComm-CallSid", id.toString());
            final SipSession session = invite.getSession();
            session.setHandler("CallManager");
            // Issue: https://telestax.atlassian.net/browse/RESTCOMM-608
            // If this is a call to Restcomm client or SIP URI bypass LB
            if (logger.isInfoEnabled())
                logger.info("bypassLoadBalancer is set to: "+RestcommConfiguration.getInstance().getMain().getBypassLbForClients());
            if (RestcommConfiguration.getInstance().getMain().getBypassLbForClients()) {
                if (type.equals(CreateCall.Type.CLIENT) || type.equals(CreateCall.Type.SIP)) {
                    ((SipSessionExt) session).setBypassLoadBalancer(true);
                    ((SipSessionExt) session).setBypassProxy(true);
                }
            }
            String offer = null;
            if (mediaSessionInfo.usesNat()) {
                final String externalIp = mediaSessionInfo.getExternalAddress().getHostAddress();
                final byte[] sdp = mediaSessionInfo.getLocalSdp().getBytes();
                offer = SdpUtils.patch("application/sdp", sdp, externalIp);
            } else {
                offer = mediaSessionInfo.getLocalSdp();
            }
            offer = SdpUtils.endWithNewLine(offer);
            invite.setContent(offer, "application/sdp");
            // Send the invite.
            invite.send();
            // Set the timeout period.
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.create(timeout, TimeUnit.SECONDS));
        }
    }

    private final class Ringing extends AbstractAction {

        public Ringing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof SipServletRequest) {
                invite = (SipServletRequest) message;
                from = (SipURI) invite.getFrom().getURI();
                to = (SipURI) invite.getTo().getURI();
                timeout = -1;
                direction = INBOUND;
                try {
                    // Send a ringing response
                    final SipServletResponse ringing = invite.createResponse(SipServletResponse.SC_RINGING);
                    addCustomHeaders(ringing);
//                    ringing.addHeader("X-RestComm-CallSid", id.toString());
                    ringing.send();
                } catch (IllegalStateException exception) {
                    if(logger.isDebugEnabled()) {
                        logger.debug("Exception while creating 180 response to inbound invite request");
                    }
                    fsm.transition(message, canceled);
                }

                SipURI initialInetUri = getInitialIpAddressPort(invite);

                if (initialInetUri != null) {
                    invite.getSession().setAttribute("realInetUri", initialInetUri);
                }
            } else if (message instanceof SipServletResponse) {
                // Timeout still valid in case we receive a 180, we don't know if the
                // call will be eventually answered.
                // Issue 585: https://telestax.atlassian.net/browse/RESTCOMM-585

                // final UntypedActorContext context = getContext();
                // context.setReceiveTimeout(Duration.Undefined());
                SipURI initialInetUri = getInitialIpAddressPort((SipServletResponse)message);

                if (initialInetUri != null) {
                    ((SipServletResponse)message).getSession().setAttribute("realInetUri", initialInetUri);
                }
            }

            // Notify the observers.
            external = CallStateChanged.State.RINGING;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class Canceling extends AbstractAction {

        public Canceling(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            try {
                if (isOutbound() && (invite.getSession().getState() != SipSession.State.INITIAL || invite.getSession().getState() != SipSession.State.TERMINATED)) {
                    final UntypedActorContext context = getContext();
                    context.setReceiveTimeout(Duration.Undefined());
                    final SipServletRequest cancel = invite.createCancel();
                    addCustomHeaders(cancel);
                    cancel.send();
                    if (logger.isInfoEnabled()) {
                        logger.info("Sent CANCEL for Call: "+self().path()+", state: "+fsm.state()+", direction: "+direction);
                    }
                }
            } catch (Exception e) {
                StringBuffer strBuffer = new StringBuffer();
                strBuffer.append("Exception while trying to create Cancel for Call with the following details, from: "+from+" to: "+to+" direction: "+direction+" call state: "+fsm.state());
                if (invite != null) {
                    strBuffer.append(" , invite RURI: "+invite.getRequestURI());
                } else {
                    strBuffer.append(" , invite is NULL! ");
                }
                strBuffer.append(" Exception: "+e.getMessage());
                logger.warning(strBuffer.toString());
            }
            msController.tell(new CloseMediaSession(), source);
        }
    }

    private final class Canceled extends AbstractAction {

        public Canceled(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            //A no-answer call will be cancelled and will arrive here. In that case don't change the external case
            //since no-answer is a final state and we need to keep it so observer knows how the call ended
//            if (!external.equals(CallStateChanged.State.NO_ANSWER)) {
                external = CallStateChanged.State.CANCELED;
//                final CallStateChanged event = new CallStateChanged(external);
//                for (final ActorRef observer : observers) {
//                    observer.tell(event, source);
//                }
//            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                if(logger.isInfoEnabled()) {
                    logger.info("Going to update CDR to CANCEL, call sid: "+id+" from: "+from+" to: "+to+" direction: "+direction);
                }
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
            fsm.transition(message, completed);
        }
    }

    private abstract class Failing extends AbstractAction {
        public Failing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof ReceiveTimeout) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }
            callUpdatedTime = DateTime.now();
            msController.tell(new CloseMediaSession(), source);
        }
    }

    private final class FailingBusy extends Failing {

        public FailingBusy(final ActorRef source) {
            super(source);
        }
    }

    private final class FailingNoAnswer extends Failing {

        public FailingNoAnswer(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            if(logger.isInfoEnabled()) {
                logger.info("Call moves to failing state because no answer");
            }
            fsm.transition(message, noAnswer);
        }
    }

    private final class Busy extends AbstractAction {

        public Busy(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();

            // Send SIP BUSY to remote peer
            if (Reject.class.equals(klass) && is(ringing) && isInbound()) {
                Reject reject = (Reject) message;
                SipServletResponse rejectResponse;
                if (reject.getReason().equalsIgnoreCase("busy")) {
                    rejectResponse = invite.createResponse(SipServletResponse.SC_BUSY_HERE);
                } else {
                    rejectResponse = invite.createResponse(SipServletResponse.SC_DECLINE);
                }
                addCustomHeaders(rejectResponse);
                rejectResponse.send();
            }

            // Explicitly invalidate the application session.
            // if (invite.getSession().isValid())
            // invite.getSession().invalidate();
            // if (invite.getApplicationSession().isValid())
            // invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.BUSY;
            final CallStateChanged event = new CallStateChanged(external, lastResponse.getStatus());
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
                outgoingCallRecord = outgoingCallRecord.setDuration(0);
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
                final int seconds = (int) ((DateTime.now().getMillis() - outgoingCallRecord.getStartTime().getMillis()) / 1000);
                outgoingCallRecord = outgoingCallRecord.setRingDuration(seconds);
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class NotFound extends AbstractAction {

        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();

            // Send SIP NOT_FOUND to remote peer
            if (org.restcomm.connect.telephony.api.NotFound.class.equals(klass) && isInbound()) {
                final SipServletResponse notFound = invite.createResponse(SipServletResponse.SC_NOT_FOUND);
                addCustomHeaders(notFound);
                notFound.send();
            }

            // Notify the observers.
            external = CallStateChanged.State.NOT_FOUND;
            final CallStateChanged event = new CallStateChanged(external, SipServletResponse.SC_NOT_FOUND);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class NoAnswer extends AbstractAction {

        public NoAnswer(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // // Explicitly invalidate the application session.
            // if (invite.getSession().isValid())
            // invite.getSession().invalidate();
            // if (invite.getApplicationSession().isValid())
            // invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.NO_ANSWER;
            final CallStateChanged event = new CallStateChanged(external, SipServletResponse.SC_REQUEST_TIMEOUT);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class Failed extends AbstractAction {

        public Failed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (isInbound()) {
                SipServletResponse resp = null;
                if (message instanceof CallFail) {
                    resp = invite.createResponse(500, "Problem to setup the call");
                    String reason = ((CallFail) message).getReason();
                    if (reason != null)
                        resp.addHeader("Reason", reason);
                } else {
                    // https://github.com/RestComm/Restcomm-Connect/issues/1663
                    // We use 503 only if there is a problem to reach RMS as LB can be configured to take out
                    // nodes that send back 503. This is meant to protect the cluster from nodes where the RMS
                    // is in bad state and not responding anymore
                    resp = invite.createResponse(503, "Problem to setup services");
                }
                addCustomHeaders(resp);
                resp.send();
            } else {
                if (message instanceof CallFail)
                    sendBye(new Hangup(((CallFail) message).getReason()));
            }

            // Notify the observers.
            external = CallStateChanged.State.FAILED;
            final CallStateChanged event = new CallStateChanged(external, lastResponse.getStatus());
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class Initializing extends AbstractAction {

        public Initializing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Start observing state changes in the MSController
            final Observe observe = new Observe(super.source);
            msController.tell(observe, super.source);

            // Initialize the MS Controller
            CreateMediaSession command = null;
            if (isOutbound()) {
                command = new CreateMediaSession("sendrecv", "", true, webrtc, id);
            } else {
                if (!liveCallModification) {
                    command = generateRequest(invite);
                } else {
                    if (lastResponse != null && lastResponse.getStatus() == 200) {
                        command = generateRequest(lastResponse);
                    }
                    // TODO no else may lead to NullPointerException
                }
            }
            msController.tell(command, source);
        }
    }

    private final class InDialogRequest extends AbstractAction {

        public InDialogRequest(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            SipServletRequest request = (SipServletRequest) message;
            if (logger.isDebugEnabled()) {
                logger.debug("IN-Dialog INVITE received: "+request.getRequestURI().toString());
            }
            CreateMediaSession command = generateRequest(request);
            msController.tell(command, self());
        }
    }

    private CreateMediaSession generateRequest(SipServletMessage sipMessage) throws IOException, SdpException, ServletParseException {
        final byte[] sdp = sipMessage.getRawContent();
        String offer = SdpUtils.getSdp(sipMessage.getContentType(), sipMessage.getRawContent());
        if (!disableSdpPatchingOnUpdatingMediaSession) {
            String externalIp = null;
            final SipURI externalSipUri = (SipURI) sipMessage.getSession().getAttribute("realInetUri");
            if (externalSipUri != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("ExternalSipUri stored in the sip session : " + externalSipUri.toString() + " will use host: " + externalSipUri.getHost().toString());
                }
                externalIp = externalSipUri.getHost().toString();
            } else {
                externalIp = sipMessage.getInitialRemoteAddr();
                if (logger.isInfoEnabled()) {
                    logger.info("ExternalSipUri stored in the session was null, will use the message InitialRemoteAddr: " + externalIp);
                }
            }
            offer = SdpUtils.patch(sipMessage.getContentType(), sdp, externalIp);
        }
        return new CreateMediaSession("sendrecv", offer, false, webrtc, inboundCallSid);
    }

    private final class UpdatingMediaSession extends AbstractAction {

        public UpdatingMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (is(dialing) || is(ringing)) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }

            final SipServletResponse response = (SipServletResponse) message;
            // Issue 99: https://bitbucket.org/telestax/telscale-restcomm/issue/99
            if (response.getStatus() == SipServletResponse.SC_OK && isOutbound()) {
                String initialIpBeforeLB = null;
                String initialPortBeforeLB = null;
                try {
                    initialIpBeforeLB = response.getHeader("X-Sip-Balancer-InitialRemoteAddr");
                    initialPortBeforeLB = response.getHeader("X-Sip-Balancer-InitialRemotePort");
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Exception during check of LB custom headers for IP address and port");
                    }
                }
                final SipServletRequest ack = response.createAck();
                addCustomHeaders(ack);
                SipSession session = response.getSession();

                if (initialIpBeforeLB != null ) {
                    if (initialPortBeforeLB == null)
                        initialPortBeforeLB = "5060";
                    if (logger.isDebugEnabled()) {
                        logger.debug("We are behind load balancer, checking if the request URI needs to be patched");
                    }
                    String realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                    SipURI uri = factory.createSipURI(null, realIP);
                    boolean patchRURI = true;
                    try {
                        // https://github.com/RestComm/Restcomm-Connect/issues/1336 checking if the initial IP and Port behind LB is part of the route set or not
                        ListIterator<? extends Address> routes = ack.getAddressHeaders(RouteHeader.NAME);
                        while(routes.hasNext() && patchRURI) {
                            SipURI route = (SipURI) routes.next().getURI();
                            String routeHost = route.getHost();
                            int routePort = route.getPort();
                            if(routePort < 0) {
                                routePort = 5060;
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("Checking if route " + routeHost + ":" + routePort + " is matching ip and port before LB " + initialIpBeforeLB + ":"
                                    + initialPortBeforeLB + " for the ACK request");
                            }
                            if(routeHost.equalsIgnoreCase(initialIpBeforeLB) && routePort == Integer.parseInt(initialPortBeforeLB)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("route " + route + " is matching ip and port before LB " + initialIpBeforeLB + ":"
                                        + initialPortBeforeLB + " for the ACK request, so not patching the Request-URI");
                                }
                                patchRURI = false;
                            }
                        }
                    } catch (ServletParseException e) {
                        logger.error("Impossible to parse the route set from the ACK " + ack, e);
                    }
                    if(patchRURI) {
                        if(logger.isDebugEnabled()) {
                            logger.debug("We are behind load balancer, will use: " + initialIpBeforeLB + ":"
                                    + initialPortBeforeLB + " for ACK message, ");
                        }
                        ack.setRequestURI(uri);
                    }
                } else if (!ack.getHeaders("Route").hasNext()) {
                    final SipServletRequest originalInvite = response.getRequest();
                    final SipURI realInetUri = (SipURI) originalInvite.getRequestURI();
                    if ((SipURI) session.getAttribute("realInetUri") == null) {
//                  session.setAttribute("realInetUri", factory.createSipURI(null, realInetUri.getHost()+":"+realInetUri.getPort()));
                        session.setAttribute("realInetUri", realInetUri);
                    }
                    final InetAddress ackRURI = InetAddress.getByName(((SipURI) ack.getRequestURI()).getHost());
                    final int ackRURIPort = ((SipURI) ack.getRequestURI()).getPort();

                    if (realInetUri != null
                            && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())
                            && (ackRURIPort != realInetUri.getPort())) {
                    if(logger.isInfoEnabled()) {
                        logger.info("Using the real ip address and port of the sip client " + realInetUri.toString()
                                + " as a request uri of the ACK");
                    }

                        ack.setRequestURI(realInetUri);
                    }
                }
                ack.send();
                if(logger.isInfoEnabled()) {
                    logger.info("Just sent out ACK : " + ack.toString());
                }
            }

            //Set Call created time, only for "Talk time".
            callUpdatedTime = DateTime.now();

            //Update CDR for Outbound Call.
            if (recordsDao != null) {
                if (outgoingCallRecord != null && isOutbound()) {
                    final int seconds = (int) ((DateTime.now().getMillis() - outgoingCallRecord.getStartTime().getMillis()) / 1000);
                    outgoingCallRecord = outgoingCallRecord.setRingDuration(seconds);
                    recordsDao.updateCallDetailRecord(outgoingCallRecord);
                    outgoingCallRecord = outgoingCallRecord.setStartTime(DateTime.now());
                    recordsDao.updateCallDetailRecord(outgoingCallRecord);
                    outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                    recordsDao.updateCallDetailRecord(outgoingCallRecord);
                }
            }

            String answer = null;
            if (!disableSdpPatchingOnUpdatingMediaSession) {
                if (logger.isInfoEnabled()) {
                    logger.info("Will patch SDP answer from 200 OK received with the external IP Address from Response on updating media session");
                }
                final String externalIp = response.getInitialRemoteAddr();
                final byte[] sdp = response.getRawContent();
                answer = SdpUtils.patch(response.getContentType(), sdp, externalIp);
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("SDP Patching on updating media session is disabled");
                }
                answer = SdpUtils.getSdp(response.getContentType(), response.getRawContent());
            }

            final UpdateMediaSession update = new UpdateMediaSession(answer);
            msController.tell(update, source);
        }
    }

    private final class WaitingForAnswer extends AbstractAction {

        public WaitingForAnswer(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Notify the observers.
            if (external != null && !external.equals(CallStateChanged.State.WAIT_FOR_ANSWER)) {
                external = CallStateChanged.State.WAIT_FOR_ANSWER;
                final CallStateChanged event = new CallStateChanged(external);
                for (final ActorRef observer : observers) {
                    observer.tell(event, source);
                }
            }
        }
    }

    private final class InProgress extends AbstractAction {

        public InProgress(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // Notify the observers.
            if (external != null && !external.equals(CallStateChanged.State.IN_PROGRESS)) {
                external = CallStateChanged.State.IN_PROGRESS;
                final CallStateChanged event = new CallStateChanged(external);
                for (final ActorRef observer : observers) {
                    observer.tell(event, source);
                }

                // Record call data
                if (outgoingCallRecord != null && isOutbound() && !outgoingCallRecord.getStatus().equalsIgnoreCase("in_progress")) {
                    outgoingCallRecord = outgoingCallRecord.setStatus(external.toString());
                    outgoingCallRecord = outgoingCallRecord.setAnsweredBy(to.getUser());

                    if (conferencing) {
                        outgoingCallRecord = outgoingCallRecord.setConferenceSid(conferenceSid);
                        outgoingCallRecord = outgoingCallRecord.setMuted(muted);
                    }
                    recordsDao.updateCallDetailRecord(outgoingCallRecord);
                }
            }
        }
    }

    private final class Joining extends AbstractAction {

        public Joining(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            msController.tell(message, super.source);
        }

    }

    private final class Leaving extends AbstractAction {

        public Leaving(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            Leave leaveMsg = (Leave) message;
            if (!leaveMsg.isLiveCallModification()) {
                if (!receivedBye) {
                    // Conference was stopped and this call was asked to leave
                    // Send BYE to remote client
                    sendBye(new Hangup("Conference time limit reached"));
                }
            } else {
                liveCallModification = true;
            }
            msController.tell(message, self());
        }

    }

    private final class Stopping extends AbstractAction {

        public Stopping(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Stops media operations and closes media session
            msController.tell(new CloseMediaSession(), source);
        }
    }

    private final class Completed extends AbstractAction {

        public Completed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if(logger.isInfoEnabled()) {
                logger.info("Completing Call sid: "+id+" from: "+from+" to: "+to+" direction: "+direction+" current external state: "+external);
            }

            //In the case of canceled that reach the completed method, don't change the external state
            if (!external.equals(CallStateChanged.State.CANCELED)) {
                // Notify the observers.
                external = CallStateChanged.State.COMPLETED;
            }
            CallStateChanged event = new CallStateChanged(external);
            if (external.equals(CallStateChanged.State.CANCELED)) {
                event = new CallStateChanged(external);
            }
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            if(logger.isInfoEnabled()) {
                logger.info("Call sid: "+id+" from: "+from+" to: "+to+" direction: "+direction+" new external state: "+external);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.toString());
                final DateTime now = DateTime.now();
                outgoingCallRecord = outgoingCallRecord.setEndTime(now);
                final int seconds = (int) ((now.getMillis() - outgoingCallRecord.getStartTime().getMillis()) / 1000);
                outgoingCallRecord = outgoingCallRecord.setDuration(seconds);
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
                if(logger.isDebugEnabled()) {
                    logger.debug("Start: " + outgoingCallRecord.getStartTime());
                    logger.debug("End: " + outgoingCallRecord.getEndTime());
                    logger.debug("Duration: " + seconds);
                    logger.debug("Just updated CDR for completed call");
                }
            }
        }
    }

    /*
     * EVENTS
     */
    private void onRecord(Record message, ActorRef self, ActorRef sender) {
        if (is(inProgress)) {
            // Forward to media server controller
            this.recording = true;
            this.msController.tell(message, sender);
        }
    }

    private void onPlay(Play message, ActorRef self, ActorRef sender) {
        if (is(inProgress) || is(waitingForAnswer)) {
            // Forward to media server controller
            this.msController.tell(message, sender);
        }
    }

    private void onCollect(Collect message, ActorRef self, ActorRef sender) {
        if (is(inProgress)) {
            collectTimeout = message.timeout();
            collectFinishKey = message.endInputKey();
            // Forward to media server controller
            this.msController.tell(message, sender);
        }
    }

    private void onStopMediaGroup(StopMediaGroup message, ActorRef self, ActorRef sender) {
        if (is(inProgress) || is(waitingForAnswer)) {
            // Forward to media server controller
            this.msController.tell(message, sender);
            if (conferencing && message.isLiveCallModification()) {
                liveCallModification = true;
                self().tell(new Leave(true), self());
            }
        }
    }

    private void onMute(Mute message, ActorRef self, ActorRef sender) {
        if (is(inProgress)) {
            // Forward to media server controller
            this.msController.tell(message, sender);
            muted = true;
        }
    }

    private void onUnmute(Unmute message, ActorRef self, ActorRef sender) {
        if (is(inProgress) && muted) {
            // Forward to media server controller
            this.msController.tell(message, sender);
            muted = false;
            if (logger.isInfoEnabled()) {
                final String infoMsg = String.format("Call %s, direction %s, unmuted", self().path(), direction);
                logger.info(infoMsg);
            }
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

    private void onStopObserving(StopObserving stopObservingMessage, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = stopObservingMessage.observer();
        if (observer != null) {
            observer.tell(stopObservingMessage, self);
            this.observers.remove(observer);
        } else {
            Iterator<ActorRef> observerIter = observers.iterator();
            while (observerIter.hasNext()) {
                ActorRef observerNext = observerIter.next();
                observerNext.tell(stopObservingMessage, self);
                if(logger.isInfoEnabled()) {
                    logger.info("Sent stop observing for call, from: "+from+" to: "+to+" direction: "+direction+" to observer: "+observerNext.path()+" observer is terminated: "+observerNext.isTerminated());
                }

//                this.observers.remove(observerNext);
            }
            this.observers.clear();
        }
    }

    private void onGetCallObservers(GetCallObservers message, ActorRef self, ActorRef sender) throws Exception {
        sender.tell(new CallResponse<List<ActorRef>>(this.observers), self);
    }

    private void onGetCallInfo(GetCallInfo message, ActorRef sender) throws Exception {
        sender.tell(info(), self());
    }

    private void onInitializeOutbound(InitializeOutbound message, ActorRef self, ActorRef sender) throws Exception {
        if (is(uninitialized)) {
            fsm.transition(message, queued);
        }
    }

    private void onChangeCallDirection(ChangeCallDirection message, ActorRef self, ActorRef sender) {
        // Needed for LiveCallModification API where the outgoing call also needs to move to the new destination.
        this.direction = INBOUND;
        this.liveCallModification = true;
        this.conferencing = false;
        this.conference = null;
        this.bridge = null;
    }

    private void onAnswer(Answer message, ActorRef self, ActorRef sender) throws Exception {
        inboundCallSid = message.callSid();
        inboundConfirmCall = message.confirmCall();
        if (is(ringing) && !invite.getSession().getState().equals(SipSession.State.TERMINATED)) {
                fsm.transition(message, initializing);
        } else {
            fsm.transition(message, canceled);
        }
    }

    private void onDial(Dial message, ActorRef self, ActorRef sender) throws Exception {
        if (is(queued)) {
            fsm.transition(message, initializing);
        }
    }

    private void onReject(Reject message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ringing)) {
            fsm.transition(message, busy);
        }
    }

    private void onCancel(Cancel message, ActorRef self, ActorRef sender) throws Exception {
        if (is(initializing) || is(dialing) || is(ringing) || is(failingNoAnswer)) {
            if(logger.isInfoEnabled()) {
                logger.info("Got CANCEL for Call with the following details, from: "+from+" to: "+to+" direction: "+direction+" state: "+fsm.state()+", will Cancel the call");
            }
            fsm.transition(message, canceling);
        } else if (is(inProgress)) {
            if(logger.isInfoEnabled()) {
                logger.info("Got CANCEL for Call with the following details, from: "+from+" to: "+to+" direction: "+direction+" state: "+fsm.state()+", will Hangup the call");
            }
            onHangup(new Hangup(), self(), sender());
        } else {
            if(logger.isInfoEnabled()) {
                logger.info("Got CANCEL for Call with the following details, from: "+from+" to: "+to+" direction: "+direction+" state: "+fsm.state());
            }
        }
    }

    private void onReceiveTimeout(ReceiveTimeout message, ActorRef self, ActorRef sender) throws Exception {
        getContext().setReceiveTimeout(Duration.Undefined());
        if (is(ringing) || is(dialing)) {
            fsm.transition(message, failingNoAnswer);
        } else if(is(inProgress) && collectSipInfoDtmf) {
            if (logger.isInfoEnabled()) {
                logger.info("Collecting DTMF with SIP INFO, inter digit timeout fired. Will send finishKey to observers");
            }
            MediaGroupResponse<String> infoResponse = new MediaGroupResponse<String>(collectFinishKey);
            for (final ActorRef observer : observers) {
                observer.tell(infoResponse, self());
            }

        } else if(logger.isInfoEnabled()) {
            logger.info("Timeout received for Call : "+self().path()+" isTerminated(): "+self().isTerminated()+". Sender: " + sender.path().toString() + " State: " + this.fsm.state()
                + " Direction: " + direction + " From: " + from + " To: " + to);
        }
    }

    private void onSipServletRequest(SipServletRequest message, ActorRef self, ActorRef sender) throws Exception {
        final String method = message.getMethod();
        if ("INVITE".equalsIgnoreCase(method)) {
            if (is(uninitialized)) {
                fsm.transition(message, ringing);
            } if (is(inProgress)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("IN-Dialog INVITE received: "+message.getRequestURI().toString());
                }

                inDialogInvite = message;

                String answer = null;
                if (!disableSdpPatchingOnUpdatingMediaSession) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Will patch SDP answer from 200 OK received with the external IP Address from Response on updating media session");
                    }
                    final String externalIp = message.getInitialRemoteAddr();
                    final byte[] sdp = message.getRawContent();
                    answer = SdpUtils.patch(message.getContentType(), sdp, externalIp);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("SDP Patching on updating media session is disabled");
                    }
                    answer = SdpUtils.getSdp(message.getContentType(), message.getRawContent());
                }

                final UpdateMediaSession update = new UpdateMediaSession(answer);
                msController.tell(update, self());
            }
        } else if ("CANCEL".equalsIgnoreCase(method)) {
            if (is(initializing)) {
                fsm.transition(message, canceling);
            } else if ((is(ringing) || is(waitingForAnswer)) && isInbound()) {
                fsm.transition(message, canceling);
            }
            // XXX can receive SIP cancel any other time?
        } else if ("BYE".equalsIgnoreCase(method)) {
            // Reply to BYE with OK
            this.receivedBye = true;
            final SipServletRequest bye = (SipServletRequest) message;
            final SipServletResponse okay = bye.createResponse(SipServletResponse.SC_OK);
            okay.send();

            // Stop recording if necessary
            if (recording) {
                if (!direction.contains("outbound")) {
                    // Initial Call sent BYE
                    recording = false;
                    if(logger.isInfoEnabled()) {
                        logger.info("Call Direction: " + direction);
                        logger.info("Initial Call - Will stop recording now");
                    }
                    msController.tell(new Stop(false), self);
                    // VoiceInterpreter will take care to prepare the Recording object
                } else if (conference != null) {
                    // Outbound call sent BYE. !Important conference is the initial call here.
                    conference.tell(new StopRecording(accountId, runtimeSettings, daoManager), null);
                }
            }

            if (conferencing) {
                // Tell conference to remove the call from participants list
                // before moving to a stopping state
                conference.tell(new RemoveParticipant(self), self);
            } else {
                // Clean media resources as necessary
                if (!is(completed))
                    fsm.transition(message, stopping);
            }
        } else if ("INFO".equalsIgnoreCase(method)) {
            processInfo(message);
        } else if ("ACK".equalsIgnoreCase(method)) {
            if (isInbound() && (is(initializing) || is(waitingForAnswer))) {
                if(logger.isInfoEnabled()) {
                    logger.info("ACK received moving state to inProgress");
                }
                fsm.transition(message, inProgress);
            }
        }
    }

    private void onSipServletResponse(SipServletResponse message, ActorRef self, ActorRef sender) throws Exception {
        this.lastResponse = message;

        final int code = message.getStatus();
        switch (code) {
            case SipServletResponse.SC_CALL_BEING_FORWARDED: {
                forwarding(message);
                break;
            }
            case SipServletResponse.SC_RINGING:
            case SipServletResponse.SC_SESSION_PROGRESS: {
                if (!is(ringing)) {
                    if(logger.isInfoEnabled()) {
                        logger.info("Got 180 Ringing for Call: "+self().path()+" To: "+to+" sender: "+sender.path()+" observers size: "+observers.size());
                    }
                    fsm.transition(message, ringing);
                }
                break;
            }
            case SipServletResponse.SC_BUSY_HERE:
            case SipServletResponse.SC_BUSY_EVERYWHERE:
            case SipServletResponse.SC_DECLINE: {
                sendCallInfoToObservers();

                //Important. If state is DIALING, then do nothing about the BUSY. If not DIALING state move to failingBusy
//                // Notify the observers.
//                external = CallStateChanged.State.BUSY;
//                final CallStateChanged event = new CallStateChanged(external);
//                for (final ActorRef observer : observers) {
//                    observer.tell(event, self);
//                }

                // XXX shouldnt it move to failingBusy IF dialing ????
//                if (is(dialing)) {
//                    break;
//                } else {
//                    fsm.transition(message, failingBusy);
//                }
                fsm.transition(message, failingBusy);
                break;
            }
            case SipServletResponse.SC_UNAUTHORIZED:
            case SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED: {
                // Handles Auth for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                if ((this.username!= null || this.username.isEmpty()) && (this.password != null && this.password.isEmpty())) {
                    sendCallInfoToObservers();
                    fsm.transition(message, failed);
                } else {
                    AuthInfo authInfo = this.factory.createAuthInfo();
                    String authHeader = message.getHeader("Proxy-Authenticate");
                    if (authHeader == null) {
                        authHeader = message.getHeader("WWW-Authenticate");
                    }
                    String tempRealm = authHeader.substring(authHeader.indexOf("realm=\"") + "realm=\"".length());
                    String realm = tempRealm.substring(0, tempRealm.indexOf("\""));
                    authInfo.addAuthInfo(message.getStatus(), realm, this.username, this.password);
                    SipServletRequest challengeRequest = message.getSession().createRequest(message.getRequest().getMethod());
                    challengeRequest.addAuthHeader(message, authInfo);
                    challengeRequest.setContent(this.invite.getContent(), this.invite.getContentType());
                    this.invite = challengeRequest;
                    // https://github.com/Mobicents/RestComm/issues/147 Make sure we send the SDP again
                    this.invite.setContent(message.getRequest().getContent(), "application/sdp");
                    challengeRequest.send();
                }
                break;
            }
            // https://github.com/Mobicents/RestComm/issues/148
            // Session in Progress Response should trigger MMS to start the Media Session
            // case SipServletResponse.SC_SESSION_PROGRESS:
            case SipServletResponse.SC_OK: {
                if (is(dialing) || (is(ringing) && !"inbound".equals(direction))) {
                    fsm.transition(message, updatingMediaSession);
                }
                break;
            }
            default: {
                if (code >= 400 && code != 487) {
                    if (code == 487 && isOutbound()) {
                            String initialIpBeforeLB = null;
                            String initialPortBeforeLB = null;
                            try {
                                initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
                                initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");
                            } catch (Exception e) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Exception during check of LB custom headers for IP address and port");
                                }
                            }
                        final SipServletRequest ack = message.createAck();
                        addCustomHeaders(ack);
                        SipSession session = message.getSession();

                        if (initialIpBeforeLB != null ) {
                            if (initialPortBeforeLB == null)
                                initialPortBeforeLB = "5060";
                            if(logger.isInfoEnabled()) {
                                logger.info("We are behind load balancer, will use: " + initialIpBeforeLB + ":"
                                        + initialPortBeforeLB + " for ACK message, ");
                            }
                            String realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                            SipURI uri = factory.createSipURI(null, realIP);
                            ack.setRequestURI(uri);
                        } else if (!ack.getHeaders("Route").hasNext()) {
                            final SipServletRequest originalInvite = message.getRequest();
                            final SipURI realInetUri = (SipURI) originalInvite.getRequestURI();
                            if ((SipURI) session.getAttribute("realInetUri") == null) {
                                session.setAttribute("realInetUri", realInetUri);
                            }
                            final InetAddress ackRURI = InetAddress.getByName(((SipURI) ack.getRequestURI()).getHost());
                            final int ackRURIPort = ((SipURI) ack.getRequestURI()).getPort();

                            if (realInetUri != null
                                    && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())
                                    && (ackRURIPort != realInetUri.getPort())) {
                                if(logger.isInfoEnabled()) {
                                    logger.info("Using the real ip address and port of the sip client " + realInetUri.toString()
                                            + " as a request uri of the ACK");
                                }
                                ack.setRequestURI(realInetUri);
                            }
                        }
                        ack.send();
                        if(logger.isInfoEnabled()) {
                            logger.info("Just sent out ACK : " + ack.toString());
                        }
                    }
                    this.fail = true;
                    fsm.transition(message, stopping);
                }
            }
        }
    }

    private void onHangup(Hangup message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("Got Hangup: "+message+" for Call, from: "+from+" to: "+to+" state: "+fsm.state()+" conferencing: "+conferencing +" conference: "+conference);
        }

        // Stop recording if necessary
        if (recording) {
            recording = false;
            if(logger.isInfoEnabled()) {
                logger.info("Call - Will stop recording now");
            }
            msController.tell(new Stop(true), self);
        }

        if (is(updatingMediaSession) || is(ringing) || is(queued) || is(dialing) || is(inProgress) || is(completed) || is(waitingForAnswer)) {
            if (conferencing) {
                // Tell conference to remove the call from participants list
                // before moving to a stopping state
                conference.tell(new RemoveParticipant(self()), self());
            }else {
                if (!receivedBye && !sentBye) {
                    // Send BYE to client if RestComm took initiative to hangup the call
                    sendBye(message);
                }

                // Move to next state to clean media resources and close session
                fsm.transition(message, stopping);
            }
        }
    }

    private void sendBye(Hangup hangup) throws IOException, TransitionNotFoundException, TransitionFailedException, TransitionRollbackException {
        final SipSession session = invite.getSession();
        final String sessionState = session.getState().name();
        if (sessionState == SipSession.State.TERMINATED.name()) {
            if (logger.isInfoEnabled()) {
                logger.info("SipSession already TERMINATED, will not send BYE");
            }
            return;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("About to send BYE, session state: " + sessionState);
            }
        }
        if (sessionState == SipSession.State.INITIAL.name() || (sessionState == SipSession.State.EARLY.name() && isInbound())) {
            int sipResponse = (enable200OkDelay && hangup.getSipResponse() != null) ? hangup.getSipResponse() : Response.SERVER_INTERNAL_ERROR;
            final SipServletResponse resp = invite.createResponse(sipResponse);
            if (hangup.getMessage() != null && !hangup.getMessage().equals("")) {
                resp.addHeader("Reason",hangup.getMessage());
            }
            addCustomHeaders(resp);
            resp.send();
            fsm.transition(hangup, completed);
            return;
        } if (sessionState == SipSession.State.EARLY.name()) {
            final SipServletRequest cancel = invite.createCancel();
            if (hangup.getMessage() != null && !hangup.getMessage().equals("")) {
                cancel.addHeader("Reason",hangup.getMessage());
            }
            addCustomHeaders(cancel);
            cancel.send();
            external = CallStateChanged.State.CANCELED;
            fsm.transition(hangup, completed);
            return;
        } else {
            final SipServletRequest bye = session.createRequest("BYE");
            addCustomHeaders(bye);
            if (hangup.getMessage() != null && !hangup.getMessage().equals("")) {
                bye.addHeader("Reason",hangup.getMessage());
            }
            SipURI realInetUri = (SipURI) session.getAttribute("realInetUri");
            InetAddress byeRURI = InetAddress.getByName(((SipURI) bye.getRequestURI()).getHost());

            // INVITE sip:+12055305520@107.21.247.251 SIP/2.0
            // Record-Route: <sip:10.154.28.245:5065;transport=udp;lr;node_host=10.13.169.214;node_port=5080;version=0>
            // Record-Route: <sip:10.154.28.245:5060;transport=udp;lr;node_host=10.13.169.214;node_port=5080;version=0>
            // Record-Route: <sip:67.231.8.195;lr=on;ftag=gK0043eb81>
            // Record-Route: <sip:67.231.4.204;r2=on;lr=on;ftag=gK0043eb81>
            // Record-Route: <sip:192.168.6.219;r2=on;lr=on;ftag=gK0043eb81>
            // Accept: application/sdp
            // Allow: INVITE,ACK,CANCEL,BYE
            // Via: SIP/2.0/UDP 10.154.28.245:5065;branch=z9hG4bK1cdb.193075b2.058724zsd_0
            // Via: SIP/2.0/UDP 10.154.28.245:5060;branch=z9hG4bK1cdb.193075b2.058724_0
            // Via: SIP/2.0/UDP 67.231.8.195;branch=z9hG4bK1cdb.193075b2.0
            // Via: SIP/2.0/UDP 67.231.4.204;branch=z9hG4bK1cdb.f9127375.0
            // Via: SIP/2.0/UDP 192.168.16.114:5060;branch=z9hG4bK00B6ff7ff87ed50497f
            // From: <sip:+1302109762259@192.168.16.114>;tag=gK0043eb81
            // To: <sip:12055305520@192.168.6.219>
            // Call-ID: 587241765_133360558@192.168.16.114
            // CSeq: 393447729 INVITE
            // Max-Forwards: 67
            // Contact: <sip:+1302109762259@192.168.16.114:5060>
            // Diversion: <sip:+112055305520@192.168.16.114:5060>;privacy=off;screen=no; reason=unknown; counter=1
            // Supported: replaces
            // Content-Disposition: session;handling=required
            // Content-Type: application/sdp
            // Remote-Party-ID: <sip:+1302109762259@192.168.16.114:5060>;privacy=off;screen=no
            // X-Sip-Balancer-InitialRemoteAddr: 67.231.8.195
            // X-Sip-Balancer-InitialRemotePort: 5060
            // Route: <sip:10.13.169.214:5080;transport=udp;lr>
            // Content-Length: 340

            ListIterator<String> recordRouteList = invite.getHeaders(RecordRouteHeader.NAME);


            if (invite.getHeader("X-Sip-Balancer-InitialRemoteAddr") != null) {
                if(logger.isInfoEnabled()){
                    logger.info("We are behind LoadBalancer and will remove the first two RecordRoutes since they are the LB node");
                }
                recordRouteList.next();
                recordRouteList.remove();
                recordRouteList.next();
                recordRouteList.remove();
            }
            if (recordRouteList.hasNext()) {
                if(logger.isInfoEnabled()) {
                    logger.info("Record Route is set, wont change the Request URI");
                }
            } else {
                if(logger.isInfoEnabled()) {
                    logger.info("Checking RURI, realInetUri: " + realInetUri + " byeRURI: " + byeRURI);
                }
                if(logger.isDebugEnabled()) {
                    logger.debug("byeRURI.isSiteLocalAddress(): " + byeRURI.isSiteLocalAddress());
                    logger.debug("byeRURI.isAnyLocalAddress(): " + byeRURI.isAnyLocalAddress());
                    logger.debug("byeRURI.isLoopbackAddress(): " + byeRURI.isLoopbackAddress());
                }
                if (realInetUri != null && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    if(logger.isInfoEnabled()) {
                        logger.info("real ip address of the sip client " + realInetUri.toString()
                            + " is not null, checking if the request URI needs to be patched");
                    }
                    boolean patchRURI = true;
                    try {
                        // https://github.com/RestComm/Restcomm-Connect/issues/1336 checking if the initial IP and Port behind LB is part of the route set or not
                        ListIterator<? extends Address> routes = bye.getAddressHeaders(RouteHeader.NAME);
                        while(routes.hasNext() && patchRURI) {
                            SipURI route = (SipURI) routes.next().getURI();
                            String routeHost = route.getHost();
                            int routePort = route.getPort();
                            if(routePort < 0) {
                                routePort = 5060;
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("Checking if route " + routeHost + ":" + routePort + " is matching ip and port of realNetURI " + realInetUri.getHost() + ":"
                                    + realInetUri.getPort() + " for the BYE request");
                            }
                            if(routeHost.equalsIgnoreCase(realInetUri.getHost()) && routePort == realInetUri.getPort()) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("route " + route + " is matching ip and port of realNetURI "+ realInetUri.getHost() + ":"
                                     + realInetUri.getPort() + " for the BYE request, so not patching the Request-URI");
                                }
                                patchRURI = false;
                            }
                        }
                    } catch (ServletParseException e) {
                        logger.error("Impossible to parse the route set from the BYE " + bye, e);
                    }
                    if(patchRURI) {
                        if(logger.isInfoEnabled()) {
                             logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                                  + " as a request uri of the BYE request");
                        }
                        bye.setRequestURI(realInetUri);
                    }
                }
            }
            if(logger.isInfoEnabled()) {
                logger.info("Will sent out BYE to: " + bye.getRequestURI());
            }
            try {
                bye.send();
                sentBye = true;
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Exception during Send Bye: "+e.toString());
                }
            }
        }
    }

    private void onNotFound(org.restcomm.connect.telephony.api.NotFound message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(ringing)) {
            fsm.transition(message, notFound);
        }
    }

    private void onMediaServerControllerStateChanged(MediaServerControllerStateChanged message, ActorRef self, ActorRef sender)
            throws Exception {
        if(logger.isInfoEnabled()) {
            logger.info("onMediaServerControllerStateChanged " + message.getState()
                 + " inboundConfirmCall " + inboundConfirmCall);
       }
        switch (message.getState()) {
            case PENDING:
                if (is(initializing)) {
                    fsm.transition(message, dialing);
                }
                break;

            case ACTIVE:
                if (is(initializing) || is(updatingMediaSession)) {
                    SipSession.State sessionState = invite.getSession().getState();
                    if (!(SipSession.State.CONFIRMED.equals(sessionState) || SipSession.State.TERMINATED.equals(sessionState))) {
                        // Issue #1649:
                        mediaSessionInfo = message.getMediaSession();
                        if(inboundConfirmCall){
                            sendInviteOk();
                        }
                        else{
                            fsm.transition(message, waitingForAnswer);
                        }
                    } else if (SipSession.State.CONFIRMED.equals(sessionState) && is(inProgress)) {
                        // We have an ongoing call and Restcomm executes new RCML app on that
                        // If the sipSession state is Confirmed, then update SDP with the new SDP from MMS
                        SipServletRequest reInvite = invite.getSession().createRequest("INVITE");
                        addCustomHeaders(reInvite);
                        mediaSessionInfo = message.getMediaSession();
                        final byte[] sdp = mediaSessionInfo.getLocalSdp().getBytes();
                        String answer = null;
                        if (mediaSessionInfo.usesNat()) {
                            final String externalIp = mediaSessionInfo.getExternalAddress().getHostAddress();
                            answer = SdpUtils.patch("application/sdp", sdp, externalIp);
                        } else {
                            answer = mediaSessionInfo.getLocalSdp().toString();
                        }

                        // Issue #215:
                        // https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
                        answer = SdpUtils.endWithNewLine(answer);

                        reInvite.setContent(answer, "application/sdp");
                        reInvite.send();
                    }

                    // Make sure the SIP session doesn't end pre-maturely.
                    invite.getApplicationSession().setExpires(0);

                    if(isInbound()){
                        if(logger.isInfoEnabled()) {
                            logger.info("current state: "+fsm.state()+" , will wait for OK to move to inProgress");
                        }
                    }
                    else{
                        fsm.transition(message, inProgress);
                    }

                } else if(is(inProgress) && inDialogRequest != null) {
                    mediaSessionInfo = message.getMediaSession();
                    final byte[] sdp = mediaSessionInfo.getLocalSdp().getBytes();
                    String answer = null;
                    if (mediaSessionInfo.usesNat()) {
                        final String externalIp = mediaSessionInfo.getExternalAddress().getHostAddress();
                        answer = SdpUtils.patch("application/sdp", sdp, externalIp);
                    } else {
                        answer = mediaSessionInfo.getLocalSdp().toString();
                    }

                    // Issue #215:
                    // https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
                    answer = SdpUtils.endWithNewLine(answer);
                    SipServletResponse resp = inDialogInvite.createResponse(Response.OK);
                    resp.setContent(answer, "application/sdp");
                    resp.send();
                }
                break;

            case INACTIVE:
                if (is(stopping)) {
                    if (fail) {
                        fsm.transition(message, failed);
                    } else {
                        fsm.transition(message, completed);
                    }
                } else if (is(canceling)) {
                    fsm.transition(message, canceled);
                } else if (is(failingBusy)) {
                    fsm.transition(message, busy);
                } else if (is(failingNoAnswer)) {
                    fsm.transition(message, noAnswer);
                }
                break;

            case FAILED:
                if (is(initializing) || is(updatingMediaSession) || is(joining) || is(leaving)) {
                    fsm.transition(message, failed);
                }
                break;

            default:
                break;
        }
    }

    private void onJoinBridge(JoinBridge message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress) || is(waitingForAnswer)) {
            this.bridge = sender;
            this.fsm.transition(message, joining);
        }
    }

    private void onJoinConference(JoinConference message, ActorRef self, ActorRef sender) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("********************* onJoinConference *********************");
        }
        if (is(inProgress)) {
            this.conferencing = true;
            this.conference = sender;
            this.conferenceSid = message.getSid();
            this.fsm.transition(message, joining);
        }
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) throws Exception {
        //The CallController will send to the Call the JoinComplete message when the join completes
        if (is(joining)) {
            // Forward message to the bridge
            if (conferencing) {
                if (outgoingCallRecord != null && isOutbound()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Updating CDR for outgoing call: "+id.toString()+", call status: "+external.name()+", to include Conference details, conference: "+conferenceSid);
                    }
                    outgoingCallRecord = outgoingCallRecord.setConferenceSid(conferenceSid);
                    outgoingCallRecord = outgoingCallRecord.setMuted(muted);

                    recordsDao.updateCallDetailRecord(outgoingCallRecord);
                }
                this.conference.tell(message, self);
            } else {
                this.bridge.tell(message, self);
            }

            // Move to state In Progress
            fsm.transition(message, inProgress);
        }
    }

    private void onLeave(Leave message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress)) {
            fsm.transition(message, leaving);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Received Leave for Call: "+self.path()+", but state is :"+fsm.state().toString());
            }
        }
    }

    private void onLeft(Left message, ActorRef self, ActorRef sender) throws Exception {
        if (is(leaving)) {
            if (conferencing) {
                // Let conference know the call exited the room
                this.conferencing = false;
                this.conference.tell(new Left(self()), self);
                this.conference = null;
                if (logger.isDebugEnabled()) {
                    logger.debug("Call left conference room and notification sent to conference actor");
                }
            }

            if (!liveCallModification) {
                // After leaving let the Interpreter know the Call is ready.
                fsm.transition(message, completed);
            } else {
                if (muted) {
                    // Forward to media server controller
                    this.msController.tell(new Unmute(), sender);
                    muted = false;
                }
                if (!receivedBye) {
                    fsm.transition(message, inProgress);
                } else {
                    fsm.transition(message, completed);
                }
            }
        }
    }

    private void onStartRecordingCall(StartRecording message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress)) {
            if (runtimeSettings == null) {
                this.runtimeSettings = message.getRuntimeSetting();
            }

            if (daoManager == null) {
                daoManager = message.getDaoManager();
            }

            if (accountId == null) {
                accountId = message.getAccountId();
            }

            // Forward message for Media Session Controller to handle
            message.setCallId(this.id);
            this.msController.tell(message, sender);
            this.recording = true;
        }
    }

    private void onStopRecordingCall(StopRecording message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress) && this.recording) {
            // Forward message for Media Session Controller to handle
            this.msController.tell(message, sender);
            this.recording = false;
        }
    }

    private void onBridgeStateChanged(BridgeStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress) && isInbound() && enable200OkDelay) {
            switch (message.getState()) {
                case BRIDGED:
                    sendInviteOk();
                    break;
                case FAILED:
                    fsm.transition(message, stopping);
                default:
                    break;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Received BridgeStateChanged for Call: "+self.path()+", but state is :"+fsm.state().toString());
            }
        }
    }

    private void sendInviteOk() throws Exception{
        if (logger.isInfoEnabled()) {
            logger.info("sending initial invite ok,  initialInviteOkSent:"+ initialInviteOkSent);
        }
        if(!initialInviteOkSent){
            final SipServletResponse okay = invite.createResponse(SipServletResponse.SC_OK);
            final byte[] sdp = mediaSessionInfo.getLocalSdp().getBytes();
            String answer = null;
            if (mediaSessionInfo.usesNat()) {
                final String externalIp = mediaSessionInfo.getExternalAddress().getHostAddress();
                answer = SdpUtils.patch("application/sdp", sdp, externalIp);
            } else {
                answer = mediaSessionInfo.getLocalSdp().toString();
            }
            // Issue #215:
            // https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
            answer = SdpUtils.endWithNewLine(answer);
            okay.setContent(answer, "application/sdp");
            addCustomHeaders(okay);
            okay.send();
            initialInviteOkSent = true;
        }
    }

    @Override
    public void postStop() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Call actor at postStop, path: "+self().path()+", direction: "+direction+", state: "+fsm.state()+", isTerminated: "+self().isTerminated()+", sender: "+sender());
            }
            onStopObserving(new StopObserving(), self(), null);
            getContext().stop(msController);
        } catch (Exception exception) {
            if(logger.isInfoEnabled()) {
                logger.info("Exception during Call postStop while trying to remove observers: "+exception);
            }
        }
        super.postStop();
    }
}

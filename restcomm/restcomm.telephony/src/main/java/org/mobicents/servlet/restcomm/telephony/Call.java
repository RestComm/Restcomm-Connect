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

package org.mobicents.servlet.restcomm.telephony;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

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

import org.joda.time.DateTime;
import org.mobicents.javax.servlet.sip.SipSessionExt;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mscontrol.messages.CloseMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.Join;
import org.mobicents.servlet.restcomm.mscontrol.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerError;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionClosed;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaSessionInfo;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartRecordingCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopRecordingCall;
import org.mobicents.servlet.restcomm.mscontrol.messages.UpdateMediaSession;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.util.SdpUtils;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;

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

    // States for the FSM
    private final State uninitialized;
    private final State queued;
    private final State ringing;
    private final State busy;
    private final State notFound;
    private final State canceled;
    private final State noAnswer;
    private final State inProgress;
    private final State completed;
    private final State failed;

    // Intermediate states
    private final State canceling;
    private final State dialing;
    private final State completing;
    private final State failing;
    private final State failingBusy;
    private final State failingNoAnswer;
    private final State creatingMediaSession;
    private final State updatingMediaSession;
    private final State joining;

    // FSM.
    private final FiniteStateMachine fsm;

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
    private SipServletResponse lastResponse;

    // Call runtime stuff.
    private final Sid id;
    private CallStateChanged.State external;
    private String direction;
    private String forwardedFrom;
    private DateTime created;
    private final List<ActorRef> observers;

    private ActorRef conference;
    private ActorRef conferenceController;

    // Media Session Control runtime stuff
    private final ActorRef msController;
    private MediaSessionInfo mediaSessionInfo;

    // Media Group runtime stuff
    private CallDetailRecordsDao recordsDao;
    private CallDetailRecord outgoingCallRecord;
    private ActorRef outboundCall;

    public Call(final SipFactory factory, final ActorRef mediaSessionController) {
        super();
        final ActorRef source = self();

        // States for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.queued = new State("queued", new Queued(source), null);
        this.ringing = new State("ringing", new Ringing(source), null);
        this.busy = new State("busy", new Busy(source), null);
        this.notFound = new State("not found", new NotFound(source), null);
        this.canceled = new State("canceled", new Canceled(source), null);
        this.noAnswer = new State("no answer", new NoAnswer(source), null);
        this.inProgress = new State("in progress", new InProgress(source), null);
        this.completed = new State("completed", new Completed(source), null);
        this.failed = new State("failed", new Failed(source), null);

        // Intermediate states
        this.canceling = new State("canceling", new Canceling(source), null);
        this.dialing = new State("dialing", new Dialing(source), null);
        this.completing = new State("completing", new Completing(source), null);
        this.failing = new State("failing", new Failing(source), null);
        this.failingBusy = new State("failing busy", new FailingBusy(source), null);
        this.failingNoAnswer = new State("failing no answer", new FailingNoAnswer(source), null);
        this.creatingMediaSession = new State("creating media session", new CreatingMediaSession(source), null);
        this.updatingMediaSession = new State("updating media session", new UpdatingMediaSession(source), null);
        this.joining = new State("joining", new Joining(source), null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.ringing));
        transitions.add(new Transition(this.uninitialized, this.queued));
        transitions.add(new Transition(this.queued, this.canceled));
        transitions.add(new Transition(this.queued, this.creatingMediaSession));
        transitions.add(new Transition(this.ringing, this.busy));
        transitions.add(new Transition(this.ringing, this.notFound));
        transitions.add(new Transition(this.ringing, this.canceling));
        transitions.add(new Transition(this.ringing, this.failingNoAnswer));
        transitions.add(new Transition(this.ringing, this.failingBusy));
        transitions.add(new Transition(this.ringing, this.noAnswer));
        transitions.add(new Transition(this.ringing, this.creatingMediaSession));
        transitions.add(new Transition(this.ringing, this.updatingMediaSession));
        transitions.add(new Transition(this.ringing, this.completing));
        transitions.add(new Transition(this.creatingMediaSession, this.canceling));
        transitions.add(new Transition(this.creatingMediaSession, this.dialing));
        transitions.add(new Transition(this.creatingMediaSession, this.failed));
        transitions.add(new Transition(this.creatingMediaSession, this.inProgress));
        transitions.add(new Transition(this.dialing, this.canceling));
        transitions.add(new Transition(this.dialing, this.failing));
        transitions.add(new Transition(this.dialing, this.failingBusy));
        transitions.add(new Transition(this.dialing, this.failingNoAnswer));
        transitions.add(new Transition(this.dialing, this.ringing));
        transitions.add(new Transition(this.dialing, this.updatingMediaSession));
        transitions.add(new Transition(this.inProgress, this.completing));
        transitions.add(new Transition(this.inProgress, this.joining));
        transitions.add(new Transition(this.joining, this.inProgress));
        transitions.add(new Transition(this.joining, this.completing));
        transitions.add(new Transition(this.joining, this.failing));
        transitions.add(new Transition(this.canceling, this.canceled));
        transitions.add(new Transition(this.failing, this.failed));
        transitions.add(new Transition(this.failingBusy, this.busy));
        transitions.add(new Transition(this.failingNoAnswer, this.noAnswer));
        transitions.add(new Transition(this.failingNoAnswer, this.canceling));
        transitions.add(new Transition(this.updatingMediaSession, this.inProgress));
        transitions.add(new Transition(this.updatingMediaSession, this.completing));
        transitions.add(new Transition(this.completing, this.completed));

        // FSM
        this.fsm = new FiniteStateMachine(this.uninitialized, transitions);

        // SIP runtime stuff.
        this.factory = factory;

        // Media Session Control runtime stuff.
        this.msController = mediaSessionController;

        // Initialize the runtime stuff.
        this.id = Sid.generate(Sid.Type.CALL);
        this.created = DateTime.now();
        this.observers = Collections.synchronizedList(new ArrayList<ActorRef>());
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
        final CallInfo info = new CallInfo(id, external, type, direction, created, forwardedFrom, name, from, to, lastResponse);
        return new CallResponse<CallInfo>(info);
    }

    private void forwarding(final Object message) {
        // XXX does nothing
    }

    // Allow updating of the callInfo at the VoiceInterpreter so that we can do Dial SIP Screening
    // (https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out) accurately from latest response
    // received
    private void sendCallInfoToObservers() {
        for (final ActorRef observer : this.observers) {
            observer.tell(info(), self());
        }
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
        String realIP = message.getInitialRemoteAddr();
        Integer realPort = message.getInitialRemotePort();
        if (realPort == null || realPort == -1)
            realPort = 5060;

        final ListIterator<String> recordRouteHeaders = message.getHeaders("Record-Route");
        final Address contactAddr = factory.createAddress(message.getHeader("Contact"));

        InetAddress contactInetAddress = InetAddress.getByName(((SipURI) contactAddr.getURI()).getHost());
        InetAddress inetAddress = InetAddress.getByName(realIP);

        // Issue #332: https://telestax.atlassian.net/browse/RESTCOMM-332
        final String initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
        String initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");

        SipURI uri = null;

        if (initialIpBeforeLB != null) {
            if (initialPortBeforeLB == null)
                initialPortBeforeLB = "5060";
            logger.info("We are behind load balancer, storing Initial Remote Address " + initialIpBeforeLB + ":"
                    + initialPortBeforeLB + " to the session for later use");
            realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
            uri = factory.createSipURI(null, realIP);
        } else if (contactInetAddress.isSiteLocalAddress() && !recordRouteHeaders.hasNext()
                && !contactInetAddress.toString().equalsIgnoreCase(inetAddress.toString())) {
            logger.info("Contact header address " + contactAddr.toString()
                    + " is a private network ip address, storing Initial Remote Address " + realIP + ":" + realPort
                    + " to the session for later use");
            realIP = realIP + ":" + realPort;
            uri = factory.createSipURI(null, realIP);
        }
        return uri;
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

        logger.info("********** Call's Current State: \"" + state.toString());
        logger.info("********** Call Processing Message: \"" + klass.getName() + " sender : " + sender.toString());

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (GetCallObservers.class.equals(klass)) {
            onGetCallObservers((GetCallObservers) message, self, sender);
        } else if (GetCallInfo.class.equals(klass)) {
            onGetCallInfo((GetCallInfo) message, self, sender);
        } else if (InitializeOutbound.class.equals(klass)) {
            onInitializeOutbound((InitializeOutbound) message, self, sender);
        } else if (Answer.class.equals(klass)) {
            onAnswer((Answer) message, self, sender);
        } else if (Dial.class.equals(klass)) {
            onDial((Dial) message, self, sender);
        } else if (Reject.class.equals(klass)) {
            onReject((Reject) message, self, sender);
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
        } else if (org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass)) {
            onNotFound((org.mobicents.servlet.restcomm.telephony.NotFound) message, self, sender);
        } else if (MediaServerControllerResponse.class.equals(klass)) {
            onMediaServerControllerResponse((MediaServerControllerResponse<?>) message, self, sender);
        } else if (MediaServerControllerError.class.equals(klass)) {
            onMediaServerControllerError((MediaServerControllerError) message, self, sender);
        } else if (AddParticipant.class.equals(klass)) {
            onAddParticipant((AddParticipant) message, self, sender);
        } else if (Join.class.equals(klass)) {
            onJoin((Join) message, self, sender);
        } else if (JoinComplete.class.equals(klass)) {
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (StartRecordingCall.class.equals(klass)) {
            onStartRecordingCall((StartRecordingCall) message, self, sender);
        } else if (StopRecordingCall.class.equals(klass)) {
            onStopRecordingCall((StopRecordingCall) message, self, sender);
        } else if (CreateMediaGroup.class.equals(klass)) {
            onCreateMediaGroup((CreateMediaGroup) message, self, sender);
        } else if (DestroyMediaGroup.class.equals(klass)) {
            onDestroyMediaGroup((DestroyMediaGroup) message, self, sender);
        }
    }

    private void onCreateMediaGroup(CreateMediaGroup message, ActorRef self, ActorRef sender) {
        this.msController.tell(message, sender);
    }

    private void onDestroyMediaGroup(DestroyMediaGroup message, ActorRef self, ActorRef sender) {
        this.msController.tell(message, sender);
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
        }
    }

    private void onGetCallObservers(GetCallObservers message, ActorRef self, ActorRef sender) throws Exception {
        sender.tell(new CallResponse<List<ActorRef>>(this.observers), self);
    }

    private void onGetCallInfo(GetCallInfo message, ActorRef self, ActorRef sender) throws Exception {
        sender.tell(info(), self);
    }

    private void onInitializeOutbound(InitializeOutbound message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, queued);
    }

    private void onAnswer(Answer message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, creatingMediaSession);
    }

    private void onDial(Dial message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, creatingMediaSession);
    }

    private void onReject(Reject message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, busy);
    }

    private void onCancel(Cancel message, ActorRef self, ActorRef sender) throws Exception {
        if (is(creatingMediaSession) || is(dialing) || is(ringing) || is(failingNoAnswer)) {
            fsm.transition(message, canceling);
        }
    }

    private void onReceiveTimeout(ReceiveTimeout message, ActorRef self, ActorRef sender) throws Exception {
        if (is(ringing)) {
            fsm.transition(message, failingNoAnswer);
        } else {
            logger.info("Timeout received. Sender: " + sender.path().toString() + " State: " + this.fsm.state()
                    + " Direction: " + direction + " From: " + from + " To: " + to);
        }
    }

    private void onSipServletRequest(SipServletRequest message, ActorRef self, ActorRef sender) throws Exception {
        final String method = message.getMethod();
        if ("INVITE".equalsIgnoreCase(method)) {
            if (is(uninitialized)) {
                fsm.transition(message, ringing);
            }
        } else if ("CANCEL".equalsIgnoreCase(method)) {
            if (is(creatingMediaSession)) {
                fsm.transition(message, canceling);
            } else {
                fsm.transition(message, canceled);
            }
        } else if ("BYE".equalsIgnoreCase(method)) {
            fsm.transition(message, completing);
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
                    fsm.transition(message, ringing);
                }
                break;
            }
            case SipServletResponse.SC_BUSY_HERE:
            case SipServletResponse.SC_BUSY_EVERYWHERE: {
                sendCallInfoToObservers();
                if (is(dialing)) {
                    break;
                } else {
                    fsm.transition(message, failingBusy);
                }
                break;
            }
            case SipServletResponse.SC_UNAUTHORIZED:
            case SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED: {
                // Handles Auth for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                if (this.username == null || this.password == null) {
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
                    sendCallInfoToObservers();
                    fsm.transition(message, failed);
                }
            }
        }
    }

    private void onHangup(Hangup message, ActorRef self, ActorRef sender) throws Exception {
        if (is(inProgress) || is(updatingMediaSession) || is(ringing) || is(queued)) {
            fsm.transition(message, completing);
        }
    }

    private void onNotFound(org.mobicents.servlet.restcomm.telephony.NotFound message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(ringing)) {
            fsm.transition(message, notFound);
        }
    }

    private void onMediaServerControllerError(MediaServerControllerError message, ActorRef self, ActorRef sender)
            throws Exception {
        if (is(creatingMediaSession)) {
            fsm.transition(message, failed);
        } else if (is(updatingMediaSession) || is(joining)) {
            fsm.transition(message, failing);
        }
    }

    private void onMediaServerControllerResponse(MediaServerControllerResponse<?> message, ActorRef self, ActorRef sender)
            throws Exception {
        Object obj = message.get();
        Class<?> klass = obj.getClass();

        if (MediaSessionInfo.class.equals(klass)) {
            if (is(creatingMediaSession)) {
                if (isInbound()) {
                    fsm.transition(message, inProgress);
                } else {
                    fsm.transition(message, dialing);
                }
            } else if (is(updatingMediaSession)) {
                fsm.transition(message, inProgress);
            }
        } else if (MediaSessionClosed.class.equals(klass)) {
            if (is(completing)) {
                fsm.transition(message, completed);
            } else if (is(failing)) {
                fsm.transition(message, failed);
            } else if (is(failingBusy)) {
                fsm.transition(message, busy);
            } else if (is(failingNoAnswer)) {
                fsm.transition(message, noAnswer);
            }
        } else if (JoinComplete.class.equals(klass)) {
            if (is(joining)) {
                // MSController completed join operation
                // Inform the conference and tell observers call is in progress
                conference.tell(message.get(), self);
                fsm.transition(message, inProgress);
            }
        }
    }

    private void onAddParticipant(AddParticipant message, ActorRef self, ActorRef sender) throws Exception {
        this.outboundCall = message.call();
        final Join join = new Join(self, this.msController, ConnectionMode.SendRecv);
        this.outboundCall.tell(join, self);
    }

    private void onJoin(Join message, ActorRef self, ActorRef sender) throws Exception {
        this.conference = sender;
        this.conferenceController = message.mscontroller();
        this.fsm.transition(message, joining);
    }

    private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) throws Exception {
        if (sender.equals(outboundCall)) {
            // Warn controller that outbound call successfully joined
            this.msController.tell(message, self);
        }
    }

    private void onStartRecordingCall(StartRecordingCall message, ActorRef self, ActorRef sender) throws Exception {
        // Forward message for Media Session Controller to handle
        message.setCallId(this.id);
        this.msController.tell(message, sender);
    }

    private void onStopRecordingCall(StopRecordingCall message, ActorRef self, ActorRef sender) throws Exception {
        // Forward message for Media Session Controller to handle
        this.msController.tell(message, sender);
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
            recordsDao = request.getDaoManager().getCallDetailRecordsDao();
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
                    builder.setDateCreated(created);
                    builder.setAccountSid(accountId);
                    builder.setTo(to.getUser());
                    builder.setCallerName(name);
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

        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object message) throws Exception {
            final MediaServerControllerResponse<MediaSessionInfo> response = (MediaServerControllerResponse<MediaSessionInfo>) message;
            final ActorRef self = self();

            mediaSessionInfo = response.get();

            // Create a SIP invite to initiate a new session.
            final StringBuilder buffer = new StringBuilder();
            buffer.append(to.getHost());
            if (to.getPort() > -1) {
                buffer.append(":").append(to.getPort());
            }
            final SipURI uri = factory.createSipURI(null, buffer.toString());
            final SipApplicationSession application = factory.createApplicationSession();
            application.setAttribute(Call.class.getName(), self);
            invite = factory.createRequest(application, "INVITE", from, to);
            invite.pushRoute(uri);

            if (headers != null) {
                // adding custom headers for SIP Out
                // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                Set<Map.Entry<String, String>> entrySet = headers.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {
                    invite.addHeader("X-" + entry.getKey(), entry.getValue());
                }
            }
            invite.addHeader("X-RestComm-ApiVersion", apiVersion);
            invite.addHeader("X-RestComm-AccountSid", accountId.toString());
            invite.addHeader("X-RestComm-CallSid", id.toString());
            final SipSession session = invite.getSession();
            session.setHandler("CallManager");
            // Issue: https://telestax.atlassian.net/browse/RESTCOMM-608
            // If this is a call to Restcomm client or SIP URI bypass LB
            if (type.equals(CreateCall.Type.CLIENT) || type.equals(CreateCall.Type.SIP)) {
                ((SipSessionExt) session).setBypassLoadBalancer(true);
                ((SipSessionExt) session).setBypassProxy(true);
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
                // Send a ringing response.
                final SipServletResponse ringing = invite.createResponse(SipServletResponse.SC_RINGING);
                ringing.send();

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
            if (isOutbound()) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
                final SipServletRequest cancel = invite.createCancel();
                cancel.send();
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
            // Explicitly invalidate the application session.
            invite.getSession().invalidate();
            invite.getApplicationSession().invalidate();

            // Notify the observers.
            external = CallStateChanged.State.CANCELED;
            final CallStateChanged event = new CallStateChanged(external);
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

    private class Failing extends AbstractAction {
        public Failing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof ReceiveTimeout) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }
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
            logger.info("Call moves to failing state because no answer");
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
                final SipServletResponse busy = invite.createResponse(SipServletResponse.SC_BUSY_HERE);
                busy.send();
            }

            // Explicitly invalidate the application session.
            invite.getSession().invalidate();
            invite.getApplicationSession().invalidate();

            // Notify the observers.
            external = CallStateChanged.State.BUSY;
            final CallStateChanged event = new CallStateChanged(external);
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

    private final class NotFound extends AbstractAction {

        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();

            // Send SIP NOT_FOUND to remote peer
            if (org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass) && isInbound()) {
                final SipServletResponse notFound = invite.createResponse(SipServletResponse.SC_NOT_FOUND);
                notFound.send();
            }

            // Notify the observers.
            external = CallStateChanged.State.NOT_FOUND;
            final CallStateChanged event = new CallStateChanged(external);
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
            // Explicitly invalidate the application session.
            if (invite.getSession().isValid()) {
                invite.getSession().invalidate();
            }

            if (invite.getApplicationSession().isValid()) {
                invite.getApplicationSession().invalidate();
            }

            // Notify the observers.
            external = CallStateChanged.State.NO_ANSWER;
            final CallStateChanged event = new CallStateChanged(external);
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
                invite.createResponse(503, "Problem to setup services").send();
            }

            // Explicitly invalidate the application session.
            if (invite.getSession().isValid()) {
                invite.getSession().setInvalidateWhenReady(true);
            }

            if (invite.getApplicationSession().isValid()) {
                invite.getApplicationSession().setInvalidateWhenReady(true);
            }

            // Notify the observers.
            external = CallStateChanged.State.FAILED;
            final CallStateChanged event = new CallStateChanged(external);
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

    private final class CreatingMediaSession extends AbstractAction {

        public CreatingMediaSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            CreateMediaSession command = null;
            if (isOutbound()) {
                command = new CreateMediaSession("sendrecv", "", true);
            } else {
                final String externalIp = invite.getInitialRemoteAddr();
                final byte[] sdp = invite.getRawContent();
                final String offer = SdpUtils.patch(invite.getContentType(), sdp, externalIp);
                command = new CreateMediaSession("sendrecv", offer, false);
            }
            msController.tell(command, source);
        }
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
                final SipServletRequest ack = response.createAck();
                final SipServletRequest originalInvite = response.getRequest();
                final SipURI realInetUri = (SipURI) originalInvite.getRequestURI();
                final InetAddress ackRURI = InetAddress.getByName(((SipURI) ack.getRequestURI()).getHost());

                if (realInetUri != null
                        && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())) {
                    logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                            + " as a request uri of the ACK");
                    ack.setRequestURI(realInetUri);
                }
                ack.send();
                logger.info("Just sent out ACK : " + ack.toString());
            }

            final String externalIp = response.getInitialRemoteAddr();
            final byte[] sdp = response.getRawContent();
            final String answer = SdpUtils.patch(response.getContentType(), sdp, externalIp);
            final UpdateMediaSession update = new UpdateMediaSession(answer);
            msController.tell(update, source);
        }
    }

    private final class InProgress extends AbstractAction {

        public InProgress(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            SipSession.State sessionState = invite.getSession().getState();

            if (is(creatingMediaSession)
                    && !(SipSession.State.CONFIRMED.equals(sessionState) || SipSession.State.TERMINATED.equals(sessionState))) {
                MediaServerControllerResponse<MediaSessionInfo> response = (MediaServerControllerResponse<MediaSessionInfo>) message;
                mediaSessionInfo = response.get();
                final SipServletResponse okay = invite.createResponse(SipServletResponse.SC_OK);
                final byte[] sdp = mediaSessionInfo.getLocalSdp().getBytes();
                String answer = null;
                if (mediaSessionInfo.usesNat()) {
                    final String externalIp = mediaSessionInfo.getExternalAddress().getHostAddress();
                    answer = SdpUtils.patch("application/sdp", sdp, externalIp);
                } else {
                    answer = mediaSessionInfo.getLocalSdp().toString();
                }
                // Issue #215: https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
                answer = SdpUtils.endWithNewLine(answer);
                okay.setContent(answer, "application/sdp");
                okay.send();
            }

            if (is(creatingMediaSession) || is(updatingMediaSession)) {
                // Make sure the SIP session doesn't end pre-maturely.
                invite.getApplicationSession().setExpires(0);
            }

            // Notify the observers.
            external = CallStateChanged.State.IN_PROGRESS;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound() && !outgoingCallRecord.getStatus().equalsIgnoreCase("in_progress")) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                outgoingCallRecord = outgoingCallRecord.setStartTime(DateTime.now());
                outgoingCallRecord = outgoingCallRecord.setAnsweredBy(to.getUser());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
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

    private final class Completing extends AbstractAction {

        public Completing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Hangup.class.equals(klass)) {
                // Send SIP BYE to remote peer
                final SipSession session = invite.getSession();
                final SipServletRequest bye = session.createRequest("BYE");
                final SipURI realInetUri = (SipURI) session.getAttribute("realInetUri");
                final InetAddress byeRURI = InetAddress.getByName(((SipURI) bye.getRequestURI()).getHost());

                if (realInetUri != null
                        && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                            + " as a request uri of the BYE request");
                    bye.setRequestURI(realInetUri);
                }
                bye.send();
            } else if (message instanceof SipServletRequest) {
                // Send SIP OK to remote peer
                final SipServletRequest bye = (SipServletRequest) message;
                final SipServletResponse okay = bye.createResponse(SipServletResponse.SC_OK);
                okay.send();
            } else if (message instanceof SipServletResponse) {
                final SipServletResponse resp = (SipServletResponse) message;
                if (resp.equals(SipServletResponse.SC_BUSY_HERE) || resp.equals(SipServletResponse.SC_BUSY_EVERYWHERE)) {
                    // Notify the observers.
                    external = CallStateChanged.State.BUSY;
                    final CallStateChanged event = new CallStateChanged(external);
                    for (final ActorRef observer : observers) {
                        observer.tell(event, source);
                    }
                }
            }

            // Destroy current media session
            msController.tell(new CloseMediaSession(), source);
        }
    }

    private final class Completed extends AbstractAction {

        public Completed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            logger.info("Completing Call");

            // Explicitly invalidate the application session.
            invite.getSession().invalidate();
            invite.getApplicationSession().invalidate();

            // Notify the observers.
            external = CallStateChanged.State.COMPLETED;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }

            // Record call data
            if (outgoingCallRecord != null && isOutbound()) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                final DateTime now = DateTime.now();
                outgoingCallRecord = outgoingCallRecord.setEndTime(now);
                final int seconds = (int) ((now.getMillis() - outgoingCallRecord.getStartTime().getMillis()) / 1000);
                outgoingCallRecord = outgoingCallRecord.setDuration(seconds);
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
                logger.debug("Start: " + outgoingCallRecord.getStartTime());
                logger.debug("End: " + outgoingCallRecord.getEndTime());
                logger.debug("Duration: " + seconds);
                logger.debug("Just updated CDR for completed call");
            }
        }
    }

}

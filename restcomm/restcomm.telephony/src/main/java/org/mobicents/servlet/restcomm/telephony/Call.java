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

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.io.File;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
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
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.javax.servlet.sip.SipSessionExt;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.CloseLink;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.DestroyConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyLink;
import org.mobicents.servlet.restcomm.mgcp.GetMediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.UpdateConnection;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.util.IPUtils;
import org.mobicents.servlet.restcomm.util.WavUtils;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 * @author gvagenas@telestax.com (George Vagenas)
 */
public final class Call extends UntypedActor {
    // Define possible directions.
    private static final String INBOUND = "inbound";
    private static final String OUTBOUND_API = "outbound-api";
    private static final String OUTBOUND_DIAL = "outbound-dial";
    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // States for the FSM.
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
    // Intermediate states.
    private final State canceling;
    private final State acquiringMediaGatewayInfo;
    private final State acquiringMediaSession;
    private final State acquiringBridge;
    private final State acquiringRemoteConnection;
    private final State initializingRemoteConnection;
    private final State openingRemoteConnection;
    private final State updatingRemoteConnection;
    private final State dialing;
    private final State failing;
    private final State failingBusy;
    private final State failingNoAnswer;
    private final State muting;
    private final State unmuting;
    private final State acquiringInternalLink;
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    private final State closingInternalLink;
    private final State closingRemoteConnection;
    // FSM.
    private final FiniteStateMachine fsm;
    // SIP runtime stuff.
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
    private URI recordingUri;
    private Sid recordingSid;
    private DateTime recordStarted;
    private long timeout;
    private SipServletRequest invite;
    private SipServletResponse lastResponse;
    // MGCP runtime stuff.
    private final ActorRef gateway;
    private MediaGatewayInfo gatewayInfo;
    private MediaSession session;
    private ActorRef bridge;
    private ActorRef remoteConn;
    private ActorRef internalLink;
    private ActorRef internalLinkEndpoint;
    private ConnectionMode internalLinkMode;
    // Runtime stuff.
    private final Sid id;
    private CallStateChanged.State external;
    private String direction;
    private String forwardedFrom;
    private DateTime created;
    private final List<ActorRef> observers;

    // CallMediaGroup
    private ActorRef group;
    private ActorRef conference;
    private CallDetailRecord outgoingCallRecord;
    private CallDetailRecordsDao recordsDao;
    private DaoManager daoManager;
    private ActorRef initialCall;
    private static Boolean recording = false;
    private ActorRef outboundCall;
    private ActorRef outboundCallBridgeEndpoint;
    private boolean liveCallModification = false;

    // Runtime Setting
    private Configuration runtimeSettings;

    public Call(final SipFactory factory, final ActorRef gateway) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        queued = new State("queued", new Queued(source), null);
        ringing = new State("ringing", new Ringing(source), null);
        busy = new State("busy", new Busy(source), null);
        notFound = new State("not found", new NotFound(source), null);
        canceled = new State("canceled", new Canceled(source), null);
        noAnswer = new State("no answer", new NoAnswer(source), null);
        inProgress = new State("in progress", new InProgress(source), null);
        completed = new State("completed", new Completed(source), null);
        failed = new State("failed", new Failed(source), null);
        // Initialize the intermediate states for the FSM.
        canceling = new State("canceling", new Canceling(source), null);
        acquiringMediaGatewayInfo = new State("acquiring media gateway info", new AcquiringMediaGatewayInfo(source), null);
        acquiringMediaSession = new State("acquiring media session", new AcquiringMediaSession(source), null);
        acquiringBridge = new State("acquiring bridge", new AcquiringBridge(source), null);
        acquiringRemoteConnection = new State("acquiring remote connection", new AcquiringRemoteConnection(source), null);
        initializingRemoteConnection = new State("initializing remote connection", new InitializingRemoteConnection(source),
                null);
        openingRemoteConnection = new State("opening remote connection", new OpeningRemoteConnection(source), null);
        updatingRemoteConnection = new State("updating remote connection", new UpdatingRemoteConnection(source), null);
        dialing = new State("dialing", new Dialing(source), null);
        failing = new State("failing", new Failing(source), null);
        failingBusy = new State("failing busy", new FailingBusy(source), null);
        failingNoAnswer = new State("failing no answer", new FailingNoAnswer(source), null);
        acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        initializingInternalLink = new State("initializing internal link", new InitializingInternalLink(source), null);
        openingInternalLink = new State("opening internal link", new OpeningInternalLink(source), null);
        updatingInternalLink = new State("updating internal link", new UpdatingInternalLink(source), null);
        closingInternalLink = new State("closing internal link", new EnteringClosingInternalLink(source),
                new ExitingClosingInternalLink(source));
        muting = new State("muting", new Muting(source), null);
        unmuting = new State("unmuting", new Unmuting(source), null);
        closingRemoteConnection = new State("closing remote connection", new ClosingRemoteConnection(source), null);
        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, queued));
        transitions.add(new Transition(uninitialized, ringing));
        transitions.add(new Transition(uninitialized, failing));
        transitions.add(new Transition(queued, canceled));
        transitions.add(new Transition(queued, acquiringMediaGatewayInfo));
        transitions.add(new Transition(queued, closingRemoteConnection));
        transitions.add(new Transition(acquiringMediaGatewayInfo, canceled));
        transitions.add(new Transition(acquiringMediaGatewayInfo, acquiringMediaSession));
        transitions.add(new Transition(acquiringMediaSession, canceled));
        transitions.add(new Transition(acquiringMediaSession, acquiringBridge));
        transitions.add(new Transition(acquiringBridge, canceled));
        transitions.add(new Transition(acquiringBridge, acquiringRemoteConnection));
        transitions.add(new Transition(acquiringBridge, inProgress));
        transitions.add(new Transition(acquiringRemoteConnection, canceled));
        transitions.add(new Transition(acquiringRemoteConnection, initializingRemoteConnection));
        transitions.add(new Transition(initializingRemoteConnection, canceled));
        transitions.add(new Transition(initializingRemoteConnection, openingRemoteConnection));
        transitions.add(new Transition(openingRemoteConnection, canceling));
        transitions.add(new Transition(openingRemoteConnection, dialing));
        transitions.add(new Transition(openingRemoteConnection, failed));
        transitions.add(new Transition(openingRemoteConnection, inProgress));
        transitions.add(new Transition(dialing, busy));
        // transitions.add(new Transition(dialing, failingBusy));
        transitions.add(new Transition(dialing, canceling));
        transitions.add(new Transition(dialing, failingNoAnswer));
        transitions.add(new Transition(dialing, ringing));
        transitions.add(new Transition(dialing, failed));
        transitions.add(new Transition(dialing, updatingRemoteConnection));
        transitions.add(new Transition(ringing, canceled));
        transitions.add(new Transition(ringing, busy));
        transitions.add(new Transition(ringing, notFound));
        transitions.add(new Transition(ringing, canceling));
        transitions.add(new Transition(ringing, noAnswer));
        transitions.add(new Transition(ringing, failed));
        transitions.add(new Transition(ringing, updatingRemoteConnection));
        transitions.add(new Transition(ringing, acquiringMediaGatewayInfo));
        transitions.add(new Transition(ringing, failingBusy));
        transitions.add(new Transition(ringing, failingNoAnswer));
        transitions.add(new Transition(ringing, closingRemoteConnection));
        transitions.add(new Transition(failingNoAnswer, noAnswer));
        transitions.add(new Transition(failingNoAnswer, canceling));
        transitions.add(new Transition(failingBusy, busy));
        transitions.add(new Transition(canceling, canceled));
        transitions.add(new Transition(updatingRemoteConnection, inProgress));
        transitions.add(new Transition(updatingRemoteConnection, closingRemoteConnection));
        transitions.add(new Transition(inProgress, muting));
        transitions.add(new Transition(inProgress, unmuting));
        transitions.add(new Transition(inProgress, acquiringInternalLink));
        transitions.add(new Transition(inProgress, closingInternalLink));
        transitions.add(new Transition(inProgress, closingRemoteConnection));
        transitions.add(new Transition(inProgress, acquiringMediaGatewayInfo));
        transitions.add(new Transition(inProgress, failed));
        transitions.add(new Transition(inProgress, acquiringBridge));
        transitions.add(new Transition(inProgress, inProgress));
        transitions.add(new Transition(acquiringInternalLink, closingRemoteConnection));
        transitions.add(new Transition(acquiringInternalLink, initializingInternalLink));
        transitions.add(new Transition(initializingInternalLink, closingRemoteConnection));
        transitions.add(new Transition(initializingInternalLink, openingInternalLink));
        transitions.add(new Transition(openingInternalLink, closingInternalLink));
        transitions.add(new Transition(openingInternalLink, closingRemoteConnection));
        transitions.add(new Transition(openingInternalLink, updatingInternalLink));
        transitions.add(new Transition(updatingInternalLink, closingInternalLink));
        transitions.add(new Transition(updatingInternalLink, closingRemoteConnection));
        transitions.add(new Transition(updatingInternalLink, inProgress));
        transitions.add(new Transition(closingInternalLink, inProgress));
        transitions.add(new Transition(closingInternalLink, completed));
        transitions.add(new Transition(muting, inProgress));
        transitions.add(new Transition(muting, closingRemoteConnection));
        transitions.add(new Transition(unmuting, inProgress));
        transitions.add(new Transition(unmuting, closingRemoteConnection));
        transitions.add(new Transition(closingRemoteConnection, closingInternalLink));
        transitions.add(new Transition(closingRemoteConnection, completed));
        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the SIP runtime stuff.
        this.factory = factory;
        // Initialize the MGCP runtime stuff.
        this.gateway = gateway;
        // Initialize the runtime stuff.
        this.id = Sid.generate(Sid.Type.CALL);
        this.created = DateTime.now();
        this.observers = Collections.synchronizedList(new ArrayList<ActorRef>());
    }

    private ActorRef getMediaGroup(final Object message) {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MediaGroup(gateway, session, bridge);
            }
        }));
    }

    private void forwarding(final Object message) {

    }

    private CallResponse<CallInfo> info() {
        final String from = this.from.getUser();
        final String to = this.to.getUser();
        final CallInfo info = new CallInfo(id, external, type, direction, created, forwardedFrom, name, from, to, invite, lastResponse);
        return new CallResponse<CallInfo>(info);
    }

    private void invite(final Object message) throws Exception {
        final AddParticipant request = (AddParticipant) message;
        final Join join = new Join(bridge, ConnectionMode.SendRecv);
        outboundCall = request.call();
        final ActorRef self = self();
        outboundCall.tell(join, self);
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

        int remotePort = message.getRemotePort();
        int contactPort = ((SipURI) contactAddr.getURI()).getPort();
        String remoteAddress = message.getRemoteAddr();

        // Issue #332: https://telestax.atlassian.net/browse/RESTCOMM-332
        final String initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
        String initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");
        String contactAddress = ((SipURI) contactAddr.getURI()).getHost();

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
        // //Assuming that the contactPort (from the Contact header) is the port that is assigned to the sip client,
        // //If RemotePort (either from Packet or from the Via header rport) is not the same as the contactPort, then we
        // //should use the remotePort and remoteAddres for the URI to use later for client behind NAT
        // else if(remotePort != contactPort) {
        // logger.info("RemotePort: "+remotePort+" is different than the Contact Address port: "+contactPort+" so storing for later use the "
        // + remoteAddress+":"+remotePort);
        // realIP = remoteAddress+":"+remotePort;
        // uri = factory.createSipURI(null, realIP);
        // }
        return uri;
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            synchronized (observers) {
                observers.add(observer);
                observer.tell(new Observing(self), self);
            }
        }
    }

    private void startRecordingCall() throws Exception {
        logger.info("Start recording call");
        boolean playBeep = false;
        String finishOnKey = "1234567890*#";
        int maxLength = 3600;
        int timeout = 5;
        recordStarted = DateTime.now();
        Record record = null;
        record = new Record(recordingUri, timeout, maxLength, finishOnKey);
        group.tell(record, null);
        recording = true;
    }

    private void stopRecordingCall() throws UnsupportedAudioFileException, IOException {
        logger.info("Stop recording call");
        if (group != null) {
//            recording = false;
            //No need to stop the group here, it was stopped earlier by VoiceInterpreter
            group.tell(new Stop(), null);
            //VoiceInterpreter.finishDialing (if BYE sent by initial calls OR Call.ClosingRemoteConnection (if BYE sent by outbound call)
            //Will take care to create the recording object
        } else {
            logger.info("Tried to stop recording but group was null.");
        }
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();
        logger.info("********** Call's "+ self().path() +" Current State: \"" + state.toString());
        logger.info("********** Call "+ self().path() +" Processing Message: \"" + klass.getName() + " sender : " + sender.getClass());

        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (GetCallObservers.class.equals(klass)) {
            sender.tell(new CallResponse<List<ActorRef>>(observers), self);
        } else if (GetCallInfo.class.equals(klass)) {
            sender.tell(info(), self);
        } else if (GetOutboundCall.class.equals(klass)) {
            sender.tell(outboundCall, self);
        } else if (InitializeOutbound.class.equals(klass)) {
            fsm.transition(message, queued);
        } else if (ChangeCallDirection.class.equals(klass)) {
            //Needed for LiveCallModification API where an the outgoingCall needs to move to the new destination also.
            //We need to change the Call Direction and also release the internal link
            this.direction = INBOUND;
            liveCallModification = true;
            conference = null;
            outboundCall = null;
            if (bridge != null) {
                logger.info("Call :"+self().path()+" Bridge endpoint: "+bridge.path()+" isTerminated: "+bridge.isTerminated());
            } else {
                logger.info("Call :"+self().path()+" Bridge endpoint is null");
            }
            if (group != null) {
                logger.info("Call :"+self().path()+" group: "+group.path()+" isTerminated: "+group.isTerminated());
            } else {
                logger.info("Call :"+self().path()+" Group is null");
            }
            //            if (bridge != null) {
            //                gateway.tell(new DestroyEndpoint(bridge), self());
            //                context().stop(bridge);
            //                bridge = null;
            //            }
            if (group == null || group.isTerminated()) {
                group = getMediaGroup(message);
            }
//            if (internalLink != null && !internalLink.isTerminated()) {
//                gateway.tell(new DestroyLink(internalLink), null);
//                context().stop(internalLink);
//                context().stop(internalLinkEndpoint);
//                internalLink = null;
//            }
//            fsm.transition(message, acquiringBridge);
        } else if (Answer.class.equals(klass) || Dial.class.equals(klass)) {
            if (!inProgress.equals(state) ) {
                fsm.transition(message, acquiringMediaGatewayInfo);
            } else {
                fsm.transition(message, inProgress);
            }

        } else if (Reject.class.equals(klass)) {
            fsm.transition(message, busy);
        } else if (JoinComplete.class.equals(klass)) {
            if (sender.equals(outboundCall)) {
                JoinComplete joinComplete = (JoinComplete) message;
                outboundCallBridgeEndpoint = joinComplete.endpoint();
                final Join join = new Join(outboundCallBridgeEndpoint, ConnectionMode.SendRecv);
                group.tell(join, null);
            }
        } else if (StartRecordingCall.class.equals(klass)) {
            StartRecordingCall startRecordingCall = (StartRecordingCall) message;
            if (runtimeSettings == null)
                this.runtimeSettings = startRecordingCall.getRuntimeSetting();
            if (daoManager == null)
                daoManager = startRecordingCall.getDaoManager();
            if (accountId == null)
                accountId = startRecordingCall.getAccountId();
            recordingSid = startRecordingCall.getRecordingSid();
            recordingUri = startRecordingCall.getRecordingUri();
            recording = true;
            startRecordingCall();
        } else if (StopRecordingCall.class.equals(klass)) {
            if (recording) {
                StopRecordingCall stopRecoringdCall = (StopRecordingCall) message;
                if (runtimeSettings == null)
                    this.runtimeSettings = stopRecoringdCall.getRuntimeSetting();
                if (daoManager == null)
                    daoManager = stopRecoringdCall.getDaoManager();
                if (accountId == null)
                    accountId = stopRecoringdCall.getAccountId();
                stopRecordingCall();
            }
        }
        else if (RecordingStarted.class.equals(klass)) {
          //VoiceInterpreter executed the Record verb and notified the call actor that we are in recording now
          //so Call should wait for NTFY for Recording before complete the call
            recording = true;
        } else if (MediaGatewayResponse.class.equals(klass)) {
            if (acquiringMediaGatewayInfo.equals(state)) {
                fsm.transition(message, acquiringMediaSession);
            } else if (acquiringMediaSession.equals(state)) {
                fsm.transition(message, acquiringBridge);
            } else if (acquiringBridge.equals(state)) {
                if (!liveCallModification) {
                    fsm.transition(message, acquiringRemoteConnection);
                } else {
                    fsm.transition(message, inProgress);
                }
            } else if (acquiringRemoteConnection.equals(state)) {
                fsm.transition(message, initializingRemoteConnection);
            } else if (acquiringInternalLink.equals(state)) {
                fsm.transition(message, initializingInternalLink);
            }
        } else if (ConnectionStateChanged.class.equals(klass)) {
            final ConnectionStateChanged event = (ConnectionStateChanged) message;

            if (ConnectionStateChanged.State.CLOSED == event.state()) {
                if (initializingRemoteConnection.equals(state)) {
                    fsm.transition(message, openingRemoteConnection);
                } else if (openingRemoteConnection.equals(state)) {
                    fsm.transition(message, failed);
                } else if (failing.equals(state)) {
                    fsm.transition(message, failed);
                } else if (failingBusy.equals(state)) {
                    fsm.transition(message, busy);
                } else if (failingNoAnswer.equals(state)) {
                    fsm.transition(message, noAnswer);
                } else if (muting.equals(state) || unmuting.equals(state)) {
                    fsm.transition(message, closingRemoteConnection);
                } else if (closingRemoteConnection.equals(state)) {
                    context().stop(remoteConn);
                    remoteConn = null;
                    if (internalLink != null) {
                        fsm.transition(message, closingInternalLink);
                    } else {
                        fsm.transition(message, completed);
                    }
                }
            } else if (ConnectionStateChanged.State.HALF_OPEN == event.state()) {
                fsm.transition(message, dialing);
            } else if (ConnectionStateChanged.State.OPEN == event.state()) {
                fsm.transition(message, inProgress);
            }
        } else if (Cancel.class.equals(klass)) {
            if (openingRemoteConnection.equals(state) || dialing.equals(state) || ringing.equals(state)
                    || failingNoAnswer.equals(state)) {
                fsm.transition(message, canceling);
            }
        } else if (LinkStateChanged.class.equals(klass)) {
            final LinkStateChanged event = (LinkStateChanged) message;
            if (LinkStateChanged.State.CLOSED == event.state()) {
                if (initializingInternalLink.equals(state)) {
                    fsm.transition(message, openingInternalLink);
                } else if (openingInternalLink.equals(state)) {
                    fsm.transition(message, closingRemoteConnection);
                } else if (closingInternalLink.equals(state)) {
                    if (remoteConn != null) {
                        fsm.transition(message, inProgress);
                    } else {
                        fsm.transition(message, completed);
                    }
                }
            } else if (LinkStateChanged.State.OPEN == event.state()) {
                if (openingInternalLink.equals(state)) {
                    fsm.transition(message, updatingInternalLink);
                } else if (updatingInternalLink.equals(state)) {
                    fsm.transition(message, inProgress);
                }
            }
        } else if (message instanceof ReceiveTimeout) {
            if (ringing.equals(state)) {
                fsm.transition(message, failingNoAnswer);
            } else {
                logger.info("Call "+ self().path() +" Timeout received. Sender: " + sender.path().toString() + " State: " + state + " Direction: "
                        + direction + " From: " + from + " To: " + to);
            }
        } else if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("INVITE".equalsIgnoreCase(method)) {
                if (uninitialized.equals(state)) {
                    fsm.transition(message, ringing);
                }
            } else if ("CANCEL".equalsIgnoreCase(method)) {
                if (openingRemoteConnection.equals(state)) {
                    fsm.transition(message, canceling);
                } else {
                    fsm.transition(message, canceled);
                }
            } else if ("BYE".equalsIgnoreCase(method)) {
                fsm.transition(message, closingRemoteConnection);
            } else if ("INFO".equalsIgnoreCase(method)) {
                processInfo(request);
            }
        } else if (message instanceof SipServletResponse) {
            final SipServletResponse response = (SipServletResponse) message;
            lastResponse = response;
            final int code = response.getStatus();
            switch (code) {
                case SipServletResponse.SC_CALL_BEING_FORWARDED: {
                    forwarding(message);
                    break;
                }
                case SipServletResponse.SC_RINGING:
                case SipServletResponse.SC_SESSION_PROGRESS: {
                    if (!state.equals(ringing))
                        fsm.transition(message, ringing);
                    break;
                }
                case SipServletResponse.SC_BUSY_HERE:
                case SipServletResponse.SC_BUSY_EVERYWHERE: {
                    sendCallInfoToObservers();
                    if (dialing.equals(state)) {
                        break;
                    } else {
                        fsm.transition(message, failingBusy);
                    }
                    break;
                }
                case SipServletResponse.SC_UNAUTHORIZED:
                case SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED: {
                    // Handles Auth for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                    if (username == null || password == null) {
                        sendCallInfoToObservers();
                        fsm.transition(message, failed);
                    } else {
                        AuthInfo authInfo = factory.createAuthInfo();
                        String authHeader = response.getHeader("Proxy-Authenticate");
                        if (authHeader == null) {
                            authHeader = response.getHeader("WWW-Authenticate");
                        }
                        String tempRealm = authHeader.substring(authHeader.indexOf("realm=\"") + "realm=\"".length());
                        String realm = tempRealm.substring(0, tempRealm.indexOf("\""));
                        authInfo.addAuthInfo(response.getStatus(), realm, username, password);
                        SipServletRequest challengeRequest = response.getSession().createRequest(
                                response.getRequest().getMethod());
                        challengeRequest.addAuthHeader(response, authInfo);
                        challengeRequest.setContent(invite.getContent(), invite.getContentType());
                        invite = challengeRequest;
                        // https://github.com/Mobicents/RestComm/issues/147 Make sure we send the SDP again
                        invite.setContent(response.getRequest().getContent(), "application/sdp");
                        challengeRequest.send();
                    }
                    break;
                }
                // // https://github.com/Mobicents/RestComm/issues/148 Session in Progress Response should trigger MMS to start
                // the Media Session
                // case SipServletResponse.SC_SESSION_PROGRESS:
                case SipServletResponse.SC_OK: {
                    if (dialing.equals(state) || (ringing.equals(state) && !direction.equals("inbound"))) {
                        fsm.transition(message, updatingRemoteConnection);
                    } else if (inProgress.equals(state) && direction.equalsIgnoreCase("inbound")) {
                        //This is a 200 OK for ReInvite we sent before at InProgress
                        SipServletResponse ok = (SipServletResponse) message;
                        ok.createAck().send();
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
        } else if (inProgress.equals(state)) {
            if (CreateMediaGroup.class.equals(klass)) {
//                logger.info("Before group set to null, bridge: "+bridge.path()+" isTerminated: "+bridge.isTerminated());
//                if (group != null) {
//                    logger.info("group was not null, will set it to null and get new one for call: "+self().path());
//                    context.stop(group);
//                }
//                logger.info("After group set to null, bridge: "+bridge.path()+" isTerminated: "+bridge.isTerminated());
                if (group == null || group.isTerminated()) {
                    logger.info("group is null or terminated, will get new one for call: "+self().path());
                    group = getMediaGroup(message);
                }
                logger.info("1 MediaGroup for call: "+self().path()+ " will be sent to sender: "+sender.path());
                sender.tell(new CallResponse<ActorRef>(group), self);
            } else if (DestroyMediaGroup.class.equals(klass)) {
                final DestroyMediaGroup request = (DestroyMediaGroup) message;
                // context.stop(request.group());
                if (group != null && !group.isTerminated()) {
                    context.stop(group);
                    group = null;
                }
            } else if (AddParticipant.class.equals(klass)) {
                invite(message);
            } else if (RemoveParticipant.class.equals(klass)) {
                remove(message);
            } else if (Join.class.equals(klass)) {
                conference = sender;
                fsm.transition(message, acquiringInternalLink);
            } else if (Leave.class.equals(klass)) {
                fsm.transition(message, closingInternalLink);
            } else if (Mute.class.equals(klass)) {
                fsm.transition(message, muting);
            } else if (Unmute.class.equals(klass)) {
                fsm.transition(message, unmuting);
            } else if (Hangup.class.equals(klass)) {
                fsm.transition(message, closingRemoteConnection);
            }
        } else if (Hangup.class.equals(klass)) {
            if (queued.equals(state) || ringing.equals(state)) {
                fsm.transition(message, closingRemoteConnection);
            }
        } else if (ringing.equals(state)) {
            if (org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass)) {
                fsm.transition(message, notFound);
            }
        } else if (CreateMediaGroup.class.equals(klass)) {
            if (group == null || group.isTerminated()) {
                logger.info("group is null or terminated, will get new one for call: "+self().path());
                group = getMediaGroup(message);
            }
            logger.info("2 MediaGroup for call: "+self().path()+ " will be sent to sender: "+sender.path());
            sender.tell(new CallResponse<ActorRef>(group), self);
//            if (group != null) {
//                context.stop(group);
//            }
//            //LCM Hack
//            //            if (bridge == null) {
//            //                final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
//            //                Future<Object> future = (Future<Object>) akka.pattern.Patterns.ask(gateway, new CreateBridgeEndpoint(session), expires);
//            //                MediaGatewayResponse<ActorRef> futureResponse = (MediaGatewayResponse<ActorRef>) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
//            //                bridge = futureResponse.get();
//            //                if (!bridge.isTerminated() && bridge != null) {
//            //                    logger.info("Bridge for call: "+self().path()+" acquired and is not terminated. Will proceed to get MediaGroup");
//            //                }
//            //            }
//            group = getMediaGroup(message);
//            sender.tell(new CallResponse<ActorRef>(group), self);
//            logger.info("2 MediaGroup for call: "+self().path()+ " created and sent to sender: "+sender.path());
        }
    }

    private void processInfo(final SipServletRequest request) throws IOException {
        final SipServletResponse okay = request.createResponse(SipServletResponse.SC_OK);
        okay.send();
        String digits = null;
        if (request.getContentType().equalsIgnoreCase("application/dtmf-relay")){
            final String content = new String(request.getRawContent());
            digits = content.split("\n")[0].replaceFirst("Signal=","").trim();
        } else {
        digits = new String(request.getRawContent());
        }
        if (digits != null) {
            MediaGroupResponse<String> infoResponse = new MediaGroupResponse<String>(digits);
            for (final ActorRef observer : observers) {
                observer.tell(infoResponse, self());
            }
            group.tell(new Stop(), self());
        }
    }

    @SuppressWarnings("unchecked")
    private String patch(final String contentType, final byte[] data, final String externalIp) throws UnknownHostException,
    SdpException {
        final String text = new String(data);
        String patchedSdp = null;
        if (contentType.equalsIgnoreCase("application/sdp")) {
            final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(text);
            // Handle the connection at the session level.
            fix(sdp.getConnection(), externalIp);
            // https://github.com/Mobicents/RestComm/issues/149
            fix(sdp.getOrigin(), externalIp);
            // Handle the connections at the media description level.
            final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
            for (final MediaDescription description : descriptions) {
                fix(description.getConnection(), externalIp);
            }
            patchedSdp = sdp.toString();
        } else {
            String boundary = contentType.split(";")[1].split("=")[1];
            String[] parts = text.split(boundary);
            String sdpText = null;
            for (String part : parts) {
                if (part.contains("application/sdp")) {
                    sdpText = part.replaceAll("Content.*", "").replaceAll("--", "").trim();
                }
            }
            final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(sdpText);
            fix(sdp.getConnection(), externalIp);
            // https://github.com/Mobicents/RestComm/issues/149
            fix(sdp.getOrigin(), externalIp);
            // Handle the connections at the media description level.
            final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
            for (final MediaDescription description : descriptions) {
                fix(description.getConnection(), externalIp);
            }
            patchedSdp = sdp.toString();
        }
        return patchedSdp;
    }

    private void fix(final Origin origin, final String externalIp) throws UnknownHostException, SdpException {
        if (origin != null) {
            if (Connection.IN.equals(origin.getNetworkType())) {
                if (Connection.IP4.equals(origin.getAddressType())) {
                    final InetAddress address = InetAddress.getByName(origin.getAddress());
                    final String ip = address.getHostAddress();
                    if (!IPUtils.isRoutableAddress(ip)) {
                        origin.setAddress(externalIp);
                    }
                }
            }
        }
    }

    private void fix(final Connection connection, final String externalIp) throws UnknownHostException, SdpException {
        if (connection != null) {
            if (Connection.IN.equals(connection.getNetworkType())) {
                if (Connection.IP4.equals(connection.getAddressType())) {
                    final InetAddress address = InetAddress.getByName(connection.getAddress());
                    final String ip = address.getHostAddress();
                    if (!IPUtils.isRoutableAddress(ip)) {
                        connection.setAddress(externalIp);
                    }
                }
            }
        }
    }

    private void remove(final Object message) {
        final RemoveParticipant request = (RemoveParticipant) message;
        final ActorRef call = request.call();
        final ActorRef self = self();
        final Leave leave = new Leave();
        call.tell(leave, self);
    }

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
            logger.info("Call: "+self().path()+" removed observer: "+observer.path());
        } else {
            observers.clear();
            logger.info("Call: "+self().path()+" removed all observers, List size: "+observers.size());
        }
    }

    // Allow updating of the callInfo at the VoiceInterpreter so that we can do Dial SIP Screening
    // (https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out) accurately from latest response
    // received
    private void sendCallInfoToObservers() {
        // logger.info("Send Call Info type " + type + " lastResponse " + lastResponse);
        for (final ActorRef observer : observers) {
            // logger.info("Send Call Info to " + observer + " from " + from + " type " + type + " lastResponse " +
            // lastResponse);
            observer.tell(info(), self());
        }
    }

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
            if (request.isFromApi()) {
                direction = OUTBOUND_API;
            } else {
                direction = OUTBOUND_DIAL;
            }
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

    private final class AcquiringMediaGatewayInfo extends AbstractAction {
        public AcquiringMediaGatewayInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final ActorRef self = self();
            gateway.tell(new GetMediaGatewayInfo(), self);
        }
    }

    private final class AcquiringMediaSession extends AbstractAction {
        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<MediaGatewayInfo> response = (MediaGatewayResponse<MediaGatewayInfo>) message;
            gatewayInfo = response.get();
            gateway.tell(new CreateMediaSession(), source);
        }
    }

    public final class AcquiringBridge extends AbstractAction {
        public AcquiringBridge(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            if (session == null) {
                final MediaGatewayResponse<MediaSession> response = (MediaGatewayResponse<MediaSession>) message;
                session = response.get();
            }
            gateway.tell(new CreateBridgeEndpoint(session), source);
        }
    }

    private final class AcquiringRemoteConnection extends AbstractAction {
        public AcquiringRemoteConnection(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            bridge = response.get();
            gateway.tell(new CreateConnection(session), source);
        }
    }

    private final class InitializingRemoteConnection extends AbstractAction {
        public InitializingRemoteConnection(ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            remoteConn = response.get();
            remoteConn.tell(new Observe(source), source);
            remoteConn.tell(new InitializeConnection(bridge), source);
        }
    }

    private final class OpeningRemoteConnection extends AbstractAction {
        public OpeningRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            OpenConnection open = null;
            if (OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction)) {
                open = new OpenConnection(ConnectionMode.SendRecv);
            } else {
                if (!liveCallModification) {
                    final String externalIp = invite.getInitialRemoteAddr();
                    final byte[] sdp = invite.getRawContent();
                    final String offer = patch(invite.getContentType(), sdp, externalIp);
                    final ConnectionDescriptor descriptor = new ConnectionDescriptor(offer);
                    open = new OpenConnection(descriptor, ConnectionMode.SendRecv);
                } else {
                    if (lastResponse != null && lastResponse.getStatus()==200) {
                        final String externalIp = lastResponse.getInitialRemoteAddr();
                        final byte[] sdp = lastResponse.getRawContent();
                        final String offer = patch(lastResponse.getContentType(), sdp, externalIp);
                        final ConnectionDescriptor descriptor = new ConnectionDescriptor(offer);
                        open = new OpenConnection(descriptor, ConnectionMode.SendRecv);
                    }
                }
            }
            remoteConn.tell(open, source);
        }
    }

    private final class Dialing extends AbstractAction {
        public Dialing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final ConnectionStateChanged response = (ConnectionStateChanged) message;
            final ActorRef self = self();
            // Create a SIP invite to initiate a new session.
            final StringBuilder buffer = new StringBuilder();
            buffer.append(to.getHost());
            if (to.getPort() > -1) {
                buffer.append(":").append(to.getPort());
            }
            final SipURI uri = factory.createSipURI(null, buffer.toString());
            final SipApplicationSession application = factory.createApplicationSession();
            application.setAttribute(Call.class.getName(), self);
            if (name != null && !name.isEmpty()) {
                //Create the from address using the inital user displayed name
                //Example: From: "Alice" <sip:userpart@host:port>
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
            if (gatewayInfo.useNat()) {
                final String externalIp = gatewayInfo.externalIP().getHostAddress();
                final byte[] sdp = response.descriptor().toString().getBytes();
                offer = patch("application/sdp", sdp, externalIp);
            } else {
                offer = response.descriptor().toString();
            }
            offer = patchSdpDescription(offer);
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

                if (initialInetUri != null)
                    invite.getSession().setAttribute("realInetUri", initialInetUri);

            } else if (message instanceof SipServletResponse) {
                // Timeout still valid in case we receive a 180, we don't know if the
                // call will be eventually answered.
                // Issue 585: https://telestax.atlassian.net/browse/RESTCOMM-585

                // final UntypedActorContext context = getContext();
                // context.setReceiveTimeout(Duration.Undefined());
            }
            // Start recording if RecordingType.RECORD_FROM_RINGING
            // if (recordingType != null && recordingType.equals(CreateCall.RecordingType.RECORD_FROM_RINGING)) {
            // if ((OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction)) && initialCall != null) {
            // logger.info("Starting recording call with recording type: " + recordingType.toString());
            // logger.info("Telling initial call to start recording");
            // initialCall.tell(new StartRecordingCall(accountId, recordingType, runtimeSettings, daoManager), null);
            // } else {
            // logger.info("Starting recording call with recording type: " + recordingType.toString());
            // startRecordingCall();
            // }
            // }
            // Notify the observers.
            external = CallStateChanged.State.RINGING;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
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
            final State state = fsm.state();
            if (OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction)) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
                final SipServletRequest cancel = invite.createCancel();
                cancel.send();
            }
            remoteConn.tell(new CloseConnection(), source);
        }
    }

    private final class Canceled extends AbstractAction {
        public Canceled(ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            //            if (remoteConn != null) {
            //                gateway.tell(new DestroyConnection(remoteConn), source);
            //                remoteConn = null;
            //            }
            // Explicitly invalidate the application session.
            //            if (invite.getSession().isValid())
            //                invite.getSession().invalidate();
            //            if (invite.getApplicationSession().isValid())
            //                invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.CANCELED;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
            fsm.transition(message, completed);
        }
    }

    private class Failing extends ClosingRemoteConnection {
        public Failing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof ReceiveTimeout) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }
            super.execute(message);
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
            final State state = fsm.state();
            if (Reject.class.equals(klass) && ringing.equals(state) && INBOUND.equals(direction)) {
                final SipServletResponse busy = invite.createResponse(SipServletResponse.SC_BUSY_HERE);
                busy.send();
            }
            if (remoteConn != null) {
                gateway.tell(new DestroyConnection(remoteConn), source);
                context().stop(remoteConn);
                remoteConn = null;
            }
            // Explicitly invalidate the application session.
            //            if (invite.getSession().isValid())
            //                invite.getSession().invalidate();
            //            if (invite.getApplicationSession().isValid())
            //                invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.BUSY;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
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
            if (org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass) && INBOUND.equals(direction)) {
                final SipServletResponse notFound = invite.createResponse(SipServletResponse.SC_NOT_FOUND);
                notFound.send();
            }
            // Notify the observers.
            external = CallStateChanged.State.NOT_FOUND;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
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
            //            // Explicitly invalidate the application session.
            //            if (invite.getSession().isValid())
            //                invite.getSession().invalidate();
            //            if (invite.getApplicationSession().isValid())
            //                invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.NO_ANSWER;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
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
            if (remoteConn != null) {
                gateway.tell(new DestroyConnection(remoteConn), source);
                context().stop(remoteConn);
                remoteConn = null;
            }
            if (direction.equalsIgnoreCase(INBOUND))
                invite.createResponse(503, "Problem to setup services").send();
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
            if (outgoingCallRecord != null && direction.contains("outbound")) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    private final class UpdatingRemoteConnection extends AbstractAction {
        public UpdatingRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final State state = fsm.state();
            if (dialing.equals(state) || ringing.equals(state)) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }
            final SipServletResponse response = (SipServletResponse) message;
            // Issue 99:
            // http://www.google.com/url?q=https://bitbucket.org/telestax/telscale-restcomm/issue/99/dial-uri-fails&usd=2&usg=ALhdy29vtLfDNXNpjTxYYp08YRatKfV9Aw
            if (response.getStatus() == SipServletResponse.SC_OK
                    && (OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction))) {
                SipServletRequest ack = response.createAck();
                SipSession session = response.getSession();

                SipServletRequest originalInvite = response.getRequest();
                SipURI realInetUri = (SipURI) originalInvite.getRequestURI();
                InetAddress ackRURI = InetAddress.getByName(((SipURI) ack.getRequestURI()).getHost());

                if (realInetUri != null
                        && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())) {
                    logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                            + " as a request uri of the ACK");
                    ack.setRequestURI(realInetUri);
                }

                ack.send();
                logger.info("Just sent out ACK : " + ack.toString());

                // // Check if we have to record the call
                // if (recordingType.equals(CreateCall.RecordingType.RECORD_FROM_ANSWER)) {
                // if ((OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction)) && initialCall != null) {
                // logger.info("Starting recording call with recording type: " + recordingType.toString());
                // logger.info("Telling initial call to start recording");
                // initialCall.tell(new StartRecordingCall(accountId, recordingType, runtimeSettings, daoManager), null);
                // } else {
                // logger.info("Starting recording call with recording type: " + recordingType.toString());
                // startRecordingCall();
                // }
                // }
            }

            final String externalIp = response.getInitialRemoteAddr();
            final byte[] sdp = response.getRawContent();
            final String answer = patch(response.getContentType(), sdp, externalIp);
            final ConnectionDescriptor descriptor = new ConnectionDescriptor(answer);
            final UpdateConnection update = new UpdateConnection(descriptor);
            remoteConn.tell(update, source);
        }
    }

    private final class InProgress extends AbstractAction {
        public InProgress(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final State state = fsm.state();
            if (updatingInternalLink.equals(state) && conference != null) { // && direction != "outbound-dial") {
                // If this is the outbound leg for an outbound call, conference is the initial call
                // Send the JoinComplete with the Bridge endpoint, so if we need to record, the initial call
                // Will ask the Ivr Endpoint to get connect to that Bridge endpoint also
                conference.tell(new JoinComplete(bridge), source);
            }
            if (openingRemoteConnection.equals(state)
                    && !(invite.getSession().getState().equals(SipSession.State.CONFIRMED) || invite.getSession().getState()
                            .equals(SipSession.State.TERMINATED))) {
                final ConnectionStateChanged response = (ConnectionStateChanged) message;
                final SipServletResponse okay = invite.createResponse(SipServletResponse.SC_OK);
                final byte[] sdp = response.descriptor().toString().getBytes();
                String answer = null;
                if (gatewayInfo.useNat()) {
                    final String externalIp = gatewayInfo.externalIP().getHostAddress();
                    answer = patch("application/sdp", sdp, externalIp);
                } else {
                    answer = response.descriptor().toString();
                }

                // Issue #215: https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
                answer = patchSdpDescription(answer);

                okay.setContent(answer, "application/sdp");
                okay.send();
            } else if (openingRemoteConnection.equals(state)
                    && invite.getSession().getState().equals(SipSession.State.CONFIRMED)) {
                // We have an ongoing call and Restcomm executes new RCML app on that
                // If the sipSession state is Confirmed, then update SDP with the new SDP from MMS
                SipServletRequest reInvite = invite.getSession().createRequest("INVITE");
                final ConnectionStateChanged response = (ConnectionStateChanged) message;
                final byte[] sdp = response.descriptor().toString().getBytes();
                String answer = null;
                if (gatewayInfo.useNat()) {
                    final String externalIp = gatewayInfo.externalIP().getHostAddress();
                    answer = patch("application/sdp", sdp, externalIp);
                } else {
                    answer = response.descriptor().toString();
                }

                // Issue #215: https://bitbucket.org/telestax/telscale-restcomm/issue/215/restcomm-adds-extra-newline-to-sdp
                answer = patchSdpDescription(answer);

                reInvite.setContent(answer, "application/sdp");
                reInvite.send();
            }
            if (openingRemoteConnection.equals(state) || updatingRemoteConnection.equals(state)) {
                // Make sure the SIP session doesn't end pre-maturely.
                invite.getApplicationSession().setExpires(0);
            }
            // Notify the observers.
            external = CallStateChanged.State.IN_PROGRESS;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")
                    && !outgoingCallRecord.getStatus().equalsIgnoreCase("in_progress")) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                final DateTime now = DateTime.now();
                outgoingCallRecord = outgoingCallRecord.setStartTime(now);
                outgoingCallRecord = outgoingCallRecord.setAnsweredBy(to.getUser());
                recordsDao.updateCallDetailRecord(outgoingCallRecord);
            }
        }
    }

    /**
     * Patches an SDP description by trimming and making sure it ends with a new line.
     *
     * @param sdpDescription The SDP description to be patched.
     * @return The patched SDP description
     * @author hrosa
     */
    private String patchSdpDescription(String sdpDescription) {
        if (sdpDescription == null || sdpDescription.isEmpty()) {
            throw new IllegalArgumentException("The SDP description cannot be null or empty");
        }
        return sdpDescription.trim().concat("\n");
    }

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
            if (self().path().toString().equalsIgnoreCase("akka://RestComm/user/$j")){
                System.out.println("Initializing Internal Link for the Outbound call");
            }
            if (bridge != null) {
                logger.info("##################### $$ Bridge for Call "+self().path()+" is terminated: "+bridge.isTerminated());
                if (bridge.isTerminated()) {
                    //                    fsm.transition(message, acquiringMediaGatewayInfo);
                    //                    return;
                    logger.info("##################### $$ Call :"+self().path()+ " bridge is terminated.");
                    //                    final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
                    //                    Future<Object> future = (Future<Object>) akka.pattern.Patterns.ask(gateway, new CreateBridgeEndpoint(session), expires);
                    //                    MediaGatewayResponse<ActorRef> futureResponse = (MediaGatewayResponse<ActorRef>) Await.result(future, Duration.create(10, TimeUnit.SECONDS));
                    //                    bridge = futureResponse.get();
                    //                    if (!bridge.isTerminated() && bridge != null) {
                    //                        logger.info("Bridge for call: "+self().path()+" acquired and is not terminated");
                    //                    } else {
                    //                        logger.info("Bridge endpoint for call: "+self().path()+" is still terminated or null");
                    //                    }
                }
            }
            //            if (bridge == null || bridge.isTerminated()) {
            //                System.out.println("##################### $$ Bridge for Call "+self().path()+" is null or terminated: "+bridge.isTerminated());
            //            }
            internalLink = response.get();
            internalLink.tell(new Observe(source), source);
            internalLink.tell(new InitializeLink(bridge, internalLinkEndpoint), source);
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

    private final class Muting extends AbstractAction {
        public Muting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateConnection update = new UpdateConnection(ConnectionMode.SendOnly);
            remoteConn.tell(update, source);

        }
    }

    private final class Unmuting extends AbstractAction {
        public Unmuting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final UpdateConnection update = new UpdateConnection(ConnectionMode.SendRecv);
            remoteConn.tell(update, source);
        }
    }

    private final class EnteringClosingInternalLink extends AbstractAction {
        public EnteringClosingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            internalLink.tell(new CloseLink(), source);
        }
    }

    private final class ExitingClosingInternalLink extends AbstractAction {
        public ExitingClosingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            gateway.tell(new DestroyLink(internalLink), source);
            internalLink = null;
            internalLinkEndpoint = null;
            internalLinkMode = null;
        }
    }

    private class ClosingRemoteConnection extends AbstractAction {
        public ClosingRemoteConnection(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Hangup.class.equals(klass)) {
                final SipSession session = invite.getSession();
                final SipServletRequest bye = session.createRequest("BYE");

                SipURI realInetUri = (SipURI) session.getAttribute("realInetUri");
                InetAddress byeRURI = InetAddress.getByName(((SipURI) bye.getRequestURI()).getHost());

                //                INVITE sip:+12055305520@107.21.247.251 SIP/2.0
                //                Record-Route: <sip:10.154.28.245:5065;transport=udp;lr;node_host=10.13.169.214;node_port=5080;version=0>
                //                Record-Route: <sip:10.154.28.245:5060;transport=udp;lr;node_host=10.13.169.214;node_port=5080;version=0>
                //                Record-Route: <sip:67.231.8.195;lr=on;ftag=gK0043eb81>
                //                Record-Route: <sip:67.231.4.204;r2=on;lr=on;ftag=gK0043eb81>
                //                Record-Route: <sip:192.168.6.219;r2=on;lr=on;ftag=gK0043eb81>
                //                Accept: application/sdp
                //                Allow: INVITE,ACK,CANCEL,BYE
                //                Via: SIP/2.0/UDP 10.154.28.245:5065;branch=z9hG4bK1cdb.193075b2.058724zsd_0
                //                Via: SIP/2.0/UDP 10.154.28.245:5060;branch=z9hG4bK1cdb.193075b2.058724_0
                //                Via: SIP/2.0/UDP 67.231.8.195;branch=z9hG4bK1cdb.193075b2.0
                //                Via: SIP/2.0/UDP 67.231.4.204;branch=z9hG4bK1cdb.f9127375.0
                //                Via: SIP/2.0/UDP 192.168.16.114:5060;branch=z9hG4bK00B6ff7ff87ed50497f
                //                From: <sip:+1302109762259@192.168.16.114>;tag=gK0043eb81
                //                To: <sip:12055305520@192.168.6.219>
                //                Call-ID: 587241765_133360558@192.168.16.114
                //                CSeq: 393447729 INVITE
                //                Max-Forwards: 67
                //                Contact: <sip:+1302109762259@192.168.16.114:5060>
                //                Diversion: <sip:+112055305520@192.168.16.114:5060>;privacy=off;screen=no; reason=unknown; counter=1
                //                Supported: replaces
                //                Content-Disposition: session;handling=required
                //                Content-Type: application/sdp
                //                Remote-Party-ID: <sip:+1302109762259@192.168.16.114:5060>;privacy=off;screen=no
                //                X-Sip-Balancer-InitialRemoteAddr: 67.231.8.195
                //                X-Sip-Balancer-InitialRemotePort: 5060
                //                Route: <sip:10.13.169.214:5080;transport=udp;lr>
                //                Content-Length: 340

                invite.getHeaders(RecordRouteHeader.NAME);

                ListIterator<String> recordRouteList = invite.getHeaders(RecordRouteHeader.NAME);

                if (invite.getHeader("X-Sip-Balancer") != null) {
                    logger.info("We are behind LoadBalancer and will remove the first two RecordRoutes since they are the LB node");
                    recordRouteList.next();
                    recordRouteList.remove();
                    recordRouteList.next();
                    recordRouteList.remove();
                }

                if (recordRouteList.hasNext()) {
                    logger.info("Record Route is set, wont change the Request URI");
                } else if (realInetUri != null && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                            + " as a request uri of the BYE request");
                    bye.setRequestURI(realInetUri);
                }

                bye.send();

                if (recording) {
                    recording = false;
                    logger.info("Call - Will stop recording now");
                    if (recordingUri != null) {
                        Double duration = WavUtils.getAudioDuration(recordingUri);
                        if (duration.equals(0.0)) {
                            logger.info("Call wraping up recording. File doesn't exist since duration is 0");
                            final DateTime end = DateTime.now();
                            duration = new Double((end.getMillis() - recordStarted.getMillis()) / 1000);
                        } else {
                            logger.info("Call wraping up recording. File already exists, length: "+ (new File(recordingUri).length()));
                        }
                        final Recording.Builder builder = Recording.builder();
                        builder.setSid(recordingSid);
                        builder.setAccountSid(accountId);
                        builder.setCallSid(id);
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
            } else if (message instanceof SipServletRequest) {
                final SipServletRequest bye = (SipServletRequest) message;
                final SipServletResponse okay = bye.createResponse(SipServletResponse.SC_OK);
                okay.send();
                if (recording) {
                    if (!direction.contains("outbound")) {
                        //Initial Call sent BYE
                        recording = false;
                        logger.info("Call Direction: "+direction);
                        logger.info("Initial Call - Will stop recording now");
                        stopRecordingCall();
                        //VoiceInterpreter will take care to prepare the Recording object
                    } else if (conference != null) {
                        //Outbound call sent BYE. !Important conference is the initial call here.
                        conference.tell(new StopRecordingCall(accountId, runtimeSettings, daoManager), null);
                    }
                }
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
            if (remoteConn != null) {
                remoteConn.tell(new CloseConnection(), source);
            }
        }
    }

    private final class Completed extends AbstractAction {
        public Completed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            logger.info("Completing Call");
            if (group != null) {
                group.tell(new StopMediaGroup(), null);
                context().stop(group);
            }
            if (remoteConn != null) {
                gateway.tell(new DestroyConnection(remoteConn), source);
                context().stop(remoteConn);
                remoteConn = null;
            }
            if (internalLink != null) {
                gateway.tell(new DestroyLink(internalLink), source);
                context().stop(internalLink);
                context().stop(internalLinkEndpoint);
                internalLink = null;
            }
            if (bridge != null) {
                logger.info("Call: "+self().path()+" about to stop bridge endpoint: "+bridge.path());
                gateway.tell(new DestroyEndpoint(bridge), source);
                context().stop(bridge);
                bridge = null;
            }
            // Explicitly invalidate the application session.
            if (invite.getSession().isValid())
                invite.getSession().invalidate();
            if (invite.getApplicationSession().isValid())
                invite.getApplicationSession().invalidate();
            // Notify the observers.
            external = CallStateChanged.State.COMPLETED;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
            if (outgoingCallRecord != null && direction.contains("outbound")) {
                outgoingCallRecord = outgoingCallRecord.setStatus(external.name());
                final DateTime now = DateTime.now();
                outgoingCallRecord = outgoingCallRecord.setEndTime(now);
                final int seconds = (int) ((DateTime.now().getMillis() - outgoingCallRecord.getStartTime().getMillis()) / 1000);
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

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

package org.mobicents.servlet.restcomm.interpreter;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;
import org.mobicents.servlet.restcomm.telephony.ConferenceStateChanged;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdInterpreter extends BaseVoiceInterpreter{

    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // States for the FSM.
    private final State startDialing;
    private final State processingDialChildren;
    private final State acquiringOutboundCallInfo;
    private final State forking;
    private final State joiningCalls;
    private final State bridged;
    private final State finishDialing;
    private final State acquiringConferenceInfo;
    private final State acquiringConferenceMediaGroup;
    private final State initializingConferenceMediaGroup;
    private final State joiningConference;
    private final State conferencing;
    private final State finishConferencing;
    private final State downloadingRcml;
    private final State downloadingFallbackRcml;
    private final State initializingCall;
    private final State initializingCallMediaGroup;
    private final State acquiringCallMediaGroup;
    private final State ready;
    private final State notFound;
    private final State rejecting;
    private final State finished;

    // FSM.
    // The conference manager.
    private final ActorRef conferenceManager;

    // State for outbound calls.
    private boolean isForking;
    private List<ActorRef> dialBranches;
    private List<Tag> dialChildren;
    private Map<ActorRef, Tag> dialChildrenWithAttributes;

    // The conferencing stuff.
    private ActorRef conference;
    private ConferenceInfo conferenceInfo;
    private ActorRef confInterpreter;
    private ConferenceStateChanged.State conferenceState;
    private boolean callMuted;
    private boolean startConferenceOnEnter = true;
    private ActorRef confSubVoiceInterpreter;
    private ActorRef conferenceMediaGroup;

    public UssdInterpreter(final Configuration configuration, final Sid account, final Sid phone, final String version,
            final URI url, final String method, final URI fallbackUrl, final String fallbackMethod, final URI statusCallback,
            final String statusCallbackMethod, final String emailAddress, final ActorRef callManager,
            final ActorRef conferenceManager, final ActorRef sms, final DaoManager storage) {
        super();
        final ActorRef source = self();
        acquiringCallMediaGroup = new State("acquiring call media group", new AcquiringCallMediaGroup(source), null);
        downloadingRcml = new State("downloading rcml", new DownloadingRcml(source), null);
        downloadingFallbackRcml = new State("downloading fallback rcml", new DownloadingFallbackRcml(source), null);
        initializingCall = new State("initializing call", new InitializingCall(source), null);
        initializingCallMediaGroup = new State("initializing call media group", new InitializingCallMediaGroup(source), null);
        ready = new State("ready", new Ready(source), null);
        notFound = new State("notFound", new NotFound(source), null);
        rejecting = new State("rejecting", new Rejecting(source), null);
        startDialing = new State("start dialing", new StartDialing(source), null);
        processingDialChildren = new State("processing dial children", new ProcessingDialChildren(source), null);
        acquiringOutboundCallInfo = new State("acquiring outbound call info", new AcquiringOutboundCallInfo(source), null);
        forking = new State("forking", new Forking(source), null);
        joiningCalls = new State("joining calls", new JoiningCalls(source), null);
        bridged = new State("bridged", new Bridged(source), null);
        finishDialing = new State("finish dialing", new FinishDialing(source), null);
        acquiringConferenceInfo = new State("acquiring conference info", new AcquiringConferenceInfo(source), null);
        acquiringConferenceMediaGroup = new State("acquiring conference media group",
                new AcquiringConferenceMediaGroup(source), null);
        initializingConferenceMediaGroup = new State("initializing conference media group",
                new InitializingConferenceMediaGroup(source), null);
        joiningConference = new State("joining conference", new JoiningConference(source), null);
        conferencing = new State("conferencing", new Conferencing(source), null);
        finishConferencing = new State("finish conferencing", new FinishConferencing(source), null);
        finished = new State("finished", new Finished(source), null);
        /*
         * dialing = new State("dialing", null, null); bridging = new State("bridging", null, null); conferencing = new
         * State("conferencing", null, null);
         */
        transitions.add(new Transition(acquiringAsrInfo, finished));
        transitions.add(new Transition(acquiringSynthesizerInfo, finished));
        transitions.add(new Transition(acquiringCallInfo, initializingCall));
        transitions.add(new Transition(acquiringCallInfo, downloadingRcml));
        transitions.add(new Transition(acquiringCallInfo, finished));
        transitions.add(new Transition(acquiringCallInfo, acquiringCallMediaGroup));
        transitions.add(new Transition(initializingCall, acquiringCallMediaGroup));
        transitions.add(new Transition(initializingCall, hangingUp));
        transitions.add(new Transition(initializingCall, finished));
        transitions.add(new Transition(acquiringCallMediaGroup, initializingCallMediaGroup));
        transitions.add(new Transition(acquiringCallMediaGroup, hangingUp));
        transitions.add(new Transition(acquiringCallMediaGroup, finished));
        transitions.add(new Transition(initializingCallMediaGroup, faxing));
        transitions.add(new Transition(initializingCallMediaGroup, downloadingRcml));
        transitions.add(new Transition(initializingCallMediaGroup, playingRejectionPrompt));
        transitions.add(new Transition(initializingCallMediaGroup, pausing));
        transitions.add(new Transition(initializingCallMediaGroup, checkingCache));
        transitions.add(new Transition(initializingCallMediaGroup, caching));
        transitions.add(new Transition(initializingCallMediaGroup, synthesizing));
        transitions.add(new Transition(initializingCallMediaGroup, redirecting));
        transitions.add(new Transition(initializingCallMediaGroup, processingGatherChildren));
        transitions.add(new Transition(initializingCallMediaGroup, creatingRecording));
        transitions.add(new Transition(initializingCallMediaGroup, creatingSmsSession));
        transitions.add(new Transition(initializingCallMediaGroup, startDialing));
        transitions.add(new Transition(initializingCallMediaGroup, hangingUp));
        transitions.add(new Transition(initializingCallMediaGroup, finished));
        transitions.add(new Transition(downloadingRcml, ready));
        transitions.add(new Transition(downloadingRcml, notFound));
        transitions.add(new Transition(downloadingRcml, downloadingFallbackRcml));
        transitions.add(new Transition(downloadingRcml, hangingUp));
        transitions.add(new Transition(downloadingRcml, finished));
        transitions.add(new Transition(downloadingFallbackRcml, ready));
        transitions.add(new Transition(downloadingFallbackRcml, hangingUp));
        transitions.add(new Transition(downloadingFallbackRcml, finished));
        transitions.add(new Transition(ready, initializingCall));
        transitions.add(new Transition(ready, faxing));
        transitions.add(new Transition(ready, pausing));
        transitions.add(new Transition(ready, checkingCache));
        transitions.add(new Transition(ready, caching));
        transitions.add(new Transition(ready, synthesizing));
        transitions.add(new Transition(ready, rejecting));
        transitions.add(new Transition(ready, redirecting));
        transitions.add(new Transition(ready, processingGatherChildren));
        transitions.add(new Transition(ready, creatingRecording));
        transitions.add(new Transition(ready, creatingSmsSession));
        transitions.add(new Transition(ready, startDialing));
        transitions.add(new Transition(ready, hangingUp));
        transitions.add(new Transition(ready, finished));
        transitions.add(new Transition(pausing, ready));
        transitions.add(new Transition(pausing, finished));
        transitions.add(new Transition(rejecting, acquiringCallMediaGroup));
        transitions.add(new Transition(rejecting, finished));
        transitions.add(new Transition(faxing, ready));
        transitions.add(new Transition(faxing, finished));
        transitions.add(new Transition(caching, finished));
        transitions.add(new Transition(playing, ready));
        transitions.add(new Transition(playing, finished));
        transitions.add(new Transition(synthesizing, finished));
        transitions.add(new Transition(redirecting, ready));
        transitions.add(new Transition(redirecting, finished));
        transitions.add(new Transition(creatingRecording, finished));
        transitions.add(new Transition(finishRecording, ready));
        transitions.add(new Transition(finishRecording, finished));
        transitions.add(new Transition(processingGatherChildren, finished));
        transitions.add(new Transition(gathering, finished));
        transitions.add(new Transition(finishGathering, ready));
        transitions.add(new Transition(finishGathering, finished));
        transitions.add(new Transition(creatingSmsSession, finished));
        transitions.add(new Transition(sendingSms, ready));
        transitions.add(new Transition(sendingSms, finished));
        transitions.add(new Transition(startDialing, processingDialChildren));
        transitions.add(new Transition(startDialing, acquiringConferenceInfo));
        transitions.add(new Transition(startDialing, faxing));
        transitions.add(new Transition(startDialing, pausing));
        transitions.add(new Transition(startDialing, checkingCache));
        transitions.add(new Transition(startDialing, caching));
        transitions.add(new Transition(startDialing, synthesizing));
        transitions.add(new Transition(startDialing, redirecting));
        transitions.add(new Transition(startDialing, processingGatherChildren));
        transitions.add(new Transition(startDialing, creatingRecording));
        transitions.add(new Transition(startDialing, creatingSmsSession));
        transitions.add(new Transition(startDialing, startDialing));
        transitions.add(new Transition(startDialing, hangingUp));
        transitions.add(new Transition(startDialing, finished));
        transitions.add(new Transition(processingDialChildren, processingDialChildren));
        transitions.add(new Transition(processingDialChildren, forking));
        transitions.add(new Transition(processingDialChildren, hangingUp));
        transitions.add(new Transition(processingDialChildren, finished));
        transitions.add(new Transition(forking, acquiringOutboundCallInfo));
        transitions.add(new Transition(forking, finishDialing));
        transitions.add(new Transition(forking, hangingUp));
        transitions.add(new Transition(forking, finished));
        transitions.add(new Transition(acquiringOutboundCallInfo, joiningCalls));
        transitions.add(new Transition(acquiringOutboundCallInfo, hangingUp));
        transitions.add(new Transition(acquiringOutboundCallInfo, finished));
        transitions.add(new Transition(joiningCalls, bridged));
        transitions.add(new Transition(joiningCalls, hangingUp));
        transitions.add(new Transition(joiningCalls, finished));
        transitions.add(new Transition(bridged, finishDialing));
        transitions.add(new Transition(bridged, hangingUp));
        transitions.add(new Transition(bridged, finished));
        transitions.add(new Transition(finishDialing, ready));
        transitions.add(new Transition(finishDialing, faxing));
        transitions.add(new Transition(finishDialing, pausing));
        transitions.add(new Transition(finishDialing, checkingCache));
        transitions.add(new Transition(finishDialing, caching));
        transitions.add(new Transition(finishDialing, synthesizing));
        transitions.add(new Transition(finishDialing, redirecting));
        transitions.add(new Transition(finishDialing, processingGatherChildren));
        transitions.add(new Transition(finishDialing, creatingRecording));
        transitions.add(new Transition(finishDialing, creatingSmsSession));
        transitions.add(new Transition(finishDialing, startDialing));
        transitions.add(new Transition(finishDialing, hangingUp));
        transitions.add(new Transition(finishDialing, finished));
        transitions.add(new Transition(acquiringConferenceInfo, acquiringConferenceMediaGroup));
        transitions.add(new Transition(acquiringConferenceInfo, hangingUp));
        transitions.add(new Transition(acquiringConferenceInfo, finished));
        transitions.add(new Transition(acquiringConferenceMediaGroup, initializingConferenceMediaGroup));
        transitions.add(new Transition(acquiringConferenceMediaGroup, faxing));
        transitions.add(new Transition(acquiringConferenceMediaGroup, pausing));
        transitions.add(new Transition(acquiringConferenceMediaGroup, checkingCache));
        transitions.add(new Transition(acquiringConferenceMediaGroup, caching));
        transitions.add(new Transition(acquiringConferenceMediaGroup, synthesizing));
        transitions.add(new Transition(acquiringConferenceMediaGroup, redirecting));
        transitions.add(new Transition(acquiringConferenceMediaGroup, processingGatherChildren));
        transitions.add(new Transition(acquiringConferenceMediaGroup, creatingRecording));
        transitions.add(new Transition(acquiringConferenceMediaGroup, creatingSmsSession));
        transitions.add(new Transition(acquiringConferenceMediaGroup, startDialing));
        transitions.add(new Transition(acquiringConferenceMediaGroup, hangingUp));
        transitions.add(new Transition(acquiringConferenceMediaGroup, finished));
        transitions.add(new Transition(initializingConferenceMediaGroup, joiningConference));
        transitions.add(new Transition(initializingConferenceMediaGroup, hangingUp));
        transitions.add(new Transition(initializingConferenceMediaGroup, finished));
        transitions.add(new Transition(joiningConference, conferencing));
        transitions.add(new Transition(joiningConference, hangingUp));
        transitions.add(new Transition(joiningConference, finished));
        transitions.add(new Transition(conferencing, finishConferencing));
        transitions.add(new Transition(conferencing, hangingUp));
        transitions.add(new Transition(conferencing, finished));
        transitions.add(new Transition(finishConferencing, ready));
        transitions.add(new Transition(finishConferencing, faxing));
        transitions.add(new Transition(finishConferencing, pausing));
        transitions.add(new Transition(finishConferencing, checkingCache));
        transitions.add(new Transition(finishConferencing, caching));
        transitions.add(new Transition(finishConferencing, synthesizing));
        transitions.add(new Transition(finishConferencing, redirecting));
        transitions.add(new Transition(finishConferencing, processingGatherChildren));
        transitions.add(new Transition(finishConferencing, creatingRecording));
        transitions.add(new Transition(finishConferencing, creatingSmsSession));
        transitions.add(new Transition(finishConferencing, startDialing));
        transitions.add(new Transition(finishConferencing, hangingUp));
        transitions.add(new Transition(finishConferencing, finished));
        transitions.add(new Transition(hangingUp, finished));
        transitions.add(new Transition(hangingUp, finishDialing));
        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the runtime stuff.
        this.accountId = account;
        this.phoneId = phone;
        this.version = version;
        this.url = url;
        this.method = method;
        this.fallbackUrl = fallbackUrl;
        this.fallbackMethod = fallbackMethod;
        this.statusCallback = statusCallback;
        this.statusCallbackMethod = statusCallbackMethod;
        this.emailAddress = emailAddress;
        this.configuration = configuration;
        this.callManager = callManager;
        this.conferenceManager = conferenceManager;
        this.asrService = asr(configuration.subset("speech-recognizer"));
        this.faxService = fax(configuration.subset("fax-service"));
        this.smsService = sms;
        this.smsSessions = new HashMap<Sid, ActorRef>();
        this.storage = storage;
        this.synthesizer = tts(configuration.subset("speech-synthesizer"));
        this.mailer = mailer(configuration.subset("smtp"));
        final Configuration runtime = configuration.subset("runtime-settings");
        String path = runtime.getString("cache-path");
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + accountId.toString();
        cachePath = path;
        String uri = runtime.getString("cache-uri");
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        uri = uri + accountId.toString();
        this.cache = cache(path, uri);
        this.downloader = downloader();
    }

    private Notification notification(final int log, final int error, final String message) {
        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(accountId);
        builder.setCallSid(callInfo.sid());
        builder.setApiVersion(version);
        builder.setLog(log);
        builder.setErrorCode(error);
        final String base = configuration.subset("runtime-settings").getString("error-dictionary-uri");
        StringBuilder buffer = new StringBuilder();
        buffer.append(base);
        if (!base.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append(error).append(".html");
        final URI info = URI.create(buffer.toString());
        builder.setMoreInfo(info);
        builder.setMessageText(message);
        final DateTime now = DateTime.now();
        builder.setMessageDate(now);
        if (request != null) {
            builder.setRequestUrl(request.getUri());
            builder.setRequestMethod(request.getMethod());
            builder.setRequestVariables(request.getParametersAsString());
        }
        if (response != null) {
            builder.setResponseHeaders(response.getHeadersAsString());
            final String type = response.getContentType();
            if (type.contains("text/xml") || type.contains("application/xml") || type.contains("text/html")) {
                try {
                    builder.setResponseBody(response.getContentAsString());
                } catch (final IOException exception) {
                    logger.error(
                            "There was an error while reading the contents of the resource " + "located @ " + url.toString(),
                            exception);
                }
            }
        }
        buffer = new StringBuilder();
        buffer.append("/").append(version).append("/Accounts/");
        buffer.append(accountId.toString()).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
    }
    
    /* (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.interpreter.BaseVoiceInterpreter#onReceive(java.lang.Object)
     */
    @Override
    public void onReceive(Object arg0) throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.interpreter.BaseVoiceInterpreter#parameters()
     */
    @Override
    List<NameValuePair> parameters() {
        // TODO Auto-generated method stub
        return null;
    }

}

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
package org.mobicents.servlet.restcomm.interpreter;

import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.fax;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.gather;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.hangup;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.pause;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.play;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.record;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.redirect;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.reject;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.say;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.sms;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.asr.AsrResponse;
import org.mobicents.servlet.restcomm.cache.DiskCacheResponse;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fax.FaxResponse;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.http.client.DownloaderResponse;
import org.mobicents.servlet.restcomm.http.client.HttpRequestDescriptor;
import org.mobicents.servlet.restcomm.interpreter.rcml.Attribute;
import org.mobicents.servlet.restcomm.interpreter.rcml.End;
import org.mobicents.servlet.restcomm.interpreter.rcml.GetNextVerb;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.interpreter.rcml.Verbs;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.sms.SmsServiceResponse;
import org.mobicents.servlet.restcomm.sms.SmsSessionResponse;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Cancel;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.restcomm.telephony.DestroyCall;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author gvagenas@telestax.com
 * @author jean.deruelle@telestax.com
 * @author pavel.slegr@telestax.com
 */
public final class SubVoiceInterpreter extends BaseVoiceInterpreter {
    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // States for the FSM.
    private final State downloadingRcml;
    private final State ready;
    private final State notFound;
    private final State rejecting;
    private final State finished;

    // application data.
    private DownloaderResponse downloaderResponse;
    private ActorRef source;
    private Boolean hangupOnEnd;
    private ActorRef originalInterpreter;

    public SubVoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone, final String version,
            final URI url, final String method, final URI fallbackUrl, final String fallbackMethod, final URI statusCallback,
            final String statusCallbackMethod, final String emailAddress, final ActorRef callManager,
            final ActorRef conferenceManager, final ActorRef sms, final DaoManager storage) {

        this(configuration, account, phone, version, url, method, fallbackUrl, fallbackMethod, statusCallback,
                statusCallbackMethod, emailAddress, callManager, conferenceManager, sms, storage, false);
    }

    public SubVoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone, final String version,
            final URI url, final String method, final URI fallbackUrl, final String fallbackMethod, final URI statusCallback,
            final String statusCallbackMethod, final String emailAddress, final ActorRef callManager,
            final ActorRef conferenceManager, final ActorRef sms, final DaoManager storage, final Boolean hangupOnEnd) {
        super();
        source = self();
        downloadingRcml = new State("downloading rcml", new DownloadingRcml(source), null);
        ready = new State("ready", new Ready(source), null);
        notFound = new State("notFound", new NotFound(source), null);
        rejecting = new State("rejecting", new Rejecting(source), null);
        finished = new State("finished", new Finished(source), null);

        transitions.add(new Transition(acquiringAsrInfo, finished));
        transitions.add(new Transition(acquiringSynthesizerInfo, finished));
        transitions.add(new Transition(acquiringCallInfo, downloadingRcml));
        transitions.add(new Transition(acquiringCallInfo, finished));
        transitions.add(new Transition(downloadingRcml, ready));
        transitions.add(new Transition(downloadingRcml, notFound));
        transitions.add(new Transition(downloadingRcml, hangingUp));
        transitions.add(new Transition(downloadingRcml, finished));
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
        transitions.add(new Transition(ready, hangingUp));
        transitions.add(new Transition(ready, finished));
        transitions.add(new Transition(pausing, ready));
        transitions.add(new Transition(pausing, finished));
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
        transitions.add(new Transition(finishGathering, finished));
        transitions.add(new Transition(creatingSmsSession, finished));
        transitions.add(new Transition(sendingSms, ready));
        transitions.add(new Transition(sendingSms, finished));
        transitions.add(new Transition(hangingUp, finished));

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
        this.asrService = asr(configuration.subset("speech-recognizer"));
        this.faxService = fax(configuration.subset("fax-service"));
        this.smsService = sms;
        this.smsSessions = new HashMap<Sid, ActorRef>();
        this.storage = storage;
        this.synthesizer = tts(configuration.subset("speech-synthesizer"));
        this.mailerNotify = mailer(configuration.subset("smtp-notify"));
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
        this.hangupOnEnd = hangupOnEnd;
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

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();
        final ActorRef sender = sender();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** SubVoiceInterpreter's Current State: " + state.toString());
            logger.info(" ********** SubVoiceInterpreter's Processing Message: " + klass.getName());
        }

        if (StartInterpreter.class.equals(klass)) {
            originalInterpreter = sender;
            fsm.transition(message, acquiringAsrInfo);
        } else if (AsrResponse.class.equals(klass)) {
            if (outstandingAsrRequests > 0) {
                asrResponse(message);
            } else {
                fsm.transition(message, acquiringSynthesizerInfo);
            }
        } else if (SpeechSynthesizerResponse.class.equals(klass)) {
            if (acquiringSynthesizerInfo.equals(state)) {
                fsm.transition(message, acquiringCallInfo);
            } else if (synthesizing.equals(state)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                if (response.succeeded()) {
                    fsm.transition(message, caching);
                } else {
                    fsm.transition(message, hangingUp);
                }
            } else if (processingGatherChildren.equals(state)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                if (response.succeeded()) {
                    fsm.transition(message, processingGatherChildren);
                } else {
                    fsm.transition(message, hangingUp);
                }
            }
        } else if (CallResponse.class.equals(klass)) {
            if (acquiringCallInfo.equals(state)) {
                final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
                callInfo = response.get();
                fsm.transition(message, downloadingRcml);
            }
        } else if (DownloaderResponse.class.equals(klass)) {
            downloaderResponse = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("response succeeded " + downloaderResponse.succeeded() + ", statusCode "
                        + downloaderResponse.get().getStatusCode());
            }
            if (downloaderResponse.succeeded() && HttpStatus.SC_OK == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, ready);
            } else if (downloaderResponse.succeeded() && HttpStatus.SC_NOT_FOUND == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, notFound);
            }
        } else if (DiskCacheResponse.class.equals(klass)) {
            final DiskCacheResponse response = (DiskCacheResponse) message;
            if (response.succeeded()) {
                if (caching.equals(state) || checkingCache.equals(state)) {
                    if (play.equals(verb.name()) || say.equals(verb.name())) {
                        fsm.transition(message, playing);
                    } else if (fax.equals(verb.name())) {
                        fsm.transition(message, faxing);
                    }
                } else if (processingGatherChildren.equals(state)) {
                    fsm.transition(message, processingGatherChildren);
                }
            } else {
                if (checkingCache.equals(state)) {
                    fsm.transition(message, synthesizing);
                } else {
                    fsm.transition(message, hangingUp);
                }
            }
        } else if (Tag.class.equals(klass)) {
            verb = (Tag) message;

            if (Verbs.dial.equals(verb.name()))
                originalInterpreter.tell(new Exception("Dial verb not supported"), source);

            if (reject.equals(verb.name())) {
                fsm.transition(message, rejecting);
            } else if (pause.equals(verb.name())) {
                fsm.transition(message, pausing);
            } else if (fax.equals(verb.name())) {
                fsm.transition(message, caching);
            } else if (play.equals(verb.name())) {
                fsm.transition(message, caching);
            } else if (say.equals(verb.name())) {
                fsm.transition(message, checkingCache);
            } else if (gather.equals(verb.name())) {
                fsm.transition(message, processingGatherChildren);
            } else if (pause.equals(verb.name())) {
                fsm.transition(message, pausing);
            } else if (hangup.equals(verb.name())) {
                fsm.transition(message, hangingUp);
            } else if (redirect.equals(verb.name())) {
                fsm.transition(message, redirecting);
            } else if (record.equals(verb.name())) {
                fsm.transition(message, creatingRecording);
            } else if (sms.equals(verb.name())) {
                fsm.transition(message, creatingSmsSession);
            } else {
                invalidVerb(verb);
            }
        } else if (End.class.equals(klass)) {
            if (!hangupOnEnd) {
                originalInterpreter.tell(message, source);
            } else {
                fsm.transition(message, hangingUp);
            }
        } else if (StartGathering.class.equals(klass)) {
            fsm.transition(message, gathering);
        } else if (CallStateChanged.class.equals(klass)) {
            final CallStateChanged event = (CallStateChanged) message;
            if (CallStateChanged.State.NO_ANSWER == event.state() || CallStateChanged.State.COMPLETED == event.state()
                    || CallStateChanged.State.FAILED == event.state() || CallStateChanged.State.BUSY == event.state()) {

                originalInterpreter.tell(new Cancel(), source);
            }
        } else if (MediaGroupResponse.class.equals(klass)) {
            final MediaGroupResponse<String> response = (MediaGroupResponse<String>) message;
            if (response.succeeded()) {
                if (playingRejectionPrompt.equals(state)) {
                    originalInterpreter.tell(message, source);
                } else if (playing.equals(state)) {
                    fsm.transition(message, ready);
                } else if (creatingRecording.equals(state)) {
                    fsm.transition(message, finishRecording);
                } else if (gathering.equals(state)) {
                    fsm.transition(message, finishGathering);
                }
            } else {
                originalInterpreter.tell(message, source);
            }
        } else if (SmsServiceResponse.class.equals(klass)) {
            final SmsServiceResponse<ActorRef> response = (SmsServiceResponse<ActorRef>) message;
            if (response.succeeded()) {
                if (creatingSmsSession.equals(state)) {
                    fsm.transition(message, sendingSms);
                }
            } else {
                fsm.transition(message, hangingUp);
            }
        } else if (SmsSessionResponse.class.equals(klass)) {
            smsResponse(message);
        } else if (FaxResponse.class.equals(klass)) {
            fsm.transition(message, ready);
        } else if (StopInterpreter.class.equals(klass)) {
            if (CallStateChanged.State.IN_PROGRESS == callState) {
                fsm.transition(message, hangingUp);
            } else {
                fsm.transition(message, finished);
            }
        } else if (message instanceof ReceiveTimeout) {
            if (pausing.equals(state)) {
                fsm.transition(message, ready);
            }
        }
    }

    @Override
    List<NameValuePair> parameters() {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        final String callSid = callInfo.sid().toString();
        parameters.add(new BasicNameValuePair("CallSid", callSid));
        final String accountSid = accountId.toString();
        parameters.add(new BasicNameValuePair("AccountSid", accountSid));
        final String from = e164(callInfo.from());
        parameters.add(new BasicNameValuePair("From", from));
        final String to = e164(callInfo.to());
        parameters.add(new BasicNameValuePair("To", to));
        final String state = callState.toString();
        parameters.add(new BasicNameValuePair("CallStatus", state));
        parameters.add(new BasicNameValuePair("ApiVersion", version));
        final String direction = callInfo.direction();
        parameters.add(new BasicNameValuePair("Direction", direction));
        final String callerName = callInfo.fromName();
        parameters.add(new BasicNameValuePair("CallerName", callerName));
        final String forwardedFrom = callInfo.forwardedFrom();
        parameters.add(new BasicNameValuePair("ForwardedFrom", forwardedFrom));
        // Adding SIP OUT Headers and SipCallId for
        // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
        if (CreateCall.Type.SIP == callInfo.type()) {
            SipServletResponse lastResponse = callInfo.lastResponse();
            if (lastResponse != null) {
                final int statusCode = lastResponse.getStatus();
                final String method = lastResponse.getMethod();
                // See https://www.twilio.com/docs/sip/receiving-sip-headers
                // On a successful call setup (when a 200 OK SIP response is returned) any X-headers on the 200 OK message are
                // posted to the call screening URL
                if (statusCode >= 200 && statusCode < 300 && "INVITE".equalsIgnoreCase(method)) {
                    final String sipCallId = lastResponse.getCallId();
                    parameters.add(new BasicNameValuePair("SipCallId", sipCallId));
                    Iterator<String> headerIt = lastResponse.getHeaderNames();
                    while (headerIt.hasNext()) {
                        String headerName = headerIt.next();
                        if (headerName.startsWith("X-")) {
                            parameters
                                    .add(new BasicNameValuePair("SipHeader_" + headerName, lastResponse.getHeader(headerName)));
                        }
                    }
                }
            }
        }
        return parameters;
    }

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class DownloadingRcml extends AbstractAction {
        public DownloadingRcml(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallResponse.class.equals(klass)) {
                final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
                callInfo = response.get();
                callState = callInfo.state();
                // Ask the downloader to get us the application that will be executed.
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(url, method, parameters);
                downloader.tell(request, source);
            }
        }
    }

    private final class Ready extends AbstractAction {
        public Ready(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (parser == null) {
                response = downloaderResponse.get();

                final String type = response.getContentType();
                if (type.contains("text/xml") || type.contains("application/xml") || type.contains("text/html")) {
                    parser = parser(response.getContentAsString());
                } else if (type.contains("audio/wav") || type.contains("audio/wave") || type.contains("audio/x-wav")) {
                    parser = parser("<Play>" + request.getUri() + "</Play>");
                } else if (type.contains("text/plain")) {
                    parser = parser("<Say>" + response.getContentAsString() + "</Say>");
                } else {
                    final StopInterpreter stop = new StopInterpreter();
                    source.tell(stop, source);
                    return;
                }
            }
            // Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
        }
    }

    private final class NotFound extends AbstractAction {
        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final DownloaderResponse response = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("response succeeded " + response.succeeded() + ", statusCode " + response.get().getStatusCode());
            }
            final Notification notification = notification(WARNING_NOTIFICATION, 21402, "URL Not Found : "
                    + response.get().getURI());
            final NotificationsDao notifications = storage.getNotificationsDao();
            notifications.addNotification(notification);
            // Hang up the call.
            call.tell(new org.mobicents.servlet.restcomm.telephony.NotFound(), source);
        }
    }

    private final class Rejecting extends AbstractAction {
        public Rejecting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            String reason = "rejected";
            Attribute attribute = verb.attribute("reason");
            if (attribute != null) {
                reason = attribute.value();
                if (reason != null && !reason.isEmpty()) {
                    if ("rejected".equalsIgnoreCase(reason)) {
                        reason = "rejected";
                    } else if ("busy".equalsIgnoreCase(reason)) {
                        reason = "busy";
                    } else {
                        reason = "rejected";
                    }
                } else {
                    reason = "rejected";
                }
            }
            // Reject the call.
            if ("rejected".equals(reason)) {
                call.tell(new Answer(), source);
            } else {
                call.tell(new Reject(), source);
            }
        }
    }

    private final class Finished extends AbstractAction {

        public Finished(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();

            if (CallStateChanged.class.equals(klass)) {
                final CallStateChanged event = (CallStateChanged) message;
                callState = event.state();
                if (callRecord != null) {
                    callRecord = callRecord.setStatus(callState.toString());
                    final DateTime end = DateTime.now();
                    callRecord = callRecord.setEndTime(end);
                    final int seconds = (int) (end.getMillis() - callRecord.getStartTime().getMillis()) / 1000;
                    callRecord = callRecord.setDuration(seconds);
                    final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
                    records.updateCallDetailRecord(callRecord);
                }
                callback();
            }

            // Stop the media group(s).
            if (call != null) {
                final StopMediaGroup stop = new StopMediaGroup();
                call.tell(stop, source);
            }

            // Destroy the Call(s).
            callManager.tell(new DestroyCall(call), source);

            // Stop the dependencies.
            final UntypedActorContext context = getContext();
            context.stop(mailerNotify);
            context.stop(downloader);
            context.stop(asrService);
            context.stop(faxService);
            context.stop(cache);
            context.stop(synthesizer);

            // Stop the interpreter.
            postCleanup();
        }
    }
}

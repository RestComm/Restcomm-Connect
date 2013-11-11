/*
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
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.sms.SmsServiceResponse;
import org.mobicents.servlet.restcomm.sms.SmsSessionResponse;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Cancel;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.restcomm.telephony.CreateMediaGroup;
import org.mobicents.servlet.restcomm.telephony.DestroyCall;
import org.mobicents.servlet.restcomm.telephony.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.telephony.MediaGroupResponse;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStatus;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.telephony.StartMediaGroup;
import org.mobicents.servlet.restcomm.telephony.StopMediaGroup;
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
    private final State initializingCallMediaGroup;
    private final State acquiringCallMediaGroup;
    private final State ready;
    private final State notFound;
    private final State rejecting;
    private final State finished;
    private final State checkingMediaGroupState;
    
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
        acquiringCallMediaGroup = new State("acquiring call media group", new AcquiringCallMediaGroup(source), null);
        downloadingRcml = new State("downloading rcml", new DownloadingRcml(source), null);
        initializingCallMediaGroup = new State("initializing call media group", new InitializingCallMediaGroup(source), null);
        ready = new State("ready", new Ready(source), null);
        notFound = new State("notFound", new NotFound(source), null);
        rejecting = new State("rejecting", new Rejecting(source), null);
        finished = new State("finished", new Finished(source), null);
        checkingMediaGroupState = new State("checkingMediaGroupState", new CheckMediaGroupState(source), null);

//		transitions.add(new Transition(uninitialized, acquiringAsrInfo));
//		transitions.add(new Transition(acquiringAsrInfo, acquiringSynthesizerInfo));
		transitions.add(new Transition(acquiringAsrInfo, finished));
//		transitions.add(new Transition(acquiringSynthesizerInfo, acquiringCallInfo));
		transitions.add(new Transition(acquiringSynthesizerInfo, finished));
		transitions.add(new Transition(acquiringCallInfo, downloadingRcml));
		transitions.add(new Transition(acquiringCallInfo, finished));
		
		transitions.add(new Transition(acquiringCallMediaGroup, checkingMediaGroupState));
		transitions.add(new Transition(acquiringCallMediaGroup, initializingCallMediaGroup));
		transitions.add(new Transition(acquiringCallMediaGroup, hangingUp));
		transitions.add(new Transition(acquiringCallMediaGroup, finished));
		
		transitions.add(new Transition(checkingMediaGroupState, initializingCallMediaGroup));
		transitions.add(new Transition(checkingMediaGroupState, faxing));
		transitions.add(new Transition(checkingMediaGroupState, downloadingRcml));
		transitions.add(new Transition(checkingMediaGroupState, playingRejectionPrompt));
		transitions.add(new Transition(checkingMediaGroupState, pausing));
		transitions.add(new Transition(checkingMediaGroupState, checkingCache));
		transitions.add(new Transition(checkingMediaGroupState, caching));
		transitions.add(new Transition(checkingMediaGroupState, synthesizing));
		transitions.add(new Transition(checkingMediaGroupState, redirecting));
		transitions.add(new Transition(checkingMediaGroupState, processingGatherChildren));
		transitions.add(new Transition(checkingMediaGroupState, creatingRecording));
		transitions.add(new Transition(checkingMediaGroupState, creatingSmsSession));
		transitions.add(new Transition(checkingMediaGroupState, hangingUp));
		transitions.add(new Transition(checkingMediaGroupState, finished));
		transitions.add(new Transition(checkingMediaGroupState, ready));
		
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
		transitions.add(new Transition(initializingCallMediaGroup, hangingUp));
		transitions.add(new Transition(initializingCallMediaGroup, finished));
		transitions.add(new Transition(initializingCallMediaGroup, ready));
		transitions.add(new Transition(downloadingRcml, ready));
		transitions.add(new Transition(downloadingRcml, notFound));
		transitions.add(new Transition(downloadingRcml, hangingUp));
		transitions.add(new Transition(downloadingRcml, finished));
		transitions.add(new Transition(downloadingRcml, acquiringCallMediaGroup));
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
//		transitions.add(new Transition(pausing, hangingUp));
		transitions.add(new Transition(pausing, finished));
		transitions.add(new Transition(rejecting, acquiringCallMediaGroup));
		transitions.add(new Transition(rejecting, finished));
//		transitions.add(new Transition(playingRejectionPrompt, hangingUp));
//		transitions.add(new Transition(faxing, faxing));
		transitions.add(new Transition(faxing, ready));
//		transitions.add(new Transition(faxing, caching));
//		transitions.add(new Transition(faxing, pausing));
//		transitions.add(new Transition(faxing, redirecting));
//		transitions.add(new Transition(faxing, synthesizing));
//		transitions.add(new Transition(faxing, processingGatherChildren));
//		transitions.add(new Transition(faxing, creatingRecording));
//		transitions.add(new Transition(faxing, creatingSmsSession));
//		transitions.add(new Transition(faxing, hangingUp));
		transitions.add(new Transition(faxing, finished));
//		transitions.add(new Transition(caching, faxing));
//		transitions.add(new Transition(caching, playing));
//		transitions.add(new Transition(caching, caching));
//		transitions.add(new Transition(caching, pausing));
//		transitions.add(new Transition(caching, redirecting));
//		transitions.add(new Transition(caching, synthesizing));
//		transitions.add(new Transition(caching, processingGatherChildren));
//		transitions.add(new Transition(caching, creatingRecording));
//		transitions.add(new Transition(caching, creatingSmsSession));
//		transitions.add(new Transition(caching, hangingUp));
		transitions.add(new Transition(caching, finished));
//		transitions.add(new Transition(checkingCache, synthesizing));
//		transitions.add(new Transition(checkingCache, playing));
//		transitions.add(new Transition(checkingCache, checkingCache));
		transitions.add(new Transition(playing, ready));
//		transitions.add(new Transition(playing, hangingUp));
		transitions.add(new Transition(playing, finished));
//		transitions.add(new Transition(synthesizing, faxing));
//		transitions.add(new Transition(synthesizing, pausing));
//		transitions.add(new Transition(synthesizing, checkingCache));
//		transitions.add(new Transition(synthesizing, caching));
//		transitions.add(new Transition(synthesizing, redirecting));
//		transitions.add(new Transition(synthesizing, processingGatherChildren));
//		transitions.add(new Transition(synthesizing, creatingRecording));
//		transitions.add(new Transition(synthesizing, creatingSmsSession));
//		transitions.add(new Transition(synthesizing, synthesizing));
//		transitions.add(new Transition(synthesizing, hangingUp));
		transitions.add(new Transition(synthesizing, finished));
//		transitions.add(new Transition(redirecting, faxing));
		transitions.add(new Transition(redirecting, ready));
//		transitions.add(new Transition(redirecting, pausing));
//		transitions.add(new Transition(redirecting, checkingCache));
//		transitions.add(new Transition(redirecting, caching));
//		transitions.add(new Transition(redirecting, synthesizing));
//		transitions.add(new Transition(redirecting, redirecting));
//		transitions.add(new Transition(redirecting, processingGatherChildren));
//		transitions.add(new Transition(redirecting, creatingRecording));
//		transitions.add(new Transition(redirecting, creatingSmsSession));
//		transitions.add(new Transition(redirecting, hangingUp));
		transitions.add(new Transition(redirecting, finished));
//		transitions.add(new Transition(creatingRecording, finishRecording));
//		transitions.add(new Transition(creatingRecording, hangingUp));
		transitions.add(new Transition(creatingRecording, finished));
//		transitions.add(new Transition(finishRecording, faxing));
		transitions.add(new Transition(finishRecording, ready));
//		transitions.add(new Transition(finishRecording, pausing));
//		transitions.add(new Transition(finishRecording, checkingCache));
//		transitions.add(new Transition(finishRecording, caching));
//		transitions.add(new Transition(finishRecording, synthesizing));
//		transitions.add(new Transition(finishRecording, redirecting));
//		transitions.add(new Transition(finishRecording, processingGatherChildren));
//		transitions.add(new Transition(finishRecording, creatingRecording));
//		transitions.add(new Transition(finishRecording, creatingSmsSession));
//		transitions.add(new Transition(finishRecording, hangingUp));
		transitions.add(new Transition(finishRecording, finished));
//		transitions.add(new Transition(processingGatherChildren, processingGatherChildren));
//		transitions.add(new Transition(processingGatherChildren, gathering));
//		transitions.add(new Transition(processingGatherChildren, hangingUp));
		transitions.add(new Transition(processingGatherChildren, finished));
//		transitions.add(new Transition(gathering, finishGathering));
//		transitions.add(new Transition(gathering, hangingUp));
		transitions.add(new Transition(gathering, finished));
//		transitions.add(new Transition(finishGathering, ready));
//		transitions.add(new Transition(finishGathering, faxing));
//		transitions.add(new Transition(finishGathering, pausing));
//		transitions.add(new Transition(finishGathering, checkingCache));
//		transitions.add(new Transition(finishGathering, caching));
//		transitions.add(new Transition(finishGathering, synthesizing));
//		transitions.add(new Transition(finishGathering, redirecting));
//		transitions.add(new Transition(finishGathering, processingGatherChildren));
//		transitions.add(new Transition(finishGathering, creatingRecording));
//		transitions.add(new Transition(finishGathering, creatingSmsSession));
//		transitions.add(new Transition(finishGathering, hangingUp));
		transitions.add(new Transition(finishGathering, finished));
//		transitions.add(new Transition(creatingSmsSession, sendingSms));
//		transitions.add(new Transition(creatingSmsSession, hangingUp));
		transitions.add(new Transition(creatingSmsSession, finished));
//		transitions.add(new Transition(sendingSms, faxing));
		transitions.add(new Transition(sendingSms, ready));
//		transitions.add(new Transition(sendingSms, pausing));
//		transitions.add(new Transition(sendingSms, caching));
//		transitions.add(new Transition(sendingSms, synthesizing));
//		transitions.add(new Transition(sendingSms, redirecting));
//		transitions.add(new Transition(sendingSms, processingGatherChildren));
//		transitions.add(new Transition(sendingSms, creatingRecording));
//		transitions.add(new Transition(sendingSms, creatingSmsSession));
//		transitions.add(new Transition(sendingSms, hangingUp));
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
            } else if (acquiringCallMediaGroup.equals(state)) {
                fsm.transition(message, checkingMediaGroupState);
            }
        } else if (DownloaderResponse.class.equals(klass)) {
            downloaderResponse = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("response succeeded " + downloaderResponse.succeeded() + ", statusCode "
                        + downloaderResponse.get().getStatusCode());
            }
            if (downloaderResponse.succeeded() && HttpStatus.SC_OK == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, acquiringCallMediaGroup);
            } else if (downloaderResponse.succeeded() && HttpStatus.SC_NOT_FOUND == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, notFound);
            }
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            final MediaGroupStateChanged event = (MediaGroupStateChanged) message;
            if (MediaGroupStateChanged.State.ACTIVE == event.state()) {
                if (initializingCallMediaGroup.equals(state) || checkingMediaGroupState.equals(state)) {
                    fsm.transition(message, ready);
                }
                if (ready.equals(state)) {
                    if (reject.equals(verb.name())) {
                        fsm.transition(message, playingRejectionPrompt);
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
                }
            } else if (MediaGroupStateChanged.State.INACTIVE == event.state()) {
                if (checkingMediaGroupState.equals(state)) {
                    fsm.transition(message, initializingCallMediaGroup);
                } else if (!hangingUp.equals(state)) {
                    fsm.transition(message, hangingUp);
                }
            }
        }

        else if (DiskCacheResponse.class.equals(klass)) {
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
        }
        // else if(AsrResponse.class.equals(klass)) {
        // asrResponse(message);
        // }
        else if (SmsSessionResponse.class.equals(klass)) {
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

    /* (non-Javadoc)
     * @see org.mobicents.servlet.restcomm.interpreter.BaseVoiceInterpreter#parameters()
     */
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


    private final class AcquiringCallMediaGroup extends AbstractAction {
        public AcquiringCallMediaGroup(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            call.tell(new CreateMediaGroup(), source);
        }
    }

    private final class CheckMediaGroupState extends AbstractAction {
        public CheckMediaGroupState(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallResponse.class.equals(klass)) {
                final CallResponse<ActorRef> response = (CallResponse<ActorRef>) message;
                callMediaGroup = response.get();
                // Ask CallMediaGroup to add us as Observer, if the callMediaGroup is active we will not reach
                // InitializingCallMediaGroup where
                // we were adding SubVoiceInterpreter as an observer. Better do it here.
                callMediaGroup.tell(new Observe(source), source);
                MediaGroupStatus status = new MediaGroupStatus();
                callMediaGroup.tell(status, source);
            }
        }

    }

    private final class InitializingCallMediaGroup extends AbstractAction {
        public InitializingCallMediaGroup(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (MediaGroupStateChanged.class.equals(klass)) {
                // final CallResponse<ActorRef> response = (CallResponse<ActorRef>)message;
                // callMediaGroup = response.get();
                // callMediaGroup.tell(new Observe(source), source);
                callMediaGroup.tell(new StartMediaGroup(), source);
            } else if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
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
            final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
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
            final UntypedActorContext context = getContext();
            final State state = fsm.state();
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
                    final StopInterpreter stop = StopInterpreter.instance();
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
            final Class<?> klass = message.getClass();
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
            // Cleanup the outbound call if necessary.
            final State state = fsm.state();
            final StopMediaGroup stop = new StopMediaGroup();
            // Destroy the media group(s).
            if (callMediaGroup != null) {
                callMediaGroup.tell(stop, source);
                final DestroyMediaGroup destroy = new DestroyMediaGroup(callMediaGroup);
                call.tell(destroy, source);
                callMediaGroup = null;
            }
            // Destroy the Call(s).
            callManager.tell(new DestroyCall(call), source);
            // Stop the dependencies.
            final UntypedActorContext context = getContext();
            context.stop(mailer);
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

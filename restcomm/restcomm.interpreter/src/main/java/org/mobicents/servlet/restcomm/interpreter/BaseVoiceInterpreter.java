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

import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.pause;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.play;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.say;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.asr.AsrInfo;
import org.mobicents.servlet.restcomm.asr.AsrRequest;
import org.mobicents.servlet.restcomm.asr.AsrResponse;
import org.mobicents.servlet.restcomm.asr.GetAsrInfo;
import org.mobicents.servlet.restcomm.asr.ISpeechAsr;
import org.mobicents.servlet.restcomm.cache.DiskCache;
import org.mobicents.servlet.restcomm.cache.DiskCacheRequest;
import org.mobicents.servlet.restcomm.cache.DiskCacheResponse;
import org.mobicents.servlet.restcomm.cache.HashGenerator;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.restcomm.email.Mail;
import org.mobicents.servlet.restcomm.email.MailMan;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Direction;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Status;
import org.mobicents.servlet.restcomm.entities.Transcription;
import org.mobicents.servlet.restcomm.fax.FaxRequest;
import org.mobicents.servlet.restcomm.fax.InterfaxService;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.http.client.Downloader;
import org.mobicents.servlet.restcomm.http.client.DownloaderResponse;
import org.mobicents.servlet.restcomm.http.client.HttpRequestDescriptor;
import org.mobicents.servlet.restcomm.http.client.HttpResponseDescriptor;
import org.mobicents.servlet.restcomm.interpreter.rcml.Attribute;
import org.mobicents.servlet.restcomm.interpreter.rcml.GetNextVerb;
import org.mobicents.servlet.restcomm.interpreter.rcml.Parser;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.sms.CreateSmsSession;
import org.mobicents.servlet.restcomm.sms.DestroySmsSession;
import org.mobicents.servlet.restcomm.sms.SmsServiceResponse;
import org.mobicents.servlet.restcomm.sms.SmsSessionAttribute;
import org.mobicents.servlet.restcomm.sms.SmsSessionInfo;
import org.mobicents.servlet.restcomm.sms.SmsSessionRequest;
import org.mobicents.servlet.restcomm.sms.SmsSessionResponse;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Collect;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.Hangup;
import org.mobicents.servlet.restcomm.telephony.MediaGroupResponse;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.telephony.Play;
import org.mobicents.servlet.restcomm.telephony.Record;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;
import org.mobicents.servlet.restcomm.util.UriUtils;
import org.mobicents.servlet.restcomm.util.WavUtils;

import scala.concurrent.duration.Duration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 * @author gvagenas@telestax.com
 * @author pavel.slegr@telestax.com
 */
public abstract class BaseVoiceInterpreter extends UntypedActor {
    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;
    static final Pattern PATTERN = Pattern.compile("[\\*#0-9]{1,12}");
    static final String EMAIL_SENDER = "restcomm@restcomm.org";
    static final String EMAIL_SUBJECT = "RestComm Error Notification - Attention Required";

    // States for the FSM.
    // ==========================
    final State uninitialized;
    final State acquiringAsrInfo;
    final State acquiringSynthesizerInfo;
    final State acquiringCallInfo;
    final State playingRejectionPrompt;
    final State pausing;
    final State caching;
    final State checkingCache;
    final State playing;
    final State synthesizing;
    final State redirecting;
    final State faxing;
    final State processingGatherChildren;
    final State gathering;
    final State finishGathering;
    final State creatingRecording;
    final State finishRecording;
    final State creatingSmsSession;
    final State sendingSms;
    final State hangingUp;
    // final State finished;

    // FSM.
    FiniteStateMachine fsm = null;
    // The user specific configuration.
    Configuration configuration = null;
    // The block storage cache.
    ActorRef cache = null;
    String cachePath = null;
    // The downloader will fetch resources for us using HTTP.
    ActorRef downloader = null;
    // The mail man that will deliver e-mail.
    ActorRef mailer = null;
    // The call manager.
    ActorRef callManager = null;
    // The conference manager.
    ActorRef conferenceManager = null;
    // The automatic speech recognition service.
    ActorRef asrService = null;
    int outstandingAsrRequests;
    // The fax service.
    ActorRef faxService = null;
    // The SMS service = null.
    ActorRef smsService = null;
    Map<Sid, ActorRef> smsSessions = null;
    // The storage engine.
    DaoManager storage = null;
    // The text to speech synthesizer service.
    ActorRef synthesizer = null;
    // The languages supported by the automatic speech recognition service.
    AsrInfo asrInfo = null;
    // The languages supported by the text to speech synthesizer service.
    SpeechSynthesizerInfo synthesizerInfo = null;
    // The call being handled by this interpreter.
    ActorRef call = null;
    ActorRef callMediaGroup = null;
    // The information for this call.
    CallInfo callInfo = null;
    // The call state.
    CallStateChanged.State callState = null;
    // A call detail record.
    CallDetailRecord callRecord = null;

    // State for outbound calls.
    ActorRef outboundCall = null;
    CallInfo outboundCallInfo = null;

    // State for the gather verb.
    List<Tag> gatherChildren = null;
    List<URI> gatherPrompts = null;
    // The call recording stuff.
    Sid recordingSid = null;
    URI recordingUri = null;
    URI publicRecordingUri = null;
    // Information to reach the application that will be executed
    // by this interpreter.
    Sid accountId;
    Sid phoneId;
    String version;
    URI url;
    String method;
    URI fallbackUrl;
    String fallbackMethod;
    URI statusCallback;
    String statusCallbackMethod;
    String emailAddress;
    // application data.
    HttpRequestDescriptor request;
    HttpResponseDescriptor response;
    // The RCML parser.
    ActorRef parser;
    Tag verb;
    Tag gatherVerb;
    Boolean processingGather = false;

    final Set<Transition> transitions = new HashSet<Transition>();

    public BaseVoiceInterpreter() {
        super();
        final ActorRef source = self();
        // 20 States in common
        uninitialized = new State("uninitialized", null, null);
        acquiringAsrInfo = new State("acquiring asr info", new AcquiringAsrInfo(source), null);
        acquiringSynthesizerInfo = new State("acquiring tts info", new AcquiringSpeechSynthesizerInfo(source), null);
        acquiringCallInfo = new State("acquiring call info", new AcquiringCallInfo(source), null);
        playingRejectionPrompt = new State("playing rejection prompt", new PlayingRejectionPrompt(source), null);
        pausing = new State("pausing", new Pausing(source), null);
        caching = new State("caching", new Caching(source), null);
        checkingCache = new State("checkingCache", new CheckCache(source), null);
        playing = new State("playing", new Playing(source), null);
        synthesizing = new State("synthesizing", new Synthesizing(source), null);
        redirecting = new State("redirecting", new Redirecting(source), null);
        faxing = new State("faxing", new Faxing(source), null);
        gathering = new State("gathering", new Gathering(source), null);
        processingGatherChildren = new State("processing gather children", new ProcessingGatherChildren(source), null);
        finishGathering = new State("finish gathering", new FinishGathering(source), null);
        creatingRecording = new State("creating recording", new CreatingRecording(source), null);
        finishRecording = new State("finish recording", new FinishRecording(source), null);
        creatingSmsSession = new State("creating sms session", new CreatingSmsSession(source), null);
        sendingSms = new State("sending sms", new SendingSms(source), null);
        hangingUp = new State("hanging up", new HangingUp(source), null);

        // Initialize the transitions for the FSM.
        transitions.add(new Transition(uninitialized, acquiringAsrInfo));
        transitions.add(new Transition(acquiringAsrInfo, acquiringSynthesizerInfo));
        transitions.add(new Transition(acquiringSynthesizerInfo, acquiringCallInfo));
        transitions.add(new Transition(pausing, hangingUp));
        transitions.add(new Transition(playingRejectionPrompt, hangingUp));
        transitions.add(new Transition(faxing, faxing));
        transitions.add(new Transition(faxing, caching));
        transitions.add(new Transition(faxing, pausing));
        transitions.add(new Transition(faxing, redirecting));
        transitions.add(new Transition(faxing, synthesizing));
        transitions.add(new Transition(faxing, processingGatherChildren));
        transitions.add(new Transition(faxing, creatingRecording));
        transitions.add(new Transition(faxing, creatingSmsSession));
        transitions.add(new Transition(faxing, hangingUp));
        transitions.add(new Transition(caching, faxing));
        transitions.add(new Transition(caching, playing));
        transitions.add(new Transition(caching, caching));
        transitions.add(new Transition(caching, pausing));
        transitions.add(new Transition(caching, redirecting));
        transitions.add(new Transition(caching, synthesizing));
        transitions.add(new Transition(caching, processingGatherChildren));
        transitions.add(new Transition(caching, creatingRecording));
        transitions.add(new Transition(caching, creatingSmsSession));
        transitions.add(new Transition(caching, hangingUp));
        transitions.add(new Transition(checkingCache, synthesizing));
        transitions.add(new Transition(checkingCache, playing));
        transitions.add(new Transition(checkingCache, checkingCache));
        transitions.add(new Transition(playing, hangingUp));
        transitions.add(new Transition(synthesizing, faxing));
        transitions.add(new Transition(synthesizing, pausing));
        transitions.add(new Transition(synthesizing, checkingCache));
        transitions.add(new Transition(synthesizing, caching));
        transitions.add(new Transition(synthesizing, redirecting));
        transitions.add(new Transition(synthesizing, processingGatherChildren));
        transitions.add(new Transition(synthesizing, creatingRecording));
        transitions.add(new Transition(synthesizing, creatingSmsSession));
        transitions.add(new Transition(synthesizing, synthesizing));
        transitions.add(new Transition(synthesizing, hangingUp));
        transitions.add(new Transition(redirecting, faxing));
        transitions.add(new Transition(redirecting, pausing));
        transitions.add(new Transition(redirecting, checkingCache));
        transitions.add(new Transition(redirecting, caching));
        transitions.add(new Transition(redirecting, synthesizing));
        transitions.add(new Transition(redirecting, redirecting));
        transitions.add(new Transition(redirecting, processingGatherChildren));
        transitions.add(new Transition(redirecting, creatingRecording));
        transitions.add(new Transition(redirecting, creatingSmsSession));
        transitions.add(new Transition(redirecting, hangingUp));
        transitions.add(new Transition(creatingRecording, finishRecording));
        transitions.add(new Transition(creatingRecording, hangingUp));
        transitions.add(new Transition(finishRecording, faxing));
        transitions.add(new Transition(finishRecording, pausing));
        transitions.add(new Transition(finishRecording, checkingCache));
        transitions.add(new Transition(finishRecording, caching));
        transitions.add(new Transition(finishRecording, synthesizing));
        transitions.add(new Transition(finishRecording, redirecting));
        transitions.add(new Transition(finishRecording, processingGatherChildren));
        transitions.add(new Transition(finishRecording, creatingRecording));
        transitions.add(new Transition(finishRecording, creatingSmsSession));
        transitions.add(new Transition(finishRecording, hangingUp));
        transitions.add(new Transition(processingGatherChildren, processingGatherChildren));
        transitions.add(new Transition(processingGatherChildren, gathering));
        transitions.add(new Transition(processingGatherChildren, synthesizing));
        transitions.add(new Transition(processingGatherChildren, hangingUp));
        transitions.add(new Transition(gathering, finishGathering));
        transitions.add(new Transition(gathering, hangingUp));
        transitions.add(new Transition(finishGathering, faxing));
        transitions.add(new Transition(finishGathering, pausing));
        transitions.add(new Transition(finishGathering, checkingCache));
        transitions.add(new Transition(finishGathering, caching));
        transitions.add(new Transition(finishGathering, synthesizing));
        transitions.add(new Transition(finishGathering, redirecting));
        transitions.add(new Transition(finishGathering, processingGatherChildren));
        transitions.add(new Transition(finishGathering, creatingRecording));
        transitions.add(new Transition(finishGathering, creatingSmsSession));
        transitions.add(new Transition(finishGathering, hangingUp));
        transitions.add(new Transition(creatingSmsSession, sendingSms));
        transitions.add(new Transition(creatingSmsSession, hangingUp));
        transitions.add(new Transition(sendingSms, faxing));
        transitions.add(new Transition(sendingSms, pausing));
        transitions.add(new Transition(sendingSms, caching));
        transitions.add(new Transition(sendingSms, synthesizing));
        transitions.add(new Transition(sendingSms, redirecting));
        transitions.add(new Transition(sendingSms, processingGatherChildren));
        transitions.add(new Transition(sendingSms, creatingRecording));
        transitions.add(new Transition(sendingSms, creatingSmsSession));
        transitions.add(new Transition(sendingSms, hangingUp));
    }

    @Override
    public abstract void onReceive(Object arg0) throws Exception;

    abstract List<NameValuePair> parameters();

    ActorRef asr(final Configuration configuration) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new ISpeechAsr(configuration);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    void asrResponse(final Object message) {
        final Class<?> klass = message.getClass();
        if (AsrResponse.class.equals(klass)) {
            final AsrResponse<String> response = (AsrResponse<String>) message;
            Transcription transcription = (Transcription) response.attributes().get("transcription");
            if (response.succeeded()) {
                transcription = transcription.setStatus(Transcription.Status.COMPLETED);
                transcription = transcription.setTranscriptionText(response.get());
            } else {
                transcription = transcription.setStatus(Transcription.Status.FAILED);
            }
            final TranscriptionsDao transcriptions = storage.getTranscriptionsDao();
            transcriptions.updateTranscription(transcription);
            // Notify the callback listener.
            final Object attribute = response.attributes().get("callback");
            if (attribute != null) {
                final URI callback = (URI) attribute;
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(callback, "POST", parameters);
                downloader.tell(request, null);
            }
            // Update pending asr responses.
            outstandingAsrRequests--;
            // Try to stop the interpreter.
            postCleanup();
        }
    }

    ActorRef fax(final Configuration configuration) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new InterfaxService(configuration);
            }
        }));
    }

    void callback() {
        if (statusCallback != null) {
            if (statusCallbackMethod == null) {
                statusCallbackMethod = "POST";
            }
            final List<NameValuePair> parameters = parameters();
            request = new HttpRequestDescriptor(statusCallback, statusCallbackMethod, parameters);
            downloader.tell(request, null);
        }
    }

    ActorRef cache(final String path, final String uri) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new DiskCache(path, uri, true);
            }
        }));
    }

    ActorRef downloader() {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Downloader();
            }
        }));
    }

    String e164(final String number) {
        if (configuration.subset("runtime-settings").getBoolean("normalize-numbers-for-outbound-calls")) {
            final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
            try {
                final PhoneNumber result = numbersUtil.parse(number, "US");
                return numbersUtil.format(result, PhoneNumberFormat.E164);
            } catch (final NumberParseException ignored) {
                return number;
            }
        } else {
            return number;
        }
    }

    void invalidVerb(final Tag verb) {
        final ActorRef self = self();
        // Get the next verb.
        final GetNextVerb next = GetNextVerb.instance();
        parser.tell(next, self);
    }

    ActorRef mailer(final Configuration configuration) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MailMan(configuration);
            }
        }));
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

    ActorRef parser(final String xml) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Parser(xml);
            }
        }));
    }

    void postCleanup() {
        final ActorRef self = self();
        if (smsSessions.isEmpty() && outstandingAsrRequests == 0) {
            final UntypedActorContext context = getContext();
            context.stop(self());
        }
    }

    void sendMail(final Notification notification) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<strong>").append("Sid: ").append("</strong></br>");
        buffer.append(notification.getSid().toString()).append("</br>");
        buffer.append("<strong>").append("Account Sid: ").append("</strong></br>");
        buffer.append(notification.getAccountSid().toString()).append("</br>");
        buffer.append("<strong>").append("Call Sid: ").append("</strong></br>");
        buffer.append(notification.getCallSid().toString()).append("</br>");
        buffer.append("<strong>").append("API Version: ").append("</strong></br>");
        buffer.append(notification.getApiVersion()).append("</br>");
        buffer.append("<strong>").append("Log: ").append("</strong></br>");
        buffer.append(notification.getLog() == ERROR_NOTIFICATION ? "ERROR" : "WARNING").append("</br>");
        buffer.append("<strong>").append("Error Code: ").append("</strong></br>");
        buffer.append(notification.getErrorCode()).append("</br>");
        buffer.append("<strong>").append("More Information: ").append("</strong></br>");
        buffer.append(notification.getMoreInfo().toString()).append("</br>");
        buffer.append("<strong>").append("Message Text: ").append("</strong></br>");
        buffer.append(notification.getMessageText()).append("</br>");
        buffer.append("<strong>").append("Message Date: ").append("</strong></br>");
        buffer.append(notification.getMessageDate().toString()).append("</br>");
        buffer.append("<strong>").append("Request URL: ").append("</strong></br>");
        buffer.append(notification.getRequestUrl().toString()).append("</br>");
        buffer.append("<strong>").append("Request Method: ").append("</strong></br>");
        buffer.append(notification.getRequestMethod()).append("</br>");
        buffer.append("<strong>").append("Request Variables: ").append("</strong></br>");
        buffer.append(notification.getRequestVariables()).append("</br>");
        buffer.append("<strong>").append("Response Headers: ").append("</strong></br>");
        buffer.append(notification.getResponseHeaders()).append("</br>");
        buffer.append("<strong>").append("Response Body: ").append("</strong></br>");
        buffer.append(notification.getResponseBody()).append("</br>");
        final Mail email = new Mail(EMAIL_SENDER, emailAddress, EMAIL_SUBJECT, buffer.toString());
        mailer.tell(email, self());
    }

    void smsResponse(final Object message) {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        if (SmsSessionResponse.class.equals(klass)) {
            final SmsSessionResponse response = (SmsSessionResponse) message;
            final SmsSessionInfo info = response.info();
            SmsMessage record = (SmsMessage) info.attributes().get("record");
            if (response.succeeded()) {
                final DateTime now = DateTime.now();
                record = record.setDateSent(now);
                record = record.setStatus(Status.SENT);
            } else {
                record = record.setStatus(Status.FAILED);
            }
            final SmsMessagesDao messages = storage.getSmsMessagesDao();
            messages.updateSmsMessage(record);
            // Notify the callback listener.
            final Object attribute = info.attributes().get("callback");
            if (attribute != null) {
                final URI callback = (URI) attribute;
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(callback, "POST", parameters);
                downloader.tell(request, null);
            }
            // Destroy the sms session.
            final ActorRef session = smsSessions.remove(record.getSid());
            final DestroySmsSession destroy = new DestroySmsSession(session);
            smsService.tell(destroy, self);
            // Try to stop the interpreter.
            postCleanup();
        }
    }

    ActorRef tts(final Configuration configuration) {
        final String classpath = configuration.getString("[@class]");

        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return (UntypedActor) Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
            }
        }));
    }

    abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    final class AcquiringAsrInfo extends AbstractAction {
        public AcquiringAsrInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final StartInterpreter request = (StartInterpreter) message;
            call = request.resource();
            asrService.tell(new GetAsrInfo(), source);
        }
    }

    final class AcquiringSpeechSynthesizerInfo extends AbstractAction {
        public AcquiringSpeechSynthesizerInfo(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public void execute(final Object message) throws Exception {
            final AsrResponse<AsrInfo> response = (AsrResponse<AsrInfo>) message;
            asrInfo = response.get();
            synthesizer.tell(new GetSpeechSynthesizerInfo(), source);
        }
    }

    final class AcquiringCallInfo extends AbstractAction {
        public AcquiringCallInfo(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public void execute(final Object message) throws Exception {
            final SpeechSynthesizerResponse<SpeechSynthesizerInfo> response = (SpeechSynthesizerResponse<SpeechSynthesizerInfo>) message;
            synthesizerInfo = response.get();
            call.tell(new Observe(source), source);
            call.tell(new GetCallInfo(), source);
        }
    }

    final class NotFound extends AbstractAction {
        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // final Class<?> klass = message.getClass();
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

    final class Rejecting extends AbstractAction {
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

    final class PlayingRejectionPrompt extends AbstractAction {
        public PlayingRejectionPrompt(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            String path = configuration.subset("runtime-settings").getString("prompts-uri");
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += "reject.wav";
            URI uri = null;
            try {
                uri = URI.create(path);
            } catch (final Exception exception) {
                final Notification notification = notification(ERROR_NOTIFICATION, 12400, exception.getMessage());
                final NotificationsDao notifications = storage.getNotificationsDao();
                notifications.addNotification(notification);
                sendMail(notification);
                final StopInterpreter stop = StopInterpreter.instance();
                source.tell(stop, source);
                return;
            }
            final Play play = new Play(uri, 1);
            callMediaGroup.tell(play, source);
        }
    }

    final class Faxing extends AbstractAction {
        public Faxing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            final DiskCacheResponse response = (DiskCacheResponse) message;
            // Parse "from".
            String from = callInfo.to();
            Attribute attribute = verb.attribute("from");
            if (attribute != null) {
                from = attribute.value();
                if (from != null && from.isEmpty()) {
                    from = e164(from);
                    if (from == null) {
                        from = verb.attribute("from").value();
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                }
            }
            // Parse "to".
            String to = callInfo.from();
            attribute = verb.attribute("to");
            if (attribute != null) {
                to = attribute.value();
                if (to != null && !to.isEmpty()) {
                    to = e164(to);
                    if (to == null) {
                        to = verb.attribute("to").value();
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                }
            }
            // Send the fax.
            final String uri = response.get().toString();
            final int offset = uri.lastIndexOf("/");
            final String path = cachePath + "/" + uri.substring(offset + 1, uri.length());
            final FaxRequest fax = new FaxRequest(to, new File(path));
            faxService.tell(fax, source);
        }
    }

    final class Pausing extends AbstractAction {
        public Pausing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            int length = 1;
            final Attribute attribute = verb.attribute("length");
            if (attribute != null) {
                final String number = attribute.value();
                if (number != null && !number.isEmpty()) {
                    try {
                        length = Integer.parseInt(number);
                    } catch (final NumberFormatException exception) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13910, "Invalid length value.");
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        notifications.addNotification(notification);
                    }
                }
            }
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.create(length, TimeUnit.SECONDS));
        }
    }

    final class CheckCache extends AbstractAction {
        public CheckCache(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            // else {
            // logger.info("Can't check cache, message not verb. Moving to the next verb");
            // // final GetNextVerb next = GetNextVerb.instance();
            // // parser.tell(next, source);
            // return;
            // }
            String hash = hash(verb);
            DiskCacheRequest request = new DiskCacheRequest(hash);
            if (logger.isErrorEnabled()) {
                logger.info("Checking cache for hash: " + hash);
            }
            cache.tell(request, source);
        }
    }

    final class Caching extends AbstractAction {
        public Caching(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (SpeechSynthesizerResponse.class.equals(klass)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                final DiskCacheRequest request = new DiskCacheRequest(response.get());
                cache.tell(request, source);
            } else if (Tag.class.equals(klass) || MediaGroupStateChanged.class.equals(klass)) {
                if (Tag.class.equals(klass)) {
                    verb = (Tag) message;
                }
                // Parse the URL.
                final String text = verb.text();
                if (text != null && !text.isEmpty()) {
                    // Try to cache the media.
                    URI target = null;
                    try {
                        target = URI.create(text);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, text + " is an invalid URI.");
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                    final URI base = request.getUri();
                    final URI uri = UriUtils.resolve(base, target);
                    final DiskCacheRequest request = new DiskCacheRequest(uri);
                    cache.tell(request, source);
                } else {
                    // Ask the parser for the next action to take.
                    final GetNextVerb next = GetNextVerb.instance();
                    parser.tell(next, source);
                }
            }
        }
    }

    final class Playing extends AbstractAction {
        public Playing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (DiskCacheResponse.class.equals(klass)) {
                // Parse the loop attribute.
                int loop = 1;
                final Attribute attribute = verb.attribute("loop");
                if (attribute != null) {
                    final String number = attribute.value();
                    if (number != null && !number.isEmpty()) {
                        try {
                            loop = Integer.parseInt(number);
                        } catch (final NumberFormatException ignored) {
                            final NotificationsDao notifications = storage.getNotificationsDao();
                            Notification notification = null;
                            if (say.equals(verb.name())) {
                                notification = notification(WARNING_NOTIFICATION, 13510, loop + " is an invalid loop value.");
                                notifications.addNotification(notification);
                            } else if (play.equals(verb.name())) {
                                notification = notification(WARNING_NOTIFICATION, 13410, loop + " is an invalid loop value.");
                                notifications.addNotification(notification);
                            }
                        }
                    }
                }
                final DiskCacheResponse response = (DiskCacheResponse) message;
                final Play play = new Play(response.get(), loop);
                callMediaGroup.tell(play, source);
            }
        }
    }

    String hash(Object message) {
        Map<String, String> details = getSynthesizeDetails(message);
        if (details == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Cannot generate hash, details are null");
            }
            return null;
        }
        String voice = details.get("voice");
        String language = details.get("language");
        String text = details.get("text");
        return HashGenerator.hashMessage(voice, language, text);
    }

    Map<String, String> getSynthesizeDetails(final Object message) {
        final Class<?> klass = message.getClass();

        Map<String, String> details = new HashMap<String, String>();

        if (Tag.class.equals(klass)) {
            verb = (Tag) message;
        } else {
            return null;
        }
        if (!say.equals(verb.name()))
            return null;

        // Parse the voice attribute.
        String voice = "man";
        Attribute attribute = verb.attribute("voice");
        if (attribute != null) {
            voice = attribute.value();
            if (voice != null && !voice.isEmpty()) {
                if (!"man".equals(voice) && !"woman".equals(voice)) {
                    final Notification notification = notification(WARNING_NOTIFICATION, 13511, voice
                            + " is an invalid voice value.");
                    final NotificationsDao notifications = storage.getNotificationsDao();
                    notifications.addNotification(notification);
                    voice = "man";
                }
            } else {
                voice = "man";
            }
        }
        // Parse the language attribute.
        String language = "en";
        attribute = verb.attribute("language");
        if (attribute != null) {
            language = attribute.value();
            if (language != null && !language.isEmpty()) {
                if (!synthesizerInfo.languages().contains(language)) {
                    language = "en";
                }
            } else {
                language = "en";
            }
        }
        // Synthesize.
        String text = verb.text();

        details.put("voice", voice);
        details.put("language", language);
        details.put("text", text);

        return details;

    }

    final class Synthesizing extends AbstractAction {
        public Synthesizing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {

            final Class<?> klass = message.getClass();

            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }

            Map<String, String> details = getSynthesizeDetails(verb);
            if (details != null && !details.isEmpty() && details.get("text") != null) {
                String voice = details.get("voice");
                String language = details.get("language");
                String text = details.get("text");
                final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(voice, language, text);
                synthesizer.tell(synthesize, source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    final class HangingUp extends AbstractAction {
        public HangingUp(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            // Hang up the call.
            call.tell(new Hangup(), source);
        }
    }

    final class Redirecting extends AbstractAction {
        public Redirecting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            final NotificationsDao notifications = storage.getNotificationsDao();
            String method = "POST";
            Attribute attribute = verb.attribute("method");
            if (attribute != null) {
                method = attribute.value();
                if (method != null && !method.isEmpty()) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13710, method
                                + " is not a valid HTTP method for <Redirect>");
                        notifications.addNotification(notification);
                        method = "POST";
                    }
                } else {
                    method = "POST";
                }
            }
            final String text = verb.text();
            if (text != null && !text.isEmpty()) {
                // Try to redirect.
                URI target = null;
                try {
                    target = URI.create(text);
                } catch (final Exception exception) {
                    final Notification notification = notification(ERROR_NOTIFICATION, 11100, text + " is an invalid URI.");
                    notifications.addNotification(notification);
                    sendMail(notification);
                    final StopInterpreter stop = StopInterpreter.instance();
                    source.tell(stop, source);
                    return;
                }
                final URI base = request.getUri();
                final URI uri = UriUtils.resolve(base, target);
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(uri, method, parameters);
                downloader.tell(request, source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    abstract class AbstractGatherAction extends AbstractAction {
        public AbstractGatherAction(final ActorRef source) {
            super(source);
        }

        protected String finishOnKey(final Tag container) {
            String finishOnKey = "#";
            Attribute attribute = container.attribute("finishOnKey");
            if (attribute != null) {
                finishOnKey = attribute.value();
                if (finishOnKey != null && !finishOnKey.isEmpty()) {
                    if (!PATTERN.matcher(finishOnKey).matches()) {
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        final Notification notification = notification(WARNING_NOTIFICATION, 13310, finishOnKey
                                + " is not a valid finishOnKey value");
                        notifications.addNotification(notification);
                        finishOnKey = "#";
                    }
                } else {
                    finishOnKey = "#";
                }
            }
            return finishOnKey;
        }
    }

    final class ProcessingGatherChildren extends AbstractGatherAction {
        public ProcessingGatherChildren(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            processingGather = true;
            final Class<?> klass = message.getClass();
            final NotificationsDao notifications = storage.getNotificationsDao();
            if (SpeechSynthesizerResponse.class.equals(klass)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                final DiskCacheRequest request = new DiskCacheRequest(response.get());
                cache.tell(request, source);
            } else {
                if (Tag.class.equals(klass)) {
                    verb = (Tag) message;
                    gatherPrompts = new ArrayList<URI>();
                    gatherChildren = new ArrayList<Tag>(verb.children());
                } else if (MediaGroupStateChanged.class.equals(klass)) {
                    gatherPrompts = new ArrayList<URI>();
                    gatherChildren = new ArrayList<Tag>(verb.children());
                } else if (DiskCacheResponse.class.equals(klass)) {
                    if (gatherPrompts == null)
                        gatherPrompts = new ArrayList<URI>();
                    if (gatherChildren == null)
                        gatherChildren = new ArrayList<Tag>(verb.children());
                    final DiskCacheResponse response = (DiskCacheResponse) message;
                    final URI uri = response.get();
                    Tag child = null;
                    if (!gatherChildren.isEmpty())
                        child = gatherChildren.remove(0);
                    // Parse the loop attribute.
                    int loop = 1;
                    Attribute attribute = null;
                    if (child != null)
                        attribute = child.attribute("loop");
                    if (attribute != null) {
                        final String number = attribute.value();
                        if (number != null && !number.isEmpty()) {
                            try {
                                loop = Integer.parseInt(number);
                            } catch (final NumberFormatException ignored) {
                                Notification notification = null;
                                if (say.equals(child.name())) {
                                    notification = notification(WARNING_NOTIFICATION, 13322, loop
                                            + " is an invalid loop value.");
                                    notifications.addNotification(notification);
                                }
                            }
                        }
                    }
                    for (int counter = 0; counter < loop; counter++) {
                        gatherPrompts.add(uri);
                    }
                }
                for (int index = 0; index < gatherChildren.size(); index++) {
                    final Tag child = gatherChildren.get(index);
                    if (play.equals(child.name())) {
                        final String text = child.text();
                        if (text != null && !text.isEmpty()) {
                            URI target = null;
                            try {
                                target = URI.create(text);
                            } catch (final Exception exception) {
                                final Notification notification = notification(ERROR_NOTIFICATION, 13325, text
                                        + " is an invalid URI.");
                                notifications.addNotification(notification);
                                sendMail(notification);
                                final StopInterpreter stop = StopInterpreter.instance();
                                source.tell(stop, source);
                                return;
                            }
                            final URI base = request.getUri();
                            final URI uri = UriUtils.resolve(base, target);
                            // Cache the prompt.
                            final DiskCacheRequest request = new DiskCacheRequest(uri);
                            cache.tell(request, source);
                            break;
                        }
                    } else if (say.equals(child.name())) {
                        // Parse the voice attribute.
                        String voice = "man";
                        Attribute attribute = child.attribute("voice");
                        if (attribute != null) {
                            voice = attribute.value();
                            if (voice != null && !voice.isEmpty()) {
                                if (!"man".equals(voice) && !"woman".equals(voice)) {
                                    final Notification notification = notification(WARNING_NOTIFICATION, 13321, voice
                                            + " is an invalid voice value.");
                                    notifications.addNotification(notification);
                                    voice = "man";
                                }
                            } else {
                                voice = "man";
                            }
                        }
                        // Parse the language attribute.
                        String language = "en";
                        attribute = child.attribute("language");
                        if (attribute != null) {
                            language = attribute.value();
                            if (language != null && !language.isEmpty()) {
                                if (!synthesizerInfo.languages().contains(language)) {
                                    language = "en";
                                }
                            } else {
                                language = "en";
                            }
                        }
                        String text = child.text();
                        if (text != null && !text.isEmpty()) {
//                            final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(voice, language, text);
//                            synthesizer.tell(synthesize, source);
//                            break;
                            String hash = hash(child);
                            DiskCacheRequest request = new DiskCacheRequest(hash);
                            cache.tell(request, source);
                            break;
                        }
                    } else if (pause.equals(child.name())) {
                        int length = 1;
                        final Attribute attribute = child.attribute("length");
                        if (attribute != null) {
                            final String number = attribute.value();
                            if (number != null && !number.isEmpty()) {
                                try {
                                    length = Integer.parseInt(number);
                                } catch (final NumberFormatException ignored) {
                                }
                            }
                        }
                        String path = configuration.subset("runtime-settings").getString("prompts-uri");
                        if (!path.endsWith("/")) {
                            path += "/";
                        }
                        path += "one-second-silence.wav";
                        final URI uri = URI.create(path);
                        for (int counter = 0; counter < length; counter++) {
                            gatherPrompts.add(uri);
                        }
                    }
                }
                // Make sure we don't leave any pauses at the beginning
                // since we can't concurrently modify the list.
                if (!gatherChildren.isEmpty()) {
                    Tag child = null;
                    do {
                        child = gatherChildren.get(0);
                        if (child != null) {
                            if (pause.equals(child.name())) {
                                gatherChildren.remove(0);
                            }
                        }
                    } while (pause.equals(child.name()));
                }
                // Start gathering.
                if (gatherChildren.isEmpty()) {
                    if (gatherVerb != null)
                        verb = gatherVerb;
                    final StartGathering start = StartGathering.instance();
                    source.tell(start, source);
                }
            }
        }
    }

    final class Gathering extends AbstractGatherAction {
        public Gathering(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final NotificationsDao notifications = storage.getNotificationsDao();
            // Parse finish on key.
            String finishOnKey = finishOnKey(verb);
            // Parse the number of digits.
            int numberOfDigits = Short.MAX_VALUE;
            Attribute attribute = verb.attribute("numDigits");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        numberOfDigits = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13314, numberOfDigits
                                + " is not a valid numDigits value");
                        notifications.addNotification(notification);
                    }
                }
            }
            // Parse timeout.
            int timeout = 5;
            attribute = verb.attribute("timeout");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        timeout = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13313, timeout
                                + " is not a valid timeout value");
                        notifications.addNotification(notification);
                    }
                }
            }
            // Start gathering.
            final Collect collect = new Collect(gatherPrompts, null, timeout, finishOnKey, numberOfDigits);
            callMediaGroup.tell(collect, source);
            // Some clean up.
            gatherChildren = null;
            gatherPrompts = null;
        }
    }

    final class FinishGathering extends AbstractGatherAction {
        public FinishGathering(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final NotificationsDao notifications = storage.getNotificationsDao();
            final MediaGroupResponse<String> response = (MediaGroupResponse<String>) message;
            // Parses "action".
            Attribute attribute = verb.attribute("action");
            String digits = response.get();
            final String finishOnKey = finishOnKey(verb);
            if (digits.equals(finishOnKey)) {
                digits = "";
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Digits collected : " + digits);
            }
            // https://bitbucket.org/telestax/telscale-restcomm/issue/150/verb-is-looping-by-default-and-never
            // If the 'timeout' is reached before the caller enters any digits, or if the caller enters the 'finishOnKey' value
            // before entering any other digits, Twilio will not make a request to the 'action' URL but instead continue
            // processing
            // the current TwiML document with the verb immediately following the <Gather>
            if (attribute != null && (digits != null && !digits.trim().isEmpty())) {
                String action = attribute.value();
                if (action != null && !action.isEmpty()) {
                    URI target = null;
                    try {
                        target = URI.create(action);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, action
                                + " is an invalid URI.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                    final URI base = request.getUri();
                    final URI uri = UriUtils.resolve(base, target);
                    // Parse "method".
                    String method = "POST";
                    attribute = verb.attribute("method");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 14104, method
                                        + " is not a valid HTTP method for <Gather>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Redirect to the action url.
                    if (digits.endsWith(finishOnKey)) {
                        final int finishOnKeyIndex = digits.lastIndexOf(finishOnKey);
                        digits = digits.substring(0, finishOnKeyIndex);
                    }
                    final List<NameValuePair> parameters = parameters();
                    parameters.add(new BasicNameValuePair("Digits", digits));
                    request = new HttpRequestDescriptor(uri, method, parameters);
                    downloader.tell(request, source);
                    return;
                }
            }
            // Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
        }
    }

    final class CreatingRecording extends AbstractAction {
        public CreatingRecording(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            final NotificationsDao notifications = storage.getNotificationsDao();
            String finishOnKey = "1234567890*#";
            Attribute attribute = verb.attribute("finishOnKey");
            if (attribute != null) {
                finishOnKey = attribute.value();
                if (finishOnKey != null && !finishOnKey.isEmpty()) {
                    if (!PATTERN.matcher(finishOnKey).matches()) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13613, finishOnKey
                                + " is not a valid finishOnKey value");
                        notifications.addNotification(notification);
                        finishOnKey = "1234567890*#";
                    }
                } else {
                    finishOnKey = "1234567890*#";
                }
            }
            boolean playBeep = true;
            attribute = verb.attribute("playBeep");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    playBeep = Boolean.parseBoolean(value);
                }
            }
            int maxLength = 3600;
            attribute = verb.attribute("maxLength");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        maxLength = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13612, maxLength
                                + " is not a valid maxLength value");
                        notifications.addNotification(notification);
                    }
                }
            }
            int timeout = 5;
            attribute = verb.attribute("timeout");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        timeout = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13612, timeout
                                + " is not a valid timeout value");
                        notifications.addNotification(notification);
                    }
                }
            }
            // Start recording.
            recordingSid = Sid.generate(Sid.Type.RECORDING);
            String path = configuration.subset("runtime-settings").getString("recordings-path");
            String httpRecordingUri = configuration.subset("runtime-settings").getString("recordings-uri");
            if (!path.endsWith("/")) {
                path += "/";
            }
            if (!httpRecordingUri.endsWith("/")) {
                httpRecordingUri += "/";
            }
            path += recordingSid.toString() + ".wav";
            httpRecordingUri += recordingSid.toString() + ".wav";
            recordingUri = URI.create(path);
            publicRecordingUri = URI.create(httpRecordingUri);
            Record record = null;
            if (playBeep) {
                final List<URI> prompts = new ArrayList<URI>(1);
                path = configuration.subset("runtime-settings").getString("prompts-uri");
                if (!path.endsWith("/")) {
                    path += "/";
                }
                path += "beep.wav";
                try {
                    prompts.add(URI.create(path));
                } catch (final Exception exception) {
                    final Notification notification = notification(ERROR_NOTIFICATION, 12400, exception.getMessage());
                    notifications.addNotification(notification);
                    sendMail(notification);
                    final StopInterpreter stop = StopInterpreter.instance();
                    source.tell(stop, source);
                    return;
                }
                record = new Record(recordingUri, prompts, timeout, maxLength, finishOnKey);
            } else {
                record = new Record(recordingUri, timeout, maxLength, finishOnKey);
            }
            callMediaGroup.tell(record, source);
        }
    }

    final class FinishRecording extends AbstractAction {
        public FinishRecording(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallStateChanged.class.equals(klass)) {
                final CallStateChanged event = (CallStateChanged) message;
                // Update the interpreter state.
                callState = event.state();
                // Update the storage.
                callRecord = callRecord.setStatus(callState.toString());
                final DateTime end = DateTime.now();
                callRecord = callRecord.setEndTime(end);
                final int seconds = (int) (end.getMillis() - callRecord.getStartTime().getMillis()) / 1000;
                callRecord = callRecord.setDuration(seconds);
                final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
                records.updateCallDetailRecord(callRecord);
                // Update the application.
                callback();
            }
            final NotificationsDao notifications = storage.getNotificationsDao();
            // Create a record of the recording.
            Double duration = WavUtils.getAudioDuration(recordingUri);
            if(duration.equals(0.0)) {
                final DateTime end = DateTime.now();
                duration = new Double((end.getMillis() - callRecord.getStartTime().getMillis()) / 1000);
            }
            final Recording.Builder builder = Recording.builder();
            builder.setSid(recordingSid);
            builder.setAccountSid(accountId);
            builder.setCallSid(callInfo.sid());
            builder.setDuration(duration);
            builder.setApiVersion(version);
            StringBuilder buffer = new StringBuilder();
            buffer.append("/").append(version).append("/Accounts/").append(accountId.toString());
            buffer.append("/Recordings/").append(recordingSid.toString());
            builder.setUri(URI.create(buffer.toString()));
            final Recording recording = builder.build();
            final RecordingsDao recordings = storage.getRecordingsDao();
            recordings.addRecording(recording);
            // Start transcription.
            URI transcribeCallback = null;
            Attribute attribute = verb.attribute("transcribeCallback");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        transcribeCallback = URI.create(value);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, transcribeCallback
                                + " is an invalid URI.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                }
            }
            boolean transcribe = false;
            if (transcribeCallback != null) {
                transcribe = true;
            } else {
                attribute = verb.attribute("transcribe");
                if (attribute != null) {
                    final String value = attribute.value();
                    if (value != null && !value.isEmpty()) {
                        transcribe = Boolean.parseBoolean(value);
                    }
                }
            }
            if (transcribe) {
                final Sid sid = Sid.generate(Sid.Type.TRANSCRIPTION);
                final Transcription.Builder otherBuilder = Transcription.builder();
                otherBuilder.setSid(sid);
                otherBuilder.setAccountSid(accountId);
                otherBuilder.setStatus(Transcription.Status.IN_PROGRESS);
                otherBuilder.setRecordingSid(recordingSid);
                otherBuilder.setDuration(duration);
                otherBuilder.setPrice(new BigDecimal("0.00"));
                buffer = new StringBuilder();
                buffer.append("/").append(version).append("/Accounts/").append(accountId.toString());
                buffer.append("/Transcriptions/").append(sid.toString());
                final URI uri = URI.create(buffer.toString());
                otherBuilder.setUri(uri);
                final Transcription transcription = otherBuilder.build();
                final TranscriptionsDao transcriptions = storage.getTranscriptionsDao();
                transcriptions.addTranscription(transcription);
                try {
                    final Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("callback", transcribeCallback);
                    attributes.put("transcription", transcription);
                    asrService.tell(new AsrRequest(new File(recordingUri), "en", attributes), source);
                    outstandingAsrRequests++;
                } catch (final Exception exception) {
                    logger.error(exception.getMessage(), exception);
                }
            }
            // If action is present redirect to the action URI.
            attribute = verb.attribute("action");
            if (attribute != null) {
                String action = attribute.value();
                if (action != null && !action.isEmpty()) {
                    URI target = null;
                    try {
                        target = URI.create(action);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, action
                                + " is an invalid URI.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                    final URI base = request.getUri();
                    final URI uri = UriUtils.resolve(base, target);
                    // Parse "method".
                    String method = "POST";
                    attribute = verb.attribute("method");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 13610, method
                                        + " is not a valid HTTP method for <Record>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Redirect to the action url.
                    String httpRecordingUri = configuration.subset("runtime-settings").getString("recordings-uri");
                    if (!httpRecordingUri.endsWith("/")) {
                        httpRecordingUri += "/";
                    }
                    httpRecordingUri += recordingSid.toString() + ".wav";
                    URI publicRecordingUri = URI.create(httpRecordingUri);
                    final List<NameValuePair> parameters = parameters();
                    parameters.add(new BasicNameValuePair("RecordingUrl", recordingUri.toString()));
                    parameters.add(new BasicNameValuePair("PublicRecordingUrl", publicRecordingUri.toString()));
                    parameters.add(new BasicNameValuePair("RecordingDuration", Double.toString(duration)));
                    if (MediaGroupResponse.class.equals(klass)) {
                        final MediaGroupResponse<String> response = (MediaGroupResponse<String>) message;
                        parameters.add(new BasicNameValuePair("Digits", response.get()));
                        request = new HttpRequestDescriptor(uri, method, parameters);
                        downloader.tell(request, source);
                    } else if (CallStateChanged.class.equals(klass)) {
                        parameters.add(new BasicNameValuePair("Digits", "hangup"));
                        request = new HttpRequestDescriptor(uri, method, parameters);
                        downloader.tell(request, null);
                        source.tell(StopInterpreter.instance(), source);
                    }
                    // A little clean up.
                    recordingSid = null;
                    recordingUri = null;
                    return;
                }
            }
            if (CallStateChanged.class.equals(klass)) {
                source.tell(StopInterpreter.instance(), source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
            // A little clean up.
            recordingSid = null;
            recordingUri = null;
        }
    }

    final class CreatingSmsSession extends AbstractAction {
        public CreatingSmsSession(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            // Save <Sms> verb.
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            // Create a new sms session to handle the <Sms> verb.
            smsService.tell(new CreateSmsSession(), source);
        }
    }

    final class SendingSms extends AbstractAction {
        public SendingSms(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final SmsServiceResponse<ActorRef> response = (SmsServiceResponse<ActorRef>) message;
            final ActorRef session = response.get();
            final NotificationsDao notifications = storage.getNotificationsDao();
            // Parse "from".
            String from = callInfo.to();
            Attribute attribute = verb.attribute("from");
            if (attribute != null) {
                from = attribute.value();
                if (from != null && !from.isEmpty()) {
                    from = e164(from);
                    if (from == null) {
                        from = verb.attribute("from").value();
                        final Notification notification = notification(ERROR_NOTIFICATION, 14102, from
                                + " is an invalid 'from' phone number.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        smsService.tell(new DestroySmsSession(session), source);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                }
            }
            // Parse "to".
            String to = callInfo.from();
            attribute = verb.attribute("to");
            if (attribute != null) {
                to = attribute.value();
                if (to != null && !to.isEmpty()) {
                    to = e164(to);
                    if (to == null) {
                        to = verb.attribute("to").value();
                        final Notification notification = notification(ERROR_NOTIFICATION, 14101, to
                                + " is an invalid 'to' phone number.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        smsService.tell(new DestroySmsSession(session), source);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                }
            }
            // Parse <Sms> text.
            String body = verb.text();
            if (body == null || body.isEmpty()) {
                final Notification notification = notification(ERROR_NOTIFICATION, 14103, body + " is an invalid SMS body.");
                notifications.addNotification(notification);
                sendMail(notification);
                smsService.tell(new DestroySmsSession(session), source);
                final StopInterpreter stop = StopInterpreter.instance();
                source.tell(stop, source);
                return;
            } else {
                // Start observing events from the sms session.
                session.tell(new Observe(source), source);
                // Store the status callback in the sms session.
                attribute = verb.attribute("statusCallback");
                if (attribute != null) {
                    String callback = attribute.value();
                    if (callback != null && !callback.isEmpty()) {
                        URI target = null;
                        try {
                            target = URI.create(callback);
                        } catch (final Exception exception) {
                            final Notification notification = notification(ERROR_NOTIFICATION, 14105, callback
                                    + " is an invalid URI.");
                            notifications.addNotification(notification);
                            sendMail(notification);
                            smsService.tell(new DestroySmsSession(session), source);
                            final StopInterpreter stop = StopInterpreter.instance();
                            source.tell(stop, source);
                            return;
                        }
                        final URI base = request.getUri();
                        final URI uri = UriUtils.resolve(base, target);
                        session.tell(new SmsSessionAttribute("callback", uri), source);
                    }
                }
                // Create an SMS detail record.
                final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
                final SmsMessage.Builder builder = SmsMessage.builder();
                builder.setSid(sid);
                builder.setAccountSid(accountId);
                builder.setApiVersion(version);
                builder.setRecipient(to);
                builder.setSender(from);
                builder.setBody(body);
                builder.setDirection(Direction.OUTBOUND_REPLY);
                builder.setStatus(Status.SENDING);
                builder.setPrice(new BigDecimal("0.00"));
                final StringBuilder buffer = new StringBuilder();
                buffer.append("/").append(version).append("/Accounts/");
                buffer.append(accountId.toString()).append("/SMS/Messages/");
                buffer.append(sid.toString());
                final URI uri = URI.create(buffer.toString());
                builder.setUri(uri);
                final SmsMessage record = builder.build();
                final SmsMessagesDao messages = storage.getSmsMessagesDao();
                messages.addSmsMessage(record);
                // Store the sms record in the sms session.
                session.tell(new SmsSessionAttribute("record", record), source);
                // Send the SMS.
                final SmsSessionRequest sms = new SmsSessionRequest(from, to, body, null);
                session.tell(sms, source);
                smsSessions.put(sid, session);
            }
            // Parses "action".
            attribute = verb.attribute("action");
            if (attribute != null) {
                String action = attribute.value();
                if (action != null && !action.isEmpty()) {
                    URI target = null;
                    try {
                        target = URI.create(action);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, action
                                + " is an invalid URI.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return;
                    }
                    final URI base = request.getUri();
                    final URI uri = UriUtils.resolve(base, target);
                    // Parse "method".
                    String method = "POST";
                    attribute = verb.attribute("method");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 14104, method
                                        + " is not a valid HTTP method for <Sms>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Redirect to the action url.
                    final List<NameValuePair> parameters = parameters();
                    final String status = Status.SENDING.toString();
                    parameters.add(new BasicNameValuePair("SmsStatus", status));
                    request = new HttpRequestDescriptor(uri, method, parameters);
                    downloader.tell(request, source);
                    return;
                }
            }
            // Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
        }
    }
}

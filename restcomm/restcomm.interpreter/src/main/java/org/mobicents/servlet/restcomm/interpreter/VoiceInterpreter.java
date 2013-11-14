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

import static akka.pattern.Patterns.ask;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.dial;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import org.mobicents.servlet.restcomm.fax.FaxResponse;
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
import org.mobicents.servlet.restcomm.interpreter.rcml.End;
import org.mobicents.servlet.restcomm.interpreter.rcml.GetNextVerb;
import org.mobicents.servlet.restcomm.interpreter.rcml.Nouns;
import org.mobicents.servlet.restcomm.interpreter.rcml.Parser;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.sms.CreateSmsSession;
import org.mobicents.servlet.restcomm.sms.DestroySmsSession;
import org.mobicents.servlet.restcomm.sms.SmsServiceResponse;
import org.mobicents.servlet.restcomm.sms.SmsSessionAttribute;
import org.mobicents.servlet.restcomm.sms.SmsSessionInfo;
import org.mobicents.servlet.restcomm.sms.SmsSessionRequest;
import org.mobicents.servlet.restcomm.sms.SmsSessionResponse;
import org.mobicents.servlet.restcomm.telephony.AddParticipant;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallManagerResponse;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Cancel;
import org.mobicents.servlet.restcomm.telephony.Collect;
import org.mobicents.servlet.restcomm.telephony.ConferenceCenterResponse;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;
import org.mobicents.servlet.restcomm.telephony.ConferenceModeratorPresent;
import org.mobicents.servlet.restcomm.telephony.ConferenceResponse;
import org.mobicents.servlet.restcomm.telephony.ConferenceStateChanged;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.restcomm.telephony.CreateConference;
import org.mobicents.servlet.restcomm.telephony.CreateMediaGroup;
import org.mobicents.servlet.restcomm.telephony.CreateWaitUrlConfMediaGroup;
import org.mobicents.servlet.restcomm.telephony.DestroyCall;
import org.mobicents.servlet.restcomm.telephony.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.telephony.Dial;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.GetConferenceInfo;
import org.mobicents.servlet.restcomm.telephony.Hangup;
import org.mobicents.servlet.restcomm.telephony.MediaGroupResponse;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.telephony.Mute;
import org.mobicents.servlet.restcomm.telephony.Play;
import org.mobicents.servlet.restcomm.telephony.Record;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.telephony.RemoveParticipant;
import org.mobicents.servlet.restcomm.telephony.StartMediaGroup;
import org.mobicents.servlet.restcomm.telephony.Stop;
import org.mobicents.servlet.restcomm.telephony.StopConference;
import org.mobicents.servlet.restcomm.telephony.StopMediaGroup;
import org.mobicents.servlet.restcomm.telephony.Unmute;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;
import org.mobicents.servlet.restcomm.util.WavUtils;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import javax.servlet.sip.SipServletResponse;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 * @author gvagenas@telestax.com
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 */
public final class VoiceInterpreter extends UntypedActor {
    private static final int ERROR_NOTIFICATION = 0;
    private static final int WARNING_NOTIFICATION = 1;
    private static final Pattern PATTERN = Pattern.compile("[\\*#0-9]{1,12}");
    private static final String EMAIL_SENDER = "restcomm@restcomm.org";
    private static final String EMAIL_SUBJECT = "RestComm Error Notification - Attention Required";
    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // States for the FSM.
    private final State uninitialized;
    private final State acquiringAsrInfo;
    private final State acquiringSynthesizerInfo;
    private final State acquiringCallInfo;
    private final State downloadingRcml;
    private final State downloadingFallbackRcml;
    private final State initializingCall;
    private final State initializingCallMediaGroup;
    private final State acquiringCallMediaGroup;
    private final State ready;
    private final State notFound;
    private final State rejecting;
    private final State playingRejectionPrompt;
    private final State pausing;
    private final State caching;
    private final State checkingCache;
    private final State playing;
    private final State synthesizing;
    private final State redirecting;
    private final State faxing;
    private final State processingGatherChildren;
    private final State gathering;
    private final State finishGathering;
    private final State creatingRecording;
    private final State finishRecording;
    private final State creatingSmsSession;
    private final State sendingSms;
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
    private final State hangingUp;
    private final State finished;
    // FSM.
    private final FiniteStateMachine fsm;
    // The user specific configuration.
    private final Configuration configuration;
    // The block storage cache.
    private final ActorRef cache;
    private final String cachePath;
    // The downloader will fetch resources for us using HTTP.
    private final ActorRef downloader;
    // The mail man that will deliver e-mail.
    private ActorRef mailer;
    // The call manager.
    private final ActorRef callManager;
    // The conference manager.
    private final ActorRef conferenceManager;
    // The automatic speech recognition service.
    private final ActorRef asrService;
    private int outstandingAsrRequests;
    // The fax service.
    private final ActorRef faxService;
    // The SMS service.
    private final ActorRef smsService;
    private final Map<Sid, ActorRef> smsSessions;
    // The storage engine.
    private final DaoManager storage;
    // The text to speech synthesizer service.
    private final ActorRef synthesizer;
    // The languages supported by the automatic speech recognition service.
    private AsrInfo asrInfo;
    // The languages supported by the text to speech synthesizer service.
    private SpeechSynthesizerInfo synthesizerInfo;
    // The call being handled by this interpreter.
    private ActorRef call;
    private ActorRef callMediaGroup;
    // The information for this call.
    private CallInfo callInfo;
    // The call state.
    private CallStateChanged.State callState;
    // A call detail record.
    private CallDetailRecord callRecord;
    // State for outbound calls.
    private boolean isForking;
    private List<ActorRef> dialBranches;
    private List<Tag> dialChildren;
    private Map<ActorRef, Tag> dialChildrenWithAttributes;
    private ActorRef outboundCall;
    private CallInfo outboundCallInfo;
    // State for the gather verb.
    private List<Tag> gatherChildren;
    private List<URI> gatherPrompts;
    // The call recording stuff.
    private Sid recordingSid;
    private URI recordingUri;
    // The conferencing stuff.
    private ActorRef conference;
    private ConferenceInfo conferenceInfo;

    private ConferenceStateChanged.State conferenceState;
    private boolean callMuted;
    private boolean startConferenceOnEnter = true;
    private ActorRef confSubVoiceInterpreter;

    private ActorRef conferenceMediaGroup;
    // Information to reach the application that will be executed
    // by this interpreter.
    private final Sid accountId;
    private final Sid phoneId;
    private final String version;
    private final URI url;
    private final String method;
    private final URI fallbackUrl;
    private final String fallbackMethod;
    private final URI statusCallback;
    private String statusCallbackMethod;
    private final String emailAddress;
    // application data.
    private HttpRequestDescriptor request;
    private HttpResponseDescriptor response;
    // The RCML parser.
    private ActorRef parser;
    private Tag verb;

    public VoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone, final String version,
            final URI url, final String method, final URI fallbackUrl, final String fallbackMethod, final URI statusCallback,
            final String statusCallbackMethod, final String emailAddress, final ActorRef callManager,
            final ActorRef conferenceManager, final ActorRef sms, final DaoManager storage) {
        super();
        final ActorRef source = self();
        uninitialized = new State("uninitialized", null, null);
        acquiringAsrInfo = new State("acquiring asr info", new AcquiringAsrInfo(source), null);
        acquiringSynthesizerInfo = new State("acquiring tts info", new AcquiringSpeechSynthesizerInfo(source), null);
        acquiringCallInfo = new State("acquiring call info", new AcquiringCallInfo(source), null);
        acquiringCallMediaGroup = new State("acquiring call media group", new AcquiringCallMediaGroup(source), null);
        downloadingRcml = new State("downloading rcml", new DownloadingRcml(source), null);
        downloadingFallbackRcml = new State("downloading fallback rcml", new DownloadingFallbackRcml(source), null);
        initializingCall = new State("initializing call", new InitializingCall(source), null);
        initializingCallMediaGroup = new State("initializing call media group", new InitializingCallMediaGroup(source), null);
        ready = new State("ready", new Ready(source), null);
        notFound = new State("notFound", new NotFound(source), null);
        rejecting = new State("rejecting", new Rejecting(source), null);
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
        hangingUp = new State("hanging up", new HangingUp(source), null);
        finished = new State("finished", new Finished(source), null);
        /*
         * dialing = new State("dialing", null, null); bridging = new State("bridging", null, null); conferencing = new
         * State("conferencing", null, null);
         */
        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringAsrInfo));
        transitions.add(new Transition(acquiringAsrInfo, acquiringSynthesizerInfo));
        transitions.add(new Transition(acquiringAsrInfo, finished));
        transitions.add(new Transition(acquiringSynthesizerInfo, acquiringCallInfo));
        transitions.add(new Transition(acquiringSynthesizerInfo, finished));
        transitions.add(new Transition(acquiringCallInfo, initializingCall));
        transitions.add(new Transition(acquiringCallInfo, downloadingRcml));
        transitions.add(new Transition(acquiringCallInfo, finished));
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
        transitions.add(new Transition(pausing, hangingUp));
        transitions.add(new Transition(pausing, finished));
        transitions.add(new Transition(rejecting, acquiringCallMediaGroup));
        transitions.add(new Transition(rejecting, finished));
        transitions.add(new Transition(playingRejectionPrompt, hangingUp));
        transitions.add(new Transition(faxing, faxing));
        transitions.add(new Transition(faxing, ready));
        transitions.add(new Transition(faxing, caching));
        transitions.add(new Transition(faxing, pausing));
        transitions.add(new Transition(faxing, redirecting));
        transitions.add(new Transition(faxing, synthesizing));
        transitions.add(new Transition(faxing, processingGatherChildren));
        transitions.add(new Transition(faxing, creatingRecording));
        transitions.add(new Transition(faxing, creatingSmsSession));
        transitions.add(new Transition(faxing, startDialing));
        transitions.add(new Transition(faxing, hangingUp));
        transitions.add(new Transition(faxing, finished));
        transitions.add(new Transition(caching, faxing));
        transitions.add(new Transition(caching, playing));
        transitions.add(new Transition(caching, caching));
        transitions.add(new Transition(caching, pausing));
        transitions.add(new Transition(caching, redirecting));
        transitions.add(new Transition(caching, synthesizing));
        transitions.add(new Transition(caching, processingGatherChildren));
        transitions.add(new Transition(caching, creatingRecording));
        transitions.add(new Transition(caching, creatingSmsSession));
        transitions.add(new Transition(caching, startDialing));
        transitions.add(new Transition(caching, hangingUp));
        transitions.add(new Transition(caching, finished));
        transitions.add(new Transition(checkingCache, synthesizing));
        transitions.add(new Transition(checkingCache, playing));
        transitions.add(new Transition(checkingCache, checkingCache));
        transitions.add(new Transition(playing, ready));
        transitions.add(new Transition(playing, hangingUp));
        transitions.add(new Transition(playing, finished));
        transitions.add(new Transition(synthesizing, faxing));
        transitions.add(new Transition(synthesizing, pausing));
        transitions.add(new Transition(synthesizing, checkingCache));
        transitions.add(new Transition(synthesizing, caching));
        transitions.add(new Transition(synthesizing, redirecting));
        transitions.add(new Transition(synthesizing, processingGatherChildren));
        transitions.add(new Transition(synthesizing, creatingRecording));
        transitions.add(new Transition(synthesizing, creatingSmsSession));
        transitions.add(new Transition(synthesizing, synthesizing));
        transitions.add(new Transition(synthesizing, startDialing));
        transitions.add(new Transition(synthesizing, hangingUp));
        transitions.add(new Transition(synthesizing, finished));
        transitions.add(new Transition(redirecting, faxing));
        transitions.add(new Transition(redirecting, ready));
        transitions.add(new Transition(redirecting, pausing));
        transitions.add(new Transition(redirecting, checkingCache));
        transitions.add(new Transition(redirecting, caching));
        transitions.add(new Transition(redirecting, synthesizing));
        transitions.add(new Transition(redirecting, redirecting));
        transitions.add(new Transition(redirecting, processingGatherChildren));
        transitions.add(new Transition(redirecting, creatingRecording));
        transitions.add(new Transition(redirecting, creatingSmsSession));
        transitions.add(new Transition(redirecting, startDialing));
        transitions.add(new Transition(redirecting, hangingUp));
        transitions.add(new Transition(redirecting, finished));
        transitions.add(new Transition(creatingRecording, finishRecording));
        transitions.add(new Transition(creatingRecording, hangingUp));
        transitions.add(new Transition(creatingRecording, finished));
        transitions.add(new Transition(finishRecording, faxing));
        transitions.add(new Transition(finishRecording, ready));
        transitions.add(new Transition(finishRecording, pausing));
        transitions.add(new Transition(finishRecording, checkingCache));
        transitions.add(new Transition(finishRecording, caching));
        transitions.add(new Transition(finishRecording, synthesizing));
        transitions.add(new Transition(finishRecording, redirecting));
        transitions.add(new Transition(finishRecording, processingGatherChildren));
        transitions.add(new Transition(finishRecording, creatingRecording));
        transitions.add(new Transition(finishRecording, creatingSmsSession));
        transitions.add(new Transition(finishRecording, startDialing));
        transitions.add(new Transition(finishRecording, hangingUp));
        transitions.add(new Transition(finishRecording, finished));
        transitions.add(new Transition(processingGatherChildren, processingGatherChildren));
        transitions.add(new Transition(processingGatherChildren, gathering));
        transitions.add(new Transition(processingGatherChildren, hangingUp));
        transitions.add(new Transition(processingGatherChildren, finished));
        transitions.add(new Transition(gathering, finishGathering));
        transitions.add(new Transition(gathering, hangingUp));
        transitions.add(new Transition(gathering, finished));
        transitions.add(new Transition(finishGathering, ready));
        transitions.add(new Transition(finishGathering, faxing));
        transitions.add(new Transition(finishGathering, pausing));
        transitions.add(new Transition(finishGathering, checkingCache));
        transitions.add(new Transition(finishGathering, caching));
        transitions.add(new Transition(finishGathering, synthesizing));
        transitions.add(new Transition(finishGathering, redirecting));
        transitions.add(new Transition(finishGathering, processingGatherChildren));
        transitions.add(new Transition(finishGathering, creatingRecording));
        transitions.add(new Transition(finishGathering, creatingSmsSession));
        transitions.add(new Transition(finishGathering, startDialing));
        transitions.add(new Transition(finishGathering, hangingUp));
        transitions.add(new Transition(finishGathering, finished));
        transitions.add(new Transition(creatingSmsSession, sendingSms));
        transitions.add(new Transition(creatingSmsSession, hangingUp));
        transitions.add(new Transition(creatingSmsSession, finished));
        transitions.add(new Transition(sendingSms, faxing));
        transitions.add(new Transition(sendingSms, ready));
        transitions.add(new Transition(sendingSms, pausing));
        transitions.add(new Transition(sendingSms, caching));
        transitions.add(new Transition(sendingSms, synthesizing));
        transitions.add(new Transition(sendingSms, redirecting));
        transitions.add(new Transition(sendingSms, processingGatherChildren));
        transitions.add(new Transition(sendingSms, creatingRecording));
        transitions.add(new Transition(sendingSms, creatingSmsSession));
        transitions.add(new Transition(sendingSms, startDialing));
        transitions.add(new Transition(sendingSms, hangingUp));
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

    private ActorRef asr(final Configuration configuration) {
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
    private void asrResponse(final Object message) {
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

    private ActorRef fax(final Configuration configuration) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new InterfaxService(configuration);
            }
        }));
    }

    private void callback() {
        if (statusCallback != null) {
            if (statusCallbackMethod == null) {
                statusCallbackMethod = "POST";
            }
            final List<NameValuePair> parameters = parameters();
            request = new HttpRequestDescriptor(statusCallback, statusCallbackMethod, parameters);
            downloader.tell(request, null);
        }
    }

    private ActorRef cache(final String path, final String uri) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new DiskCache(path, uri, true);
            }
        }));
    }

    private ActorRef downloader() {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Downloader();
            }
        }));
    }

    private String e164(final String number) {
        final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
        try {
            final PhoneNumber result = numbersUtil.parse(number, "US");
            return numbersUtil.format(result, PhoneNumberFormat.E164);
        } catch (final NumberParseException ignored) {
            return number;
        }
    }

    private void invalidVerb(final Tag verb) {
        final ActorRef self = self();
        // Get the next verb.
        final GetNextVerb next = GetNextVerb.instance();
        parser.tell(next, self);
    }

    private ActorRef mailer(final Configuration configuration) {
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

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();
        final ActorRef sender = sender();
        if (logger.isInfoEnabled()) {
            logger.info(" ********** VoiceInterpreter's Current State: " + state.toString());
            logger.info(" ********** VoiceInterpreter's Processing Message: " + klass.getName());
        }
        if (StartInterpreter.class.equals(klass)) {
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
            if (forking.equals(state)) {
                // Allow updating of the callInfo at the VoiceInterpreter so that we can do Dial SIP Screening
                // (https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out) accurately from latest
                // response received
                final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
                callInfo = response.get();
            } else if (acquiringCallInfo.equals(state)) {
                final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
                callInfo = response.get();
                final String direction = callInfo.direction();
                if ("inbound".equals(direction)) {
                    fsm.transition(message, downloadingRcml);
                } else {
                    fsm.transition(message, initializingCall);
                }
            } else if (acquiringCallMediaGroup.equals(state)) {
                fsm.transition(message, initializingCallMediaGroup);
            } else if (acquiringOutboundCallInfo.equals(state)) {
                fsm.transition(message, joiningCalls);
            }
        } else if (CallStateChanged.class.equals(klass)) {
            final CallStateChanged event = (CallStateChanged) message;
            callState = event.state();
            if (CallStateChanged.State.RINGING == event.state()) {
                // update db and callback statusCallback url.
            } else if (CallStateChanged.State.IN_PROGRESS == event.state()) {
                if (initializingCall.equals(state) || rejecting.equals(state)) {
                    fsm.transition(message, acquiringCallMediaGroup);
                } else if (joiningConference.equals(state)) {
                    fsm.transition(message, conferencing);
                } else if (forking.equals(state)) {
                    outboundCall = sender;
                    fsm.transition(message, acquiringOutboundCallInfo);
                } else if (joiningCalls.equals(state)) {
                    fsm.transition(message, bridged);
                }
            } else if (CallStateChanged.State.NO_ANSWER == event.state() || CallStateChanged.State.COMPLETED == event.state()
                    || CallStateChanged.State.FAILED == event.state()) {
                // changed for https://bitbucket.org/telestax/telscale-restcomm/issue/132/ so that we can do Dial SIP Screening
                if ((bridged.equals(state) || forking.equals(state)) && (sender == outboundCall || outboundCall == null)) {
                    fsm.transition(message, finishDialing);
                } else if (creatingRecording.equals(state)) {
                    fsm.transition(message, finishRecording);
                } else if (!forking.equals(state) || call == sender()) {
                    fsm.transition(message, finished);
                }
            } else if (CallStateChanged.State.BUSY == event.state()) {
                fsm.transition(message, finishDialing);
            }
        } else if (CallManagerResponse.class.equals(klass)) {
            final CallManagerResponse<ActorRef> response = (CallManagerResponse<ActorRef>) message;
            if (response.succeeded()) {
                if (startDialing.equals(state)) {
                    fsm.transition(message, processingDialChildren);
                } else if (processingDialChildren.equals(state)) {
                    fsm.transition(message, processingDialChildren);
                }
            } else {
                fsm.transition(message, hangingUp);
            }
        } else if (StartForking.class.equals(klass)) {
            fsm.transition(message, processingDialChildren);
        } else if (ConferenceCenterResponse.class.equals(klass)) {
            if (startDialing.equals(state)) {
                fsm.transition(message, acquiringConferenceInfo);
            }
        } else if (Fork.class.equals(klass)) {
            if (processingDialChildren.equals(state)) {
                fsm.transition(message, forking);
            }
        } else if (ConferenceResponse.class.equals(klass)) {
            if (acquiringConferenceInfo.equals(state)) {
                fsm.transition(message, acquiringConferenceMediaGroup);
            } else if (acquiringConferenceMediaGroup.equals(state)) {
                fsm.transition(message, initializingConferenceMediaGroup);
            }
        } else if (ConferenceStateChanged.class.equals(klass)) {
            final ConferenceStateChanged event = (ConferenceStateChanged) message;
            if (ConferenceStateChanged.State.COMPLETED == event.state()) {
                if (conferencing.equals(state)) {
                    fsm.transition(message, finishConferencing);
                }
            } else if (ConferenceStateChanged.State.RUNNING_MODERATOR_PRESENT == event.state()) {
                conferenceState = event.state();
                conferenceStateModeratorPresent(message);
            }
        } else if (DownloaderResponse.class.equals(klass)) {
            final DownloaderResponse response = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("Rcml URI : " + response.get().getURI() + "response succeeded " + response.succeeded()
                        + ", statusCode " + response.get().getStatusCode());
            }
            if (response.succeeded() && HttpStatus.SC_OK == response.get().getStatusCode()) {
                fsm.transition(message, ready);
            } else if (response.succeeded() && HttpStatus.SC_NOT_FOUND == response.get().getStatusCode()) {
                fsm.transition(message, notFound);
            } else {
                if (downloadingRcml.equals(state)) {
                    if (fallbackUrl != null) {
                        fsm.transition(message, downloadingFallbackRcml);
                    } else {
                        fsm.transition(message, finished);
                    }
                } else {
                    fsm.transition(message, finished);
                }
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
            final Tag verb = (Tag) message;
            if (CallStateChanged.State.RINGING == callState) {
                if (reject.equals(verb.name())) {
                    fsm.transition(message, rejecting);
                } else if (pause.equals(verb.name())) {
                    fsm.transition(message, pausing);
                } else {
                    fsm.transition(message, initializingCall);
                }
            } else if (dial.equals(verb.name())) {
                fsm.transition(message, startDialing);
            } else if (fax.equals(verb.name())) {
                fsm.transition(message, caching);
            } else if (play.equals(verb.name())) {
                fsm.transition(message, caching);
            } else if (say.equals(verb.name())) {
                // fsm.transition(message, synthesizing);
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
            fsm.transition(message, hangingUp);
        } else if (StartGathering.class.equals(klass)) {
            fsm.transition(message, gathering);
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            final MediaGroupStateChanged event = (MediaGroupStateChanged) message;
            if (MediaGroupStateChanged.State.ACTIVE == event.state()) {
                if (initializingCallMediaGroup.equals(state)) {
                    final String direction = callInfo.direction();
                    if ("inbound".equals(direction)) {
                        if (reject.equals(verb.name())) {
                            fsm.transition(message, playingRejectionPrompt);
                        } else if (dial.equals(verb.name())) {
                            fsm.transition(message, startDialing);
                        } else if (fax.equals(verb.name())) {
                            fsm.transition(message, caching);
                        } else if (play.equals(verb.name())) {
                            fsm.transition(message, caching);
                        } else if (say.equals(verb.name())) {
                            // fsm.transition(message, synthesizing);
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
                    } else {
                        fsm.transition(message, downloadingRcml);
                    }
                } else if (initializingConferenceMediaGroup.equals(state)) {
                    fsm.transition(message, joiningConference);
                }
            } else if (MediaGroupStateChanged.State.INACTIVE == event.state()) {
                if (!hangingUp.equals(state)) {
                    fsm.transition(message, hangingUp);
                }
            }
        } else if (MediaGroupResponse.class.equals(klass)) {
            final MediaGroupResponse<String> response = (MediaGroupResponse<String>) message;
            if (response.succeeded()) {
                if (playingRejectionPrompt.equals(state)) {
                    fsm.transition(message, hangingUp);
                } else if (playing.equals(state)) {
                    fsm.transition(message, ready);
                } else if (creatingRecording.equals(state)) {
                    fsm.transition(message, finishRecording);
                } else if (gathering.equals(state)) {
                    fsm.transition(message, finishGathering);
                } else if (initializingConferenceMediaGroup.equals(state)) {
                    fsm.transition(message, joiningConference);
                }
            } else {
                fsm.transition(message, hangingUp);
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
            } else if (conferencing.equals(state)) {
                fsm.transition(message, finishConferencing);
            } else if (forking.equals(state)) {
                fsm.transition(message, finishDialing);
            } else if (bridged.equals(state)) {
                fsm.transition(message, finishDialing);
            }
        }
    }

    private void conferenceStateModeratorPresent(final Object message) {
        if (!startConferenceOnEnter && !callMuted) {
            logger.info("VoiceInterpreter#conferenceStateModeratorPresent will unmute the call");
            call.tell(new Unmute(), self());
        }

        if (confSubVoiceInterpreter != null) {
            logger.info("VoiceInterpreter stopping confSubVoiceInterpreter");

            // Stop the conference back ground music
            final StopInterpreter stop = StopInterpreter.instance();
            confSubVoiceInterpreter.tell(stop, self());
        }
    }

    private List<NameValuePair> parameters() {
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
        // logger.info("Type " + callInfo.type());
        if (CreateCall.Type.SIP == callInfo.type()) {
            // Adding SIP OUT Headers and SipCallId for
            // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
            SipServletResponse lastResponse = callInfo.lastResponse();
            // logger.info("lastResponse " + lastResponse);
            if (lastResponse != null) {
                final int statusCode = lastResponse.getStatus();
                final String method = lastResponse.getMethod();
                // See https://www.twilio.com/docs/sip/receiving-sip-headers
                // Headers on the final SIP response message (any 4xx or 5xx message or the final BYE/200) are posted to the
                // Dial action URL.
                if ((statusCode >= 400 && "INVITE".equalsIgnoreCase(method))
                        || (statusCode >= 200 && statusCode < 300 && "BYE".equalsIgnoreCase(method))) {
                    final String sipCallId = lastResponse.getCallId();
                    parameters.add(new BasicNameValuePair("DialSipCallId", sipCallId));
                    parameters.add(new BasicNameValuePair("DialSipResponseCode", "" + statusCode));
                    Iterator<String> headerIt = lastResponse.getHeaderNames();
                    while (headerIt.hasNext()) {
                        String headerName = headerIt.next();
                        if (headerName.startsWith("X-")) {
                            parameters.add(new BasicNameValuePair("DialSipHeader_" + headerName, lastResponse
                                    .getHeader(headerName)));
                        }
                    }
                }
            }
        }
        return parameters;
    }

    private ActorRef parser(final String xml) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Parser(xml);
            }
        }));
    }

    private void postCleanup() {
        final ActorRef self = self();
        if (smsSessions.isEmpty() && outstandingAsrRequests == 0) {
            final UntypedActorContext context = getContext();
            context.stop(self);
        }
    }

    private URI resolve(final URI base, final URI uri) {
        if (base.equals(uri)) {
            return uri;
        } else {
            if (!uri.isAbsolute()) {
                return base.resolve(uri);
            } else {
                return uri;
            }
        }
    }

    private void sendMail(final Notification notification) {
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

    private void smsResponse(final Object message) {
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

    private ActorRef tts(final Configuration configuration) {
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

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringAsrInfo extends AbstractAction {
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

    private final class AcquiringSpeechSynthesizerInfo extends AbstractAction {
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

    private final class AcquiringCallInfo extends AbstractAction {
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

    private final class InitializingCall extends AbstractAction {
        public InitializingCall(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallResponse.class.equals(klass)) {
                // Update the interpreter state.
                final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
                callInfo = response.get();
                callState = callInfo.state();
                // Update the storage.
                if (callRecord != null) {
                    callRecord = callRecord.setStatus(callState.toString());
                    final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
                    records.updateCallDetailRecord(callRecord);
                }
                // Update the application.
                callback();
                // Start dialing.
                call.tell(new Dial(), source);
            } else if (Tag.class.equals(klass)) {
                // Update the interpreter state.
                verb = (Tag) message;
                // Answer the call.
                call.tell(new Answer(), source);
            }
        }
    }

    private final class AcquiringCallMediaGroup extends AbstractAction {
        public AcquiringCallMediaGroup(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallStateChanged.class.equals(klass)) {
                // Update the interpreter state.
                final CallStateChanged event = (CallStateChanged) message;
                callState = event.state();
                // Update the storage.
                if (callRecord != null) {
                    callRecord = callRecord.setStatus(callState.toString());
                    callRecord = callRecord.setStartTime(DateTime.now());
                    final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
                    records.updateCallDetailRecord(callRecord);
                }
                // Update the application.
                callback();
            }
            call.tell(new CreateMediaGroup(), source);
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
            if (CallResponse.class.equals(klass)) {
                final CallResponse<ActorRef> response = (CallResponse<ActorRef>) message;
                callMediaGroup = response.get();
                callMediaGroup.tell(new Observe(source), source);
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
                if (callInfo.direction().equals("inbound")) {
                    // Create a call detail record for the call.
                    final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                    builder.setSid(callInfo.sid());
                    builder.setDateCreated(callInfo.dateCreated());
                    builder.setAccountSid(accountId);
                    builder.setTo(callInfo.to());
                    builder.setCallerName(callInfo.fromName());
                    builder.setFrom(callInfo.from());
                    builder.setForwardedFrom(callInfo.forwardedFrom());
                    builder.setPhoneNumberSid(phoneId);
                    builder.setStatus(callState.toString());
                    final DateTime now = DateTime.now();
                    builder.setStartTime(now);
                    builder.setDirection(callInfo.direction());
                    builder.setApiVersion(version);
                    builder.setPrice(new BigDecimal("0.00"));
                    // TODO implement currency property to be read from Configuration
                    builder.setPriceUnit(Currency.getInstance("USD"));
                    final StringBuilder buffer = new StringBuilder();
                    buffer.append("/").append(version).append("/Accounts/");
                    buffer.append(accountId.toString()).append("/Calls/");
                    buffer.append(callInfo.sid().toString());
                    final URI uri = URI.create(buffer.toString());
                    builder.setUri(uri);

                    builder.setCallPath(call.path().toString());

                    callRecord = builder.build();
                    records.addCallDetailRecord(callRecord);
                    // Update the application.
                    callback();
                }
            }
            // Ask the downloader to get us the application that will be executed.
            final List<NameValuePair> parameters = parameters();
            request = new HttpRequestDescriptor(url, method, parameters);
            downloader.tell(request, source);
        }
    }

    private final class DownloadingFallbackRcml extends AbstractAction {
        public DownloadingFallbackRcml(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            // Notify the account of the issue.
            if (DownloaderResponse.class.equals(klass)) {
                final DownloaderResponse result = (DownloaderResponse) message;
                final Throwable cause = result.cause();
                Notification notification = null;
                if (cause instanceof ClientProtocolException) {
                    notification = notification(ERROR_NOTIFICATION, 11206, cause.getMessage());
                } else if (cause instanceof IOException) {
                    notification = notification(ERROR_NOTIFICATION, 11205, cause.getMessage());
                } else if (cause instanceof URISyntaxException) {
                    notification = notification(ERROR_NOTIFICATION, 11100, cause.getMessage());
                }
                if (notification != null) {
                    final NotificationsDao notifications = storage.getNotificationsDao();
                    notifications.addNotification(notification);
                    sendMail(notification);
                }
            }
            // Try to use the fall back url and method.
            final List<NameValuePair> parameters = parameters();
            request = new HttpRequestDescriptor(fallbackUrl, fallbackMethod, parameters);
            downloader.tell(request, source);
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
            if (initializingCallMediaGroup.equals(state)) {
                // Handle pending verbs.
                source.tell(verb, source);
                return;
            } else if (downloadingRcml.equals(state) || downloadingFallbackRcml.equals(state) || redirecting.equals(state)
                    || finishGathering.equals(state) || finishRecording.equals(state) || sendingSms.equals(state)
                    || finishDialing.equals(state) || finishConferencing.equals(state)) {
                response = ((DownloaderResponse) message).get();
                if (parser != null) {
                    context.stop(parser);
                    parser = null;
                }
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
            } else if (pausing.equals(state)) {
                context.setReceiveTimeout(Duration.Undefined());
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

    private final class PlayingRejectionPrompt extends AbstractAction {
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

    private final class Faxing extends AbstractAction {
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

    private final class Pausing extends AbstractAction {
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

    private final class CheckCache extends AbstractAction {
        public CheckCache(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
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

    private final class Caching extends AbstractAction {
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
                    final URI uri = resolve(base, target);
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

    private final class Playing extends AbstractAction {
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

    private String hash(Object message) {
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

    private Map<String, String> getSynthesizeDetails(final Object message) {
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

    private final class Synthesizing extends AbstractAction {
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
            if (details != null && !details.isEmpty()) {
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

    private final class HangingUp extends AbstractAction {
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

    private final class Redirecting extends AbstractAction {
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
                final URI uri = resolve(base, target);
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

    private abstract class AbstractGatherAction extends AbstractAction {
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

    private final class ProcessingGatherChildren extends AbstractGatherAction {
        public ProcessingGatherChildren(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
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
                    final DiskCacheResponse response = (DiskCacheResponse) message;
                    final URI uri = response.get();
                    final Tag child = gatherChildren.remove(0);
                    // Parse the loop attribute.
                    int loop = 1;
                    final Attribute attribute = child.attribute("loop");
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
                            final URI uri = resolve(base, target);
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
                            final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(voice, language, text);
                            synthesizer.tell(synthesize, source);
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
                    final StartGathering start = StartGathering.instance();
                    source.tell(start, source);
                }
            }
        }
    }

    private final class Gathering extends AbstractGatherAction {
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

    private final class FinishGathering extends AbstractGatherAction {
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
                    final URI uri = resolve(base, target);
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

    private final class CreatingRecording extends AbstractAction {
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
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += recordingSid.toString() + ".wav";
            recordingUri = URI.create(path);
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

    private final class FinishRecording extends AbstractAction {
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
            final Double duration = WavUtils.getAudioDuration(recordingUri);
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
                // TODO implement currency property to be read from Configuration
                otherBuilder.setPriceUnit(Currency.getInstance("USD"));
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
                    final URI uri = resolve(base, target);
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
                    final List<NameValuePair> parameters = parameters();
                    parameters.add(new BasicNameValuePair("RecordingUrl", recordingUri.toString()));
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

    private final class CreatingSmsSession extends AbstractAction {
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

    private final class SendingSms extends AbstractAction {
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
            if (body == null || body.isEmpty() || body.length() > 160) {
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
                        final URI uri = resolve(base, target);
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
                // TODO implement currency property to be read from Configuration
                builder.setPriceUnit(Currency.getInstance("USD"));
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
                final SmsSessionRequest sms = new SmsSessionRequest(from, to, body);
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
                    final URI uri = resolve(base, target);
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

    private abstract class AbstractDialAction extends AbstractAction {
        public AbstractDialAction(final ActorRef source) {
            super(source);
        }

        protected String callerId(final Tag container) {
            // Parse "from".
            String callerId = null;
            Attribute attribute = verb.attribute("callerId");
            if (attribute != null) {
                callerId = attribute.value();
                if (callerId != null && !callerId.isEmpty()) {
                    callerId = e164(callerId);
                    if (callerId == null) {
                        callerId = verb.attribute("callerId").value();
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        final Notification notification = notification(ERROR_NOTIFICATION, 13214, callerId
                                + " is an invalid callerId.");
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = StopInterpreter.instance();
                        source.tell(stop, source);
                        return null;
                    }
                }
            }
            return callerId;
        }

        protected Tag conference(final Tag container) {
            final List<Tag> children = container.children();
            for (final Tag child : children) {
                if (Nouns.conference.equals(child.name())) {
                    return child;
                }
            }
            return null;
        }

        protected int timeout(final Tag container) {
            int timeout = 30;
            Attribute attribute = container.attribute("timeout");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        timeout = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        final Notification notification = notification(WARNING_NOTIFICATION, 13212, value
                                + " is not a valid timeout value for <Dial>");
                        notifications.addNotification(notification);
                    }
                }
            }
            return timeout;
        }

        protected int timeLimit(final Tag container) {
            int timeLimit = 14400;
            Attribute attribute = container.attribute("timeLimit");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        timeLimit = Integer.parseInt(value);
                    } catch (final NumberFormatException exception) {
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        final Notification notification = notification(WARNING_NOTIFICATION, 13216, value
                                + " is not a valid timeLimit value for <Dial>");
                        notifications.addNotification(notification);
                    }
                }
            }
            return timeLimit;
        }
    }

    private final class StartDialing extends AbstractDialAction {
        public StartDialing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            final String text = verb.text();
            if (text != null && !text.isEmpty()) {
                // Handle bridging.
                isForking = false;
                final CreateCall create = new CreateCall(e164(callerId(verb)), e164(text), null, null, false, timeout(verb),
                        CreateCall.Type.PSTN, accountId);
                callManager.tell(create, source);
            } else if (verb.hasChildren()) {
                // Handle conferencing.
                final Tag child = conference(verb);
                if (child != null) {
                    final String name = child.text();
                    final StringBuilder buffer = new StringBuilder();
                    buffer.append(accountId.toString()).append(":").append(name);
                    final CreateConference create = new CreateConference(buffer.toString());
                    conferenceManager.tell(create, source);
                } else {
                    // Handle forking.
                    dialBranches = new ArrayList<ActorRef>();
                    dialChildren = new ArrayList<Tag>(verb.children());
                    dialChildrenWithAttributes = new HashMap<ActorRef, Tag>();
                    isForking = true;
                    final StartForking start = StartForking.instance();
                    source.tell(start, source);
                }
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    private final class ProcessingDialChildren extends AbstractDialAction {
        public ProcessingDialChildren(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            Class<?> klass = message.getClass();
            if (CallManagerResponse.class.equals(klass)) {
                final CallManagerResponse<ActorRef> response = (CallManagerResponse<ActorRef>) message;
                final ActorRef branch = response.get();
                dialBranches.add(branch);
                Tag child = dialChildren.get(0);
                if (child.hasAttributes()) {
                    dialChildrenWithAttributes.put(branch, child);
                }
                dialChildren.remove(child);
            }
            if (!dialChildren.isEmpty()) {
                CreateCall create = null;
                final Tag child = dialChildren.get(0);
                if (Nouns.client.equals(child.name())) {
                    create = new CreateCall(e164(callerId(verb)), e164(child.text()), null, null, false, timeout(verb),
                            CreateCall.Type.CLIENT, accountId);
                } else if (Nouns.number.equals(child.name())) {
                    create = new CreateCall(e164(callerId(verb)), e164(child.text()), null, null, false, timeout(verb),
                            CreateCall.Type.PSTN, accountId);
                } else if (Nouns.uri.equals(child.name())) {
                    create = new CreateCall(e164(callerId(verb)), e164(child.text()), null, null, false, timeout(verb),
                            CreateCall.Type.SIP, accountId);
                } else if (Nouns.SIP.equals(child.name())) {
                    // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                    String username = null;
                    String password = null;
                    if (child.attribute("username") != null) {
                        username = child.attribute("username").value();
                    }
                    if (child.attribute("password") != null) {
                        password = child.attribute("password").value();
                    }
                    create = new CreateCall(e164(callerId(verb)), e164(child.text()), username, password, false, timeout(verb),
                            CreateCall.Type.SIP, accountId);
                }
                callManager.tell(create, source);
            } else {
                // Fork.
                final Fork fork = Fork.instance();
                source.tell(fork, source);
                dialChildren = null;
            }
        }
    }

    private final class Forking extends AbstractDialAction {
        public Forking(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (CallManagerResponse.class.equals(klass)) {
                final CallManagerResponse<ActorRef> response = (CallManagerResponse<ActorRef>) message;
                outboundCall = response.get();
                outboundCall.tell(new Observe(source), source);
                outboundCall.tell(new Dial(), source);
            } else if (Fork.class.equals(klass)) {
                final Observe observe = new Observe(source);
                final Dial dial = new Dial();
                for (final ActorRef branch : dialBranches) {
                    branch.tell(observe, source);
                    branch.tell(dial, source);
                }
            }
            String path = configuration.subset("runtime-settings").getString("prompts-uri");
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += "ringing.wav";
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
            final Play play = new Play(uri, Short.MAX_VALUE);
            callMediaGroup.tell(play, source);
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.create(timeout(verb), TimeUnit.SECONDS));
        }
    }

    private final class AcquiringOutboundCallInfo extends AbstractDialAction {
        public AcquiringOutboundCallInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (isForking) {
                dialBranches.remove(outboundCall);
                for (final ActorRef branch : dialBranches) {
                    branch.tell(new Cancel(), source);
                    callManager.tell(new DestroyCall(branch), source);
                }
                dialBranches = null;
            }
            outboundCall.tell(new GetCallInfo(), source);
        }
    }

    private final class JoiningCalls extends AbstractDialAction {
        public JoiningCalls(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final CallResponse<CallInfo> response = (CallResponse<CallInfo>) message;
            outboundCallInfo = response.get();

            logger.info("Checking for tag with attributes for this outboundcall");
            Tag child = dialChildrenWithAttributes.get(outboundCall);
            if (child != null && child.attribute("url") != null) {

                URI url = new URL(child.attribute("url").value()).toURI();
                String method = null;
                if (child.hasAttribute("method")) {
                    method = child.attribute("method").value().toUpperCase();
                } else {
                    method = "POST";
                }

                final SubVoiceInterpreterBuilder builder = new SubVoiceInterpreterBuilder(getContext().system());
                builder.setConfiguration(configuration);
                builder.setStorage(storage);
                builder.setCallManager(self());
                builder.setSmsService(smsService);
                builder.setAccount(accountId);
                builder.setVersion(version);
                builder.setUrl(url);
                builder.setMethod(method);
                final ActorRef interpreter = builder.build();
                StartInterpreter start = new StartInterpreter(outboundCall);
                Timeout expires = new Timeout(Duration.create(600, TimeUnit.SECONDS));
                Future<Object> future = (Future<Object>) ask(interpreter, start, expires);
                Object object = Await.result(future, Duration.create(600, TimeUnit.SECONDS));

                if (!End.class.equals(object.getClass())) {
                    fsm.transition(message, hangingUp);
                    return;
                }

            }

            final AddParticipant add = new AddParticipant(outboundCall);
            call.tell(add, source);
        }
    }

    private final class Bridged extends AbstractDialAction {
        public Bridged(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            callMediaGroup.tell(new Stop(), source);
            final int timeLimit = timeLimit(verb);
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.create(timeLimit, TimeUnit.SECONDS));
        }
    }

    private final class FinishDialing extends AbstractDialAction {
        public FinishDialing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final State state = fsm.state();
            if (message instanceof ReceiveTimeout) {
                if (forking.equals(state)) {
                    final UntypedActorContext context = getContext();
                    context.setReceiveTimeout(Duration.Undefined());
                    for (final ActorRef branch : dialBranches) {
                        branch.tell(new Cancel(), source);
                        callManager.tell(new DestroyCall(branch), source);
                    }
                    dialBranches = null;
                } else if (bridged.equals(state)) {
                    outboundCall.tell(new Hangup(), source);
                }
            }
            // Parses "action".
            final NotificationsDao notifications = storage.getNotificationsDao();
            Attribute attribute = verb.attribute("action");
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
                    final URI uri = resolve(base, target);
                    // Parse "method".
                    String method = "POST";
                    attribute = verb.attribute("method");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 13210, method
                                        + " is not a valid HTTP method for <Dial>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Redirect to the action url.
                    final List<NameValuePair> parameters = parameters();
                    request = new HttpRequestDescriptor(uri, method, parameters);
                    downloader.tell(request, source);
                    dialChildren = null;
                    outboundCall = null;
                    return;
                }
            }
            // Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
            dialChildren = null;
            outboundCall = null;
        }
    }

    private final class AcquiringConferenceInfo extends AbstractDialAction {
        public AcquiringConferenceInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final ConferenceCenterResponse response = (ConferenceCenterResponse) message;
            conference = response.get();
            final GetConferenceInfo request = new GetConferenceInfo();
            conference.tell(new Observe(source), source);
            conference.tell(request, source);
        }
    }

    private final class AcquiringConferenceMediaGroup extends AbstractDialAction {
        public AcquiringConferenceMediaGroup(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final ConferenceResponse<ConferenceInfo> response = (ConferenceResponse<ConferenceInfo>) message;
            conferenceInfo = response.get();
            conferenceState = conferenceInfo.state();
            final Tag child = conference(verb);
            // If there is room join the conference.
            int max = 40;
            Attribute attribute = child.attribute("maxParticipants");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    try {
                        max = Integer.parseInt(value);
                    } catch (final NumberFormatException ignored) {
                    }
                }
            }
            if (conferenceInfo.participants().size() < max) {
                final CreateMediaGroup request = new CreateMediaGroup();
                conference.tell(request, source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    private final class InitializingConferenceMediaGroup extends AbstractDialAction {
        public InitializingConferenceMediaGroup(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final ConferenceResponse<ActorRef> response = (ConferenceResponse<ActorRef>) message;
            conferenceMediaGroup = response.get();
            conferenceMediaGroup.tell(new Observe(source), source);
            final StartMediaGroup request = new StartMediaGroup();
            conferenceMediaGroup.tell(request, source);
        }
    }

    private final class JoiningConference extends AbstractDialAction {
        public JoiningConference(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Tag child = conference(verb);
            // Play beep.
            boolean beep = true;
            Attribute attribute = child.attribute("beep");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    beep = Boolean.parseBoolean(value);
                }
            }
            if (beep) {
                String path = configuration.subset("runtime-settings").getString("prompts-uri");
                if (!path.endsWith("/")) {
                    path += "/";
                }
                path += "beep.wav";
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
                conferenceMediaGroup.tell(play, source);
            }
            // Join the conference.
            final AddParticipant request = new AddParticipant(call);
            conference.tell(request, source);
        }
    }

    private final class Conferencing extends AbstractDialAction {
        public Conferencing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final NotificationsDao notifications = storage.getNotificationsDao();
            final Tag child = conference(verb);
            // Mute

            Attribute attribute = child.attribute("muted");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    callMuted = Boolean.parseBoolean(value);
                }
            }

            if (callMuted) {
                final Mute mute = new Mute();
                call.tell(mute, source);
            }
            // Parse start conference.

            attribute = child.attribute("startConferenceOnEnter");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    startConferenceOnEnter = Boolean.parseBoolean(value);
                }
            }

            if (!startConferenceOnEnter && conferenceState == ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT) {

                if (!callMuted) {
                    final Mute mute = new Mute();
                    logger.info("Muting the call as startConferenceOnEnter =" + startConferenceOnEnter + " callMuted = "
                            + callMuted);
                    call.tell(mute, source);
                }

                // Parse wait url.
                URI waitUrl = null;
                attribute = child.attribute("waitUrl");
                if (attribute != null) {
                    String value = attribute.value();
                    if (value != null && !value.isEmpty()) {
                        try {
                            waitUrl = URI.create(value);
                        } catch (final Exception exception) {
                            final Notification notification = notification(ERROR_NOTIFICATION, 13233, method
                                    + " is not a valid waitUrl value for <Conference>");
                            notifications.addNotification(notification);
                            sendMail(notification);
                            final StopInterpreter stop = StopInterpreter.instance();
                            source.tell(stop, source);

                            // TODO shouldn't we return here?
                        }
                    }

                    final URI base = request.getUri();
                    waitUrl = resolve(base, waitUrl);
                    // Parse method.
                    String method = "POST";
                    attribute = child.attribute("waitMethod");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 13234, method
                                        + " is not a valid waitMethod value for <Conference>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Start the waitUrl media player.

                    if (waitUrl != null) {
                        final ConfVoiceInterpreterBuilder confVoiceInterpreterBuilder = new ConfVoiceInterpreterBuilder(
                                getContext().system());
                        confVoiceInterpreterBuilder.setAccount(accountId);
                        confVoiceInterpreterBuilder.setCallInfo(callInfo);
                        confVoiceInterpreterBuilder.setConference(conference);
                        confVoiceInterpreterBuilder.setConfiguration(configuration);
                        confVoiceInterpreterBuilder.setEmailAddress(emailAddress);
                        confVoiceInterpreterBuilder.setMethod(method);
                        confVoiceInterpreterBuilder.setStorage(storage);
                        confVoiceInterpreterBuilder.setUrl(waitUrl);
                        confVoiceInterpreterBuilder.setVersion(version);

                        final ActorRef confInterpreter = confVoiceInterpreterBuilder.build();

                        CreateWaitUrlConfMediaGroup createWaitUrlConfMediaGroup = new CreateWaitUrlConfMediaGroup(
                                confInterpreter);
                        conference.tell(createWaitUrlConfMediaGroup, source);
                    }
                }
            } else if (conferenceState == ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT) {
                conference.tell(new ConferenceModeratorPresent(), source);
            }
            // Set timer.
            final int timeLimit = timeLimit(verb);
            final UntypedActorContext context = getContext();
            context.setReceiveTimeout(Duration.create(timeLimit, TimeUnit.SECONDS));
        }
    }

    private final class FinishConferencing extends AbstractDialAction {
        public FinishConferencing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof ReceiveTimeout) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
                final RemoveParticipant remove = new RemoveParticipant(call);
                conference.tell(remove, source);
            }
            // Clean up.\
            callMediaGroup.tell(new StopMediaGroup(), source);
            final DestroyMediaGroup destroy = new DestroyMediaGroup(conferenceMediaGroup);
            conference.tell(destroy, source);
            conferenceMediaGroup = null;
            conference = null;
            // Parse remaining conference attributes.
            final NotificationsDao notifications = storage.getNotificationsDao();
            final Tag child = conference(verb);
            // Parse "endConferenceOnExit"
            boolean endOnExit = false;
            Attribute attribute = child.attribute("endConferenceOnExit");
            if (attribute != null) {
                final String value = attribute.value();
                if (value != null && !value.isEmpty()) {
                    endOnExit = Boolean.parseBoolean(value);
                }
            }
            if (endOnExit) {
                final StopConference stop = new StopConference();
                conference.tell(stop, source);
            }
            // Parse "action".
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
                    final URI uri = resolve(base, target);
                    // Parse "method".
                    String method = "POST";
                    attribute = verb.attribute("method");
                    if (attribute != null) {
                        method = attribute.value();
                        if (method != null && !method.isEmpty()) {
                            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                                final Notification notification = notification(WARNING_NOTIFICATION, 13210, method
                                        + " is not a valid HTTP method for <Dial>");
                                notifications.addNotification(notification);
                                method = "POST";
                            }
                        } else {
                            method = "POST";
                        }
                    }
                    // Redirect to the action url.
                    final List<NameValuePair> parameters = parameters();
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
            if (bridged.equals(state)) {
                if (outboundCall != null) {
                    outboundCall.tell(new StopObserving(source), source);
                    outboundCall.tell(new Hangup(), source);
                }
            }
            // If we still have a conference media group release it.
            final StopMediaGroup stop = new StopMediaGroup();
            if (conferenceMediaGroup != null) {
                conferenceMediaGroup.tell(stop, source);
                final DestroyMediaGroup destroy = new DestroyMediaGroup(conferenceMediaGroup);
                conference.tell(destroy, source);
                conferenceMediaGroup = null;
            }
            // If the call is in a conference remove it.
            if (conference != null) {
                final RemoveParticipant remove = new RemoveParticipant(call);
                conference.tell(remove, source);
            }
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

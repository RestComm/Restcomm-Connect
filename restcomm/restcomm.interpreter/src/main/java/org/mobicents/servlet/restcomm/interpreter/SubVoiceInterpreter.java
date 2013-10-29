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
import org.apache.http.HttpStatus;
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
import org.mobicents.servlet.restcomm.interpreter.rcml.Parser;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.interpreter.rcml.Verbs;
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
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Cancel;
import org.mobicents.servlet.restcomm.telephony.Collect;
import org.mobicents.servlet.restcomm.telephony.CreateMediaGroup;
import org.mobicents.servlet.restcomm.telephony.DestroyCall;
import org.mobicents.servlet.restcomm.telephony.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.Hangup;
import org.mobicents.servlet.restcomm.telephony.MediaGroupResponse;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStatus;
import org.mobicents.servlet.restcomm.telephony.Play;
import org.mobicents.servlet.restcomm.telephony.Record;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.telephony.StartMediaGroup;
import org.mobicents.servlet.restcomm.telephony.StopMediaGroup;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;
import org.mobicents.servlet.restcomm.util.WavUtils;

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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author gvagenas@telestax.com
 */
public final class SubVoiceInterpreter extends UntypedActor {
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
	private final State hangingUp;
	private final State finished;
	private final State checkingMediaGroupState;
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

	private ActorRef outboundCall;
	private CallInfo outboundCallInfo;
	// State for the gather verb.
	private List<Tag> gatherChildren;
	private List<URI> gatherPrompts;
	// The call recording stuff.
	private Sid recordingSid;
	private URI recordingUri;
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
	private DownloaderResponse downloaderResponse;
	// The RCML parser.
	private ActorRef parser;
	private ActorRef source;
	private Tag verb;

	private Boolean hangupOnEnd;

	private ActorRef originalInterpreter;

	public SubVoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone,
			final String version, final URI url, final String method, final URI fallbackUrl,
			final String fallbackMethod, final URI statusCallback, final String statusCallbackMethod,
			final String emailAddress, final ActorRef callManager, final ActorRef conferenceManager,
			final ActorRef sms, final DaoManager storage) {

		this(configuration, account, phone, version, url, method,
				fallbackUrl, fallbackMethod, statusCallback, statusCallbackMethod, emailAddress,
				callManager, conferenceManager, sms, storage, false);
	}

	public SubVoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone,
			final String version, final URI url, final String method, final URI fallbackUrl,
			final String fallbackMethod, final URI statusCallback, final String statusCallbackMethod,
			final String emailAddress, final ActorRef callManager, final ActorRef conferenceManager,
			final ActorRef sms, final DaoManager storage, final Boolean hangupOnEnd) {
		super();
		source = self();
		uninitialized = new State("uninitialized", null, null);
		acquiringAsrInfo = new State("acquiring asr info",
				new AcquiringAsrInfo(source), null);
		acquiringSynthesizerInfo = new State("acquiring tts info",
				new AcquiringSpeechSynthesizerInfo(source), null);
		acquiringCallInfo = new State("acquiring call info",
				new AcquiringCallInfo(source), null);
		acquiringCallMediaGroup = new State("acquiring call media group",
				new AcquiringCallMediaGroup(source), null);
		downloadingRcml = new State("downloading rcml",
				new DownloadingRcml(source), null);
		initializingCallMediaGroup = new State("initializing call media group",
				new InitializingCallMediaGroup(source), null);
		ready = new State("ready", new Ready(source), null);
		notFound = new State("notFound", new NotFound(source), null);
		rejecting = new State("rejecting", new Rejecting(source), null);
		playingRejectionPrompt = new State("playing rejection prompt",
				new PlayingRejectionPrompt(source), null);
		pausing = new State("pausing", new Pausing(source), null);
		caching = new State("caching", new Caching(source), null);
		checkingCache = new State("checkingCache", new CheckCache(source), null);
		playing = new State("playing", new Playing(source), null);
		synthesizing = new State("synthesizing", new Synthesizing(source), null);
		redirecting = new State("redirecting", new Redirecting(source), null);
		faxing = new State("faxing", new Faxing(source), null);
		gathering = new State("gathering", new Gathering(source), null);
		processingGatherChildren = new State("processing gather children",
				new ProcessingGatherChildren(source), null);
		finishGathering = new State("finish gathering",
				new FinishGathering(source), null);
		creatingRecording = new State("creating recording",
				new CreatingRecording(source), null);
		finishRecording = new State("finish recording", 
				new FinishRecording(source), null);
		creatingSmsSession = new State("creating sms session",
				new CreatingSmsSession(source), null);
		sendingSms = new State("sending sms", new SendingSms(source), null);

		hangingUp = new State("hanging up", new HangingUp(source), null);
		finished = new State("finished", new Finished(source), null);
		checkingMediaGroupState = new State("checkingMediaGroupState", new CheckMediaGroupState(source), null);

		// Initialize the transitions for the FSM.
		final Set<Transition> transitions = new HashSet<Transition>();
		transitions.add(new Transition(uninitialized, acquiringAsrInfo));
		transitions.add(new Transition(acquiringAsrInfo, acquiringSynthesizerInfo));
		transitions.add(new Transition(acquiringAsrInfo, finished));
		transitions.add(new Transition(acquiringSynthesizerInfo, acquiringCallInfo));
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
		transitions.add(new Transition(sendingSms, hangingUp));
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
		if(!path.endsWith("/")) {
			path = path + "/";
		}
		path = path + accountId.toString();
		cachePath = path;
		String uri = runtime.getString("cache-uri");
		if(!uri.endsWith("/")) {
			uri = uri + "/";
		}
		uri = uri + accountId.toString();
		this.cache = cache(path, uri);
		this.downloader = downloader();
		this.hangupOnEnd = hangupOnEnd;
	}

	private ActorRef asr(final Configuration configuration) {
		final UntypedActorContext context = getContext();
		return context.actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;
			@Override public Actor create() throws Exception {
				return new ISpeechAsr(configuration);
			}
		}));
	}

	@SuppressWarnings("unchecked")
	private void asrResponse(final Object message) {
		final Class<?> klass = message.getClass();
		if(AsrResponse.class.equals(klass)) {
			final AsrResponse<String> response = (AsrResponse<String>)message;
			Transcription transcription = (Transcription)response.attributes().get("transcription");
			if(response.succeeded()) {
				transcription = transcription.setStatus(Transcription.Status.COMPLETED);
				transcription = transcription.setTranscriptionText(response.get());
			} else {
				transcription = transcription.setStatus(Transcription.Status.FAILED);
			}
			final TranscriptionsDao transcriptions = storage.getTranscriptionsDao();
			transcriptions.updateTranscription(transcription);
			// Notify the callback listener.
			final Object attribute = response.attributes().get("callback");
			if(attribute != null) {
				final URI callback = (URI)attribute;
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
			@Override public Actor create() throws Exception {
				return new InterfaxService(configuration);
			}
		}));
	}

	private void callback() {
		if(statusCallback != null) {
			if(statusCallbackMethod == null) {
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
			@Override public UntypedActor create() throws Exception {
				return new DiskCache(path, uri, true);
			}
		}));
	}

	private ActorRef downloader() {
		final UntypedActorContext context = getContext();
		return context.actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;
			@Override public UntypedActor create() throws Exception {
				return new Downloader();
			}
		}));
	}

	private String e164(final String number) {
		final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
		try {
			final PhoneNumber result = numbersUtil.parse(number, "US");
			return numbersUtil.format(result, PhoneNumberFormat.E164);
		} catch(final NumberParseException ignored) {
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
			@Override public UntypedActor create() throws Exception {
				return new MailMan(configuration);
			}
		}));
	}

	private Notification notification(final int log, final int error,
			final String message) {
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
		if(!base.endsWith("/")) {
			buffer.append("/");
		}
		buffer.append(error).append(".html");
		final URI info = URI.create(buffer.toString());
		builder.setMoreInfo(info);
		builder.setMessageText(message);
		final DateTime now = DateTime.now();
		builder.setMessageDate(now);
		if(request != null) {
			builder.setRequestUrl(request.getUri());
			builder.setRequestMethod(request.getMethod());
			builder.setRequestVariables(request.getParametersAsString());
		}
		if(response != null) {
			builder.setResponseHeaders(response.getHeadersAsString());
			final String type = response.getContentType();
			if(type.contains("text/xml") || type.contains("application/xml") ||
					type.contains("text/html")) {
				try {
					builder.setResponseBody(response.getContentAsString());
				} catch(final IOException exception) {
					logger.error("There was an error while reading the contents of the resource " +
							"located @ " + url.toString(), exception);
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
	@Override public void onReceive(final Object message) throws Exception {
		final Class<?> klass = message.getClass();
		final State state = fsm.state();
		final ActorRef sender = sender();

		if(logger.isInfoEnabled()) {
			logger.info(" ********** SubVoiceInterpreter's Current State: " + state.toString());
			logger.info(" ********** SubVoiceInterpreter's Processing Message: " + klass.getName());
		}

		if(StartInterpreter.class.equals(klass)) {
			originalInterpreter = sender;
			fsm.transition(message, acquiringAsrInfo);
		} 
		else if(AsrResponse.class.equals(klass)) {
			if(outstandingAsrRequests > 0){
				asrResponse(message);
			} else {
			fsm.transition(message, acquiringSynthesizerInfo);
			}
		} 
		else if(SpeechSynthesizerResponse.class.equals(klass)) {
			if(acquiringSynthesizerInfo.equals(state)) {
				fsm.transition(message, acquiringCallInfo);
			} else if(synthesizing.equals(state)) {
				final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>)message;
				if(response.succeeded()) {
					fsm.transition(message, caching);
				} else {
					fsm.transition(message, hangingUp);
				}
			} else if(processingGatherChildren.equals(state)) {
				final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>)message;
				if(response.succeeded()) {
					fsm.transition(message, processingGatherChildren);
				} else {
					fsm.transition(message, hangingUp);
				}
			}
		} 
		else if(CallResponse.class.equals(klass)) {
			if(acquiringCallInfo.equals(state)) {
				final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
				callInfo = response.get();
				fsm.transition(message, downloadingRcml);
			} else if(acquiringCallMediaGroup.equals(state)) {
				fsm.transition(message, checkingMediaGroupState);
			} 
		} 	
		else if(DownloaderResponse.class.equals(klass)) {
			downloaderResponse = (DownloaderResponse)message;
			if(logger.isDebugEnabled()) {
				logger.debug("response succeeded " + downloaderResponse.succeeded() + ", statusCode " + downloaderResponse.get().getStatusCode());
			}
			if(downloaderResponse.succeeded() && HttpStatus.SC_OK == downloaderResponse.get().getStatusCode()) {
				fsm.transition(message, acquiringCallMediaGroup);
			} else if(downloaderResponse.succeeded() && HttpStatus.SC_NOT_FOUND == downloaderResponse.get().getStatusCode()) {
				fsm.transition(message, notFound);
			} 
		}	
		else if(MediaGroupStateChanged.class.equals(klass)) {
			final MediaGroupStateChanged event = (MediaGroupStateChanged)message;
			if(MediaGroupStateChanged.State.ACTIVE == event.state()) {
				if(initializingCallMediaGroup.equals(state) || checkingMediaGroupState.equals(state)) {
					fsm.transition(message, ready);
				} if (ready.equals(state)){
					if(reject.equals(verb.name())) {
						fsm.transition(message, playingRejectionPrompt);
					} else if(fax.equals(verb.name())) {
						fsm.transition(message, caching);
					} else if(play.equals(verb.name())) {
						fsm.transition(message, caching);
					} else if(say.equals(verb.name())) {
						fsm.transition(message, checkingCache);
					} else if(gather.equals(verb.name())) {
						fsm.transition(message, processingGatherChildren);
					} else if(pause.equals(verb.name())) {
						fsm.transition(message, pausing);
					} else if(hangup.equals(verb.name())) {
						fsm.transition(message, hangingUp);
					} else if(redirect.equals(verb.name())) {
						fsm.transition(message, redirecting);
					} else if(record.equals(verb.name())) {
						fsm.transition(message, creatingRecording);
					} else if(sms.equals(verb.name())) {
						fsm.transition(message, creatingSmsSession);
					} else {
						invalidVerb(verb);
					}
				}
			}else if(MediaGroupStateChanged.State.INACTIVE == event.state()) {
				if(checkingMediaGroupState.equals(state)){
					fsm.transition(message, initializingCallMediaGroup);
				} else if(!hangingUp.equals(state)) {
					fsm.transition(message, hangingUp);
				}
			}
		}

		else if(DiskCacheResponse.class.equals(klass)) {
			final DiskCacheResponse response = (DiskCacheResponse)message;
			if(response.succeeded()) {
				if(caching.equals(state) || checkingCache.equals(state)) {
					if(play.equals(verb.name()) || say.equals(verb.name())) {
						fsm.transition(message, playing);
					} else if(fax.equals(verb.name())) {
						fsm.transition(message, faxing);
					}
				} else if(processingGatherChildren.equals(state)) {
					fsm.transition(message, processingGatherChildren);
				}
			} else {
				if(checkingCache.equals(state)){
					fsm.transition(message, synthesizing);
				} else {
					fsm.transition(message, hangingUp);
				}
			}
		} 
		else if(Tag.class.equals(klass)) {
			verb = (Tag)message;

			if(Verbs.dial.equals(verb.name()))
				originalInterpreter.tell(new Exception("Dial verb not supported"), source);

			if(reject.equals(verb.name())) {
				fsm.transition(message, rejecting);
			} else if(pause.equals(verb.name())) {
				fsm.transition(message, pausing);
            } else if(fax.equals(verb.name())) {
				fsm.transition(message, caching);
			} else if(play.equals(verb.name())) {
				fsm.transition(message, caching);
			} else if(say.equals(verb.name())) {
				fsm.transition(message, checkingCache);
			} else if(gather.equals(verb.name())) {
				fsm.transition(message, processingGatherChildren);
			} else if(pause.equals(verb.name())) {
				fsm.transition(message, pausing);
			} else if(hangup.equals(verb.name())) {
				fsm.transition(message, hangingUp);
			} else if(redirect.equals(verb.name())) {
				fsm.transition(message, redirecting);
			} else if(record.equals(verb.name())) {
				fsm.transition(message, creatingRecording);
			} else if(sms.equals(verb.name())) {
				fsm.transition(message, creatingSmsSession);
			} else {
				invalidVerb(verb);
			}
		} 
		else if(End.class.equals(klass)) {
			if(!hangupOnEnd) {
				originalInterpreter.tell(message, source);
			} else {
				fsm.transition(message, hangingUp);
			}
		} 
		else if(StartGathering.class.equals(klass)) {
			fsm.transition(message, gathering);
		}  
		else if(CallStateChanged.class.equals(klass)) {
			final CallStateChanged event = (CallStateChanged)message;
			if(CallStateChanged.State.NO_ANSWER == event.state() ||
					CallStateChanged.State.COMPLETED == event.state() ||
					CallStateChanged.State.FAILED == event.state() ||
					CallStateChanged.State.BUSY == event.state()) {

				originalInterpreter.tell(new Cancel(), source);
			}
		}
		else if(MediaGroupResponse.class.equals(klass)) {
			final MediaGroupResponse<String> response = (MediaGroupResponse<String>)message;
			if(response.succeeded()) {
				if(playingRejectionPrompt.equals(state)) {
					originalInterpreter.tell(message, source);
				} else if(playing.equals(state)) {
					fsm.transition(message, ready);
				} else if(creatingRecording.equals(state)) {
					fsm.transition(message, finishRecording);
				} else if(gathering.equals(state)) {
					fsm.transition(message, finishGathering);
				} 
			} else {
				originalInterpreter.tell(message, source);
			}
		} else if(SmsServiceResponse.class.equals(klass)) {
			final SmsServiceResponse<ActorRef> response = (SmsServiceResponse<ActorRef>)message;
			if(response.succeeded()) {
				if(creatingSmsSession.equals(state)) {
					fsm.transition(message, sendingSms);
				}
			} else {
				fsm.transition(message, hangingUp);
			}
		} 
//		else if(AsrResponse.class.equals(klass)) {
//			asrResponse(message);
//		} 
		else if(SmsSessionResponse.class.equals(klass)) {
			smsResponse(message);
		} else if(FaxResponse.class.equals(klass)) {
			fsm.transition(message, ready);
		} else if(StopInterpreter.class.equals(klass)) {
			if(CallStateChanged.State.IN_PROGRESS == callState) {
				fsm.transition(message, hangingUp);
			} else {
				fsm.transition(message, finished);
			}
		} 
		else if(message instanceof ReceiveTimeout) {
			if(pausing.equals(state)) {
				fsm.transition(message, ready);
			}
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
		return parameters;
	}

	private ActorRef parser(final String xml) {
		final UntypedActorContext context = getContext();
		return context.actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;
			@Override public UntypedActor create() throws Exception {
				return new Parser(xml);
			}
		}));
	}

	private void postCleanup() {
		final ActorRef self = self();
		if(smsSessions.isEmpty() && outstandingAsrRequests == 0) {
			final UntypedActorContext context = getContext();
			context.stop(self);
		}
	}

	private URI resolve(final URI base, final URI uri) {
		if(base.equals(uri)) {
			return uri;
		} else {
			if(!uri.isAbsolute()) {
				return base.resolve(uri);
			} else {
				return uri;
			}
		}
	}

	private void sendMail(final Notification notification) {
		if(emailAddress == null || emailAddress.isEmpty()) {
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
		if(SmsSessionResponse.class.equals(klass)) {
			final SmsSessionResponse response = (SmsSessionResponse)message;
			final SmsSessionInfo info = response.info();
			SmsMessage record = (SmsMessage)info.attributes().get("record");
			if(response.succeeded()) {
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
			if(attribute != null) {
				final URI callback = (URI)attribute;
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
			@Override public Actor create() throws Exception {			  
				return (UntypedActor)Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
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

	private final class  AcquiringAsrInfo extends AbstractAction {
		public AcquiringAsrInfo(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final StartInterpreter request = (StartInterpreter)message;
			call = request.resource();
			asrService.tell(new GetAsrInfo(), source);
		}
	}

	private final class AcquiringSpeechSynthesizerInfo extends AbstractAction {
		public AcquiringSpeechSynthesizerInfo(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings({ "unchecked" })
		@Override public void execute(final Object message) throws Exception {
			final AsrResponse<AsrInfo> response = (AsrResponse<AsrInfo>)message;
			asrInfo = response.get();
			synthesizer.tell(new GetSpeechSynthesizerInfo(), source);
		}
	}

	private final class AcquiringCallInfo extends AbstractAction {
		public AcquiringCallInfo(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings({ "unchecked" })
		@Override public void execute(final Object message) throws Exception {
			final SpeechSynthesizerResponse<SpeechSynthesizerInfo> response =
					(SpeechSynthesizerResponse<SpeechSynthesizerInfo>)message;
			synthesizerInfo = response.get();
			call.tell(new Observe(source), source);
			call.tell(new GetCallInfo(), source);
		}
	}

	private final class AcquiringCallMediaGroup extends AbstractAction {
		public AcquiringCallMediaGroup(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
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
			if(CallResponse.class.equals(klass)) {
				final CallResponse<ActorRef> response = (CallResponse<ActorRef>)message;
				callMediaGroup = response.get();
				//Ask CallMediaGroup to add us as Observer, if the callMediaGroup is active we will not reach InitializingCallMediaGroup where
				//we were adding SubVoiceInterpreter as an observer. Better do it here.
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
		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(MediaGroupStateChanged.class.equals(klass)) {
				//				final CallResponse<ActorRef> response = (CallResponse<ActorRef>)message;
				//				callMediaGroup = response.get();
				//				callMediaGroup.tell(new Observe(source), source);
				callMediaGroup.tell(new StartMediaGroup(), source);
			} else if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
		}
	}

	private final class DownloadingRcml extends AbstractAction {
		public DownloadingRcml(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			final CallDetailRecordsDao records = storage.getCallDetailRecordsDao();
			if(CallResponse.class.equals(klass)) {
				final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
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

		@Override public void execute(final Object message) throws Exception {
			final UntypedActorContext context = getContext();
			final State state = fsm.state();
			if(parser == null){
				response = downloaderResponse.get();

				final String type = response.getContentType();
				if(type.contains("text/xml") || type.contains("application/xml") ||
						type.contains("text/html")) {
					parser = parser(response.getContentAsString());
				} else if(type.contains("audio/wav") || type.contains("audio/wave") ||
						type.contains("audio/x-wav")) {
					parser = parser("<Play>" + request.getUri() + "</Play>");
				} else if(type.contains("text/plain")) {
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			final DownloaderResponse response = (DownloaderResponse)message;
			if(logger.isDebugEnabled()) {
				logger.debug("response succeeded " + response.succeeded() + ", statusCode " + response.get().getStatusCode());
			}           
			final Notification notification = notification(WARNING_NOTIFICATION, 21402,
					"URL Not Found : " +response.get().getURI());
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
			String reason = "rejected";
			Attribute attribute = verb.attribute("reason");
			if(attribute != null) {
				reason = attribute.value();
				if(reason != null && !reason.isEmpty()) {
					if("rejected".equalsIgnoreCase(reason)) {
						reason = "rejected";
					} else if("busy".equalsIgnoreCase(reason)) {
						reason = "busy";
					} else {
						reason = "rejected";
					}
				} else {
					reason = "rejected";
				}
			}
			// Reject the call.
			if("rejected".equals(reason)) {
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

		@Override public void execute(final Object message) throws Exception {
			String path = configuration.subset("runtime-settings").getString("prompts-uri");
			if(!path.endsWith("/")) {
				path += "/";
			}
			path += "reject.wav";
			URI uri = null;
			try {
				uri = URI.create(path);
			} catch(final Exception exception) {
				final Notification notification = notification(ERROR_NOTIFICATION, 12400,
						exception.getMessage());
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

		@Override public void execute(Object message) throws Exception {
			final DiskCacheResponse response = (DiskCacheResponse)message;
			// Parse "from".
			String from = callInfo.to();
			Attribute attribute = verb.attribute("from");
			if(attribute != null) {
				from = attribute.value();
				if(from != null && from.isEmpty()) {
					from = e164(from);
					if(from == null) {
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
			if(attribute != null) {
				to = attribute.value();
				if(to != null && !to.isEmpty()) {
					to = e164(to);
					if(to == null) {
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
			int length = 1;
			final Attribute attribute = verb.attribute("length");
			if(attribute != null) {
				final String number = attribute.value();
				if(number != null && !number.isEmpty()) {
					try {
						length = Integer.parseInt(number);
					} catch(final NumberFormatException exception) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13910,
								"Invalid length value.");
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
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			} 
			//		  else {
			//			  logger.info("Can't check cache, message not verb. Moving to the next verb");
			////			  final GetNextVerb next = GetNextVerb.instance();
			////			  parser.tell(next, source);
			//			  return;
			//		  }
			String hash = hash(verb);
			DiskCacheRequest request = new DiskCacheRequest(hash);
			if(logger.isErrorEnabled()) {
				logger.info("Checking cache for hash: "+hash);
			}
			cache.tell(request, source);
		}
	}

	private final class Caching extends AbstractAction {
		public Caching(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(SpeechSynthesizerResponse.class.equals(klass)) {
				final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>)message;
				final DiskCacheRequest request = new DiskCacheRequest(response.get());
				cache.tell(request, source);
			} else if(Tag.class.equals(klass) || MediaGroupStateChanged.class.equals(klass)) {
				if(Tag.class.equals(klass)) {
					verb = (Tag)message;
				}
				// Parse the URL.
				final String text = verb.text();
				if(text != null && !text.isEmpty()) {
					// Try to cache the media.
					URI target = null;
					try {
						target = URI.create(text);
					} catch(final Exception exception) {
						final Notification notification = notification(ERROR_NOTIFICATION, 11100,
								text + " is an invalid URI.");
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(DiskCacheResponse.class.equals(klass)) {
				// Parse the loop attribute.
				int loop = 1;
				final Attribute attribute = verb.attribute("loop");
				if(attribute != null) {
					final String number = attribute.value();
					if(number != null && !number.isEmpty()) {
						try {
							loop = Integer.parseInt(number);
						} catch(final NumberFormatException ignored) {
							final NotificationsDao notifications = storage.getNotificationsDao();
							Notification notification = null;
							if(say.equals(verb.name())) {
								notification = notification(WARNING_NOTIFICATION, 13510,
										loop + " is an invalid loop value.");
								notifications.addNotification(notification);
							} else if(play.equals(verb.name())) {
								notification = notification(WARNING_NOTIFICATION, 13410,
										loop + " is an invalid loop value.");
								notifications.addNotification(notification);
							}
						}
					}
				}
				final DiskCacheResponse response = (DiskCacheResponse)message;
				final Play play = new Play(response.get(), loop);
				callMediaGroup.tell(play, source);
			}
		}
	}


	private String hash(Object message){
		Map<String,String> details = getSynthesizeDetails(message);
		if(details == null){
			if(logger.isInfoEnabled()) {
				logger.info("Cannot generate hash, details are null");
			}
			return null;
		}
		String voice = details.get("voice");
		String language = details.get("language");
		String text = details.get("text");
		return HashGenerator.hashMessage(voice, language, text);
	}

	private Map<String, String> getSynthesizeDetails(final Object message){
		final Class<?> klass = message.getClass();

		Map<String, String> details = new HashMap<String,String>();

		if(Tag.class.equals(klass)) {
			verb = (Tag)message;
		} else {
			return null;
		}
		if(!say.equals(verb.name()))
			return null;

		// Parse the voice attribute.
		String voice = "man";
		Attribute attribute = verb.attribute("voice");
		if(attribute != null) {
			voice = attribute.value();
			if(voice != null && !voice.isEmpty()) {
				if(!"man".equals(voice) && !"woman".equals(voice)) {
					final Notification notification = notification(WARNING_NOTIFICATION, 13511,
							voice + " is an invalid voice value.");
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
		if(attribute != null) {
			language = attribute.value();
			if(language != null && !language.isEmpty()) {
				if(!synthesizerInfo.languages().contains(language)) {
					language = "en";
				}
			} else {
				language = "en";
			}
		}
		// Synthesize.
		String text = verb.text();

		details.put("voice", voice);
		details.put("language",language);
		details.put("text", text);

		return details;

	}

	private final class Synthesizing extends AbstractAction {
		public Synthesizing(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {

			final Class<?> klass = message.getClass();

			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}

			Map<String,String> details = getSynthesizeDetails(verb);
			if(details !=null && !details.isEmpty()){
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
			// Hang up the call.
			call.tell(new Hangup(), source);
		}
	}

	private final class Redirecting extends AbstractAction {
		public Redirecting(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
			final NotificationsDao notifications = storage.getNotificationsDao();
			String method = "POST";
			Attribute attribute = verb.attribute("method");
			if(attribute != null) {
				method = attribute.value();
				if(method != null && !method.isEmpty()) {
					if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13710,
								method + " is not a valid HTTP method for <Redirect>");
						notifications.addNotification(notification);
						method = "POST";
					}
				} else {
					method = "POST";
				}
			}
			final String text = verb.text();
			if(text != null && !text.isEmpty()) {
				// Try to redirect.
				URI target = null;
				try {
					target = URI.create(text);
				} catch(final Exception exception) {
					final Notification notification = notification(ERROR_NOTIFICATION, 11100,
							text + " is an invalid URI.");
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
			if(attribute != null) {
				finishOnKey = attribute.value();
				if(finishOnKey != null && !finishOnKey.isEmpty()) {
					if(!PATTERN.matcher(finishOnKey).matches()) {
						final NotificationsDao notifications = storage.getNotificationsDao();
						final Notification notification = notification(WARNING_NOTIFICATION, 13310,
								finishOnKey + " is not a valid finishOnKey value");
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
		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			final NotificationsDao notifications = storage.getNotificationsDao();
			if(SpeechSynthesizerResponse.class.equals(klass)) {
				final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>)message;
				final DiskCacheRequest request = new DiskCacheRequest(response.get());
				cache.tell(request, source);
			} else {
				if(Tag.class.equals(klass)) {
					verb = (Tag)message;
					gatherPrompts = new ArrayList<URI>();
					gatherChildren = new ArrayList<Tag>(verb.children());
				} else if(MediaGroupStateChanged.class.equals(klass)) {
					gatherPrompts = new ArrayList<URI>();
					gatherChildren = new ArrayList<Tag>(verb.children());
				} else if(DiskCacheResponse.class.equals(klass)) {
					final DiskCacheResponse response = (DiskCacheResponse)message;
					final URI uri = response.get();
					final Tag child = gatherChildren.remove(0);
					// Parse the loop attribute.
					int loop = 1;
					final Attribute attribute = child.attribute("loop");
					if(attribute != null) {
						final String number = attribute.value();
						if(number != null && !number.isEmpty()) {
							try {
								loop = Integer.parseInt(number);
							} catch(final NumberFormatException ignored) {
								Notification notification = null;
								if(say.equals(child.name())) {
									notification = notification(WARNING_NOTIFICATION, 13322,
											loop + " is an invalid loop value.");
									notifications.addNotification(notification);
								}
							}
						}
					}
					for(int counter = 0; counter < loop; counter++) {
						gatherPrompts.add(uri);
					}
				}
				for(int index = 0; index < gatherChildren.size(); index++) {
					final Tag child = gatherChildren.get(index);
					if(play.equals(child.name())) {
						final String text = child.text();
						if(text != null && !text.isEmpty()) {
							URI target = null;
							try {
								target = URI.create(text);
							} catch(final Exception exception) {
								final Notification notification = notification(ERROR_NOTIFICATION, 13325,
										text + " is an invalid URI.");
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
					} else if(say.equals(child.name())) {
						// Parse the voice attribute.
						String voice = "man";
						Attribute attribute = child.attribute("voice");
						if(attribute != null) {
							voice = attribute.value();
							if(voice != null && !voice.isEmpty()) {
								if(!"man".equals(voice) && !"woman".equals(voice)) {
									final Notification notification = notification(WARNING_NOTIFICATION, 13321,
											voice + " is an invalid voice value.");
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
						if(attribute != null) {
							language = attribute.value();
							if(language != null && !language.isEmpty()) {
								if(!synthesizerInfo.languages().contains(language)) {
									language = "en";
								}
							} else {
								language = "en";
							}
						}
						String text = child.text();
						if(text != null && !text.isEmpty()) {
							final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(voice, language, text);
							synthesizer.tell(synthesize, source);
							break;
						}
					} else if(pause.equals(child.name())) {
						int length = 1;
						final Attribute attribute = child.attribute("length");
						if(attribute != null) {
							final String number = attribute.value();
							if(number != null && !number.isEmpty()) {
								try { length = Integer.parseInt(number); }
								catch(final NumberFormatException ignored) { }
							}
						}
						String path = configuration.subset("runtime-settings").getString("prompts-uri");
						if(!path.endsWith("/")) {
							path += "/";
						}
						path += "one-second-silence.wav";
						final URI uri = URI.create(path);
						for(int counter = 0; counter < length; counter++) {
							gatherPrompts.add(uri);
						}
					}
				}
				// Make sure we don't leave any pauses at the beginning
				// since we can't concurrently modify the list.
				if(!gatherChildren.isEmpty()) {
					Tag child = null;
					do {
						child = gatherChildren.get(0);
						if(child != null) {
							if(pause.equals(child.name())) {
								gatherChildren.remove(0);
							}
						}
					} while(pause.equals(child.name()));
				}
				// Start gathering.
				if(gatherChildren.isEmpty()) {
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

		@Override public void execute(final Object message) throws Exception {
			final NotificationsDao notifications = storage.getNotificationsDao();
			// Parse finish on key.
			String finishOnKey = finishOnKey(verb);
			// Parse the number of digits.
			int numberOfDigits = Short.MAX_VALUE;
			Attribute attribute = verb.attribute("numDigits");
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					try {
						numberOfDigits = Integer.parseInt(value);
					} catch(final NumberFormatException exception) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13314,
								numberOfDigits + " is not a valid numDigits value");
						notifications.addNotification(notification);
					}
				}
			}
			// Parse timeout.
			int timeout = 5;
			attribute = verb.attribute("timeout");
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					try {
						timeout = Integer.parseInt(value);
					} catch(final NumberFormatException exception) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13313,
								timeout + " is not a valid timeout value");
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
		@Override public void execute(final Object message) throws Exception {
			final NotificationsDao notifications = storage.getNotificationsDao();
			final MediaGroupResponse<String> response = (MediaGroupResponse<String>)message;
			// Parses "action".
			Attribute attribute = verb.attribute("action");
			String digits = response.get();
			final String finishOnKey = finishOnKey(verb);
			if(digits.equals(finishOnKey)) {
				digits = "";
			}
			if(logger.isDebugEnabled()) {
				logger.debug("Digits collected : " + digits);
			}
			// https://bitbucket.org/telestax/telscale-restcomm/issue/150/verb-is-looping-by-default-and-never
			// If the 'timeout' is reached before the caller enters any digits, or if the caller enters the 'finishOnKey' value 
			// before entering any other digits, Twilio will not make a request to the 'action' URL but instead continue processing 
			// the current TwiML document with the verb immediately following the <Gather>
			if(attribute != null && (digits != null && !digits.trim().isEmpty())) {
				String action = attribute.value();
				if(action != null && !action.isEmpty()) {
					URI target = null;
					try {
						target = URI.create(action);
					} catch(final Exception exception) {
						final Notification notification = notification(ERROR_NOTIFICATION, 11100,
								action + " is an invalid URI.");
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
					if(attribute != null) {
						method = attribute.value();
						if(method != null && !method.isEmpty()) {
							if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
								final Notification notification = notification(WARNING_NOTIFICATION, 14104,
										method + " is not a valid HTTP method for <Gather>");
								notifications.addNotification(notification);
								method = "POST";
							}
						} else {
							method = "POST";
						}
					}
					// Redirect to the action url.
					if(digits.endsWith(finishOnKey)) {
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

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
			}
			final NotificationsDao notifications = storage.getNotificationsDao();
			String finishOnKey = "1234567890*#";
			Attribute attribute = verb.attribute("finishOnKey");
			if(attribute != null) {
				finishOnKey = attribute.value();
				if(finishOnKey != null && !finishOnKey.isEmpty()) {
					if(!PATTERN.matcher(finishOnKey).matches()) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13613,
								finishOnKey + " is not a valid finishOnKey value");
						notifications.addNotification(notification);
						finishOnKey = "1234567890*#";
					}
				} else {
					finishOnKey = "1234567890*#";
				}
			}
			boolean playBeep = true;
			attribute = verb.attribute("playBeep");
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					playBeep = Boolean.parseBoolean(value);
				}
			}
			int maxLength = 3600;
			attribute = verb.attribute("maxLength");
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					try {
						maxLength = Integer.parseInt(value);
					} catch(final NumberFormatException exception) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13612,
								maxLength + " is not a valid maxLength value");
						notifications.addNotification(notification);
					}
				}
			}
			int timeout = 5;
			attribute = verb.attribute("timeout");
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					try {
						timeout = Integer.parseInt(value);
					} catch(final NumberFormatException exception) {
						final Notification notification = notification(WARNING_NOTIFICATION, 13612,
								timeout + " is not a valid timeout value");
						notifications.addNotification(notification);
					}
				}
			}
			// Start recording.
			recordingSid = Sid.generate(Sid.Type.RECORDING);
			String path = configuration.subset("runtime-settings").getString("recordings-path");
			if(!path.endsWith("/")) {
				path += "/";
			}
			path += recordingSid.toString() + ".wav";
			recordingUri = URI.create(path);
			Record record = null;
			if(playBeep) {
				final List<URI> prompts = new ArrayList<URI>(1);
				path = configuration.subset("runtime-settings").getString("prompts-uri");
				if(!path.endsWith("/")) {
					path += "/";
				}
				path += "beep.wav";
				try {
					prompts.add(URI.create(path));
				} catch(final Exception exception) {
					final Notification notification = notification(ERROR_NOTIFICATION, 12400,
							exception.getMessage());
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
		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(CallStateChanged.class.equals(klass)) {
				final CallStateChanged event = (CallStateChanged)message;
				// Update the interpreter state.
				callState = event.state();
				// Update the storage.
				callRecord = callRecord.setStatus(callState.toString());
				final DateTime end = DateTime.now();
				callRecord = callRecord.setEndTime(end);
				final int seconds = (int)(end.getMillis() -
						callRecord.getStartTime().getMillis()) / 1000;
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
			if(attribute != null) {
				final String value = attribute.value();
				if(value != null && !value.isEmpty()) {
					try {
						transcribeCallback = URI.create(value);
					} catch(final Exception exception) {
						final Notification notification = notification(ERROR_NOTIFICATION, 11100,
								transcribeCallback + " is an invalid URI.");
						notifications.addNotification(notification);
						sendMail(notification);
						final StopInterpreter stop = StopInterpreter.instance();
						source.tell(stop, source);
						return;
					}
				}
			}
			boolean transcribe = false;
			if(transcribeCallback != null) {
				transcribe = true;
			} else {
				attribute = verb.attribute("transcribe");
				if(attribute != null) {
					final String value = attribute.value();
					if(value != null && !value.isEmpty()) {
						transcribe = Boolean.parseBoolean(value);
					}
				}
			}
			if(transcribe) {
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
				} catch(final Exception exception) {				    
					logger.error(exception.getMessage(), exception);
				}
			}
			// If action is present redirect to the action URI.
			attribute = verb.attribute("action");
			if(attribute != null) {
				String action = attribute.value();
				if(action != null && !action.isEmpty()) {
					URI target = null;
					try {
						target = URI.create(action);
					} catch(final Exception exception) {
						final Notification notification = notification(ERROR_NOTIFICATION, 11100,
								action + " is an invalid URI.");
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
					if(attribute != null) {
						method = attribute.value();
						if(method != null && !method.isEmpty()) {
							if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
								final Notification notification = notification(WARNING_NOTIFICATION, 13610,
										method + " is not a valid HTTP method for <Record>");
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
					if(MediaGroupResponse.class.equals(klass)) {
						final MediaGroupResponse<String> response = (MediaGroupResponse<String>)message;
						parameters.add(new BasicNameValuePair("Digits", response.get()));
						request = new HttpRequestDescriptor(uri, method, parameters);
						downloader.tell(request, source);
					} else if(CallStateChanged.class.equals(klass)) {
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
			if(CallStateChanged.class.equals(klass)) {
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

		@Override public void execute(Object message) throws Exception {
			// Save <Sms> verb.
			final Class<?> klass = message.getClass();
			if(Tag.class.equals(klass)) {
				verb = (Tag)message;
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
		@Override public void execute(final Object message) throws Exception {
			final SmsServiceResponse<ActorRef> response = (SmsServiceResponse<ActorRef>)message;
			final ActorRef session = response.get();
			final NotificationsDao notifications = storage.getNotificationsDao();
			// Parse "from".
			String from = callInfo.to();
			Attribute attribute = verb.attribute("from");
			if(attribute != null) {
				from = attribute.value();
				if(from != null && !from.isEmpty()) {
					from = e164(from);
					if(from == null) {
						from = verb.attribute("from").value();
						final Notification notification = notification(ERROR_NOTIFICATION, 14102,
								from + " is an invalid 'from' phone number.");
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
			if(attribute != null) {
				to = attribute.value();
				if(to != null && !to.isEmpty()) {
					to = e164(to);
					if(to == null) {
						to = verb.attribute("to").value();
						final Notification notification = notification(ERROR_NOTIFICATION, 14101,
								to + " is an invalid 'to' phone number.");
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
			if(body == null || body.isEmpty() || body.length() > 160) {
				final Notification notification = notification(ERROR_NOTIFICATION, 14103,
						body + " is an invalid SMS body.");
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
				if(attribute != null) {
					String callback = attribute.value();
					if(callback != null && !callback.isEmpty()) {
						URI target = null;
						try {
							target = URI.create(callback);
						} catch(final Exception exception) {
							final Notification notification = notification(ERROR_NOTIFICATION, 14105,
									callback + " is an invalid URI.");
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
				final SmsSessionRequest sms = new SmsSessionRequest(from , to, body);
				session.tell(sms, source);
				smsSessions.put(sid, session);
			}
			// Parses "action".
			attribute = verb.attribute("action");
			if(attribute != null) {
				String action = attribute.value();
				if(action != null && !action.isEmpty()) {
					URI target = null;
					try {
						target = URI.create(action);
					} catch(final Exception exception) {
						final Notification notification = notification(ERROR_NOTIFICATION, 11100,
								action + " is an invalid URI.");
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
					if(attribute != null) {
						method = attribute.value();
						if(method != null && !method.isEmpty()) {
							if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
								final Notification notification = notification(WARNING_NOTIFICATION, 14104,
										method + " is not a valid HTTP method for <Sms>");
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

	private final class Finished extends AbstractAction {
		public Finished(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(CallStateChanged.class.equals(klass)) {
				final CallStateChanged event = (CallStateChanged)message;
				callState = event.state();
				if (callRecord != null){
					callRecord = callRecord.setStatus(callState.toString());
					final DateTime end = DateTime.now();
					callRecord = callRecord.setEndTime(end);
					final int seconds = (int)(end.getMillis() -
							callRecord.getStartTime().getMillis()) / 1000;
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
			if(callMediaGroup != null) {
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

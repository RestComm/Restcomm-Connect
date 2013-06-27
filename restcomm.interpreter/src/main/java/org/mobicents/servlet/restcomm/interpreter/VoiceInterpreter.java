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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.asr.AsrInfo;
import org.mobicents.servlet.restcomm.asr.AsrResponse;
import org.mobicents.servlet.restcomm.asr.GetAsrInfo;
import org.mobicents.servlet.restcomm.asr.ISpeechAsr;
import org.mobicents.servlet.restcomm.cache.DiskCache;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Direction;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Status;
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
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.*;
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
import org.mobicents.servlet.restcomm.telephony.CreateMediaGroup;
import org.mobicents.servlet.restcomm.telephony.DestroyMediaGroup;
import org.mobicents.servlet.restcomm.telephony.Dial;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.Hangup;
import org.mobicents.servlet.restcomm.telephony.MediaGroupResponse;
import org.mobicents.servlet.restcomm.telephony.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.telephony.Play;
import org.mobicents.servlet.restcomm.telephony.Reject;
import org.mobicents.servlet.restcomm.telephony.StartMediaGroup;
import org.mobicents.servlet.restcomm.tts.AcapelaSpeechSynthesizer;
import org.mobicents.servlet.restcomm.tts.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.SpeechSynthesizerResponse;

import scala.concurrent.duration.Duration;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public final class VoiceInterpreter extends UntypedActor {
  private static final int ERROR_NOTIFICATION = 0;
  private static final int WARNING_NOTIFICATION = 1;
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
  private final State ready;
  private final State acquiringCallMediaGroup;
  private final State rejecting;
  private final State playingRejectionPrompt;
  private final State pausing;
  private final State playing;
  private final State redirecting;
  private final State creatingSmsSession;
  private final State sendingSms;
  private final State hangingUp;
  private final State waitingForOutstandingResponses;
  private final State finished;
  /*
  private final State dialing;
  private final State caching;
  private final State synthesizing;
  private final State gathering;
  private final State recording;
  private final State bridging;
  private final State forking;
  private final State conferencing;
  */
  // FSM.
  private final FiniteStateMachine fsm;
  // The user specific configuration.
  private final Configuration configuration;
  // The block storage cache.
  private final ActorRef cache;
  // The downloader will fetch resources for us using HTTP.
  private final ActorRef downloader;
  // The call manager.
  private final ActorRef callManager;
  // The automatic speech recognition service.
  private final ActorRef asrService;
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
  private final String statusCallbackMethod;
  // application data.
  private HttpRequestDescriptor request;
  private HttpResponseDescriptor response;
  // The RCML parser.
  private ActorRef parser;
  private Tag verb;
	  
  public VoiceInterpreter(final Configuration configuration, final Sid account, final Sid phone,
	  final String version, final URI url, final String method, final URI fallbackUrl,
	  final String fallbackMethod, final URI statusCallback, final String statusCallbackMethod,
	  final ActorRef callManager, final ActorRef sms, final DaoManager storage) {
    super();
    final ActorRef source = self();
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
    downloadingFallbackRcml = new State("downloading fallback rcml",
        new DownloadingFallbackRcml(source), null);
    initializingCall = new State("initializing call",
        new InitializingCall(source), null);
    initializingCallMediaGroup = new State("initializing call media group",
        new InitializingCallMediaGroup(source), null);
    ready = new State("ready", new Ready(source), null);
    rejecting = new State("rejecting", new Rejecting(source), null);
    playingRejectionPrompt = new State("playing rejection prompt",
        new PlayingRejectionPrompt(source), null);
    pausing = new State("pausing", new Pausing(source), null);
    playing = new State("playing", null, null);
    redirecting = new State("redirecting", new Redirecting(source), null);
    creatingSmsSession = new State("creating sms session",
        new CreatingSmsSession(source), null);
    sendingSms = new State("sending sms", new SendingSms(source), null);
    hangingUp = new State("hanging up", new HangingUp(source), null);
    waitingForOutstandingResponses = new State("waiting for outstaning resonses",
        new WaitingForOutstandingResponses(source), null);
    finished = new State("finished", new Finished(source), null);
    /*
    dialing = new State("dialing", null, null);
    synthesizing = new State("synthesizing", null, null);
    
    gathering = new State("gathering", null, null);
    recording = new State("recording", null, null);
    bridging = new State("bridging", null, null);
    conferencing = new State("conferencing", null, null);
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
    transitions.add(new Transition(acquiringCallMediaGroup, initializingCallMediaGroup));
    transitions.add(new Transition(acquiringCallMediaGroup, hangingUp));
    transitions.add(new Transition(acquiringCallMediaGroup, finished));
    transitions.add(new Transition(downloadingRcml, ready));
    transitions.add(new Transition(downloadingRcml, downloadingFallbackRcml));
    transitions.add(new Transition(downloadingRcml, hangingUp));
    transitions.add(new Transition(downloadingRcml, finished));
    transitions.add(new Transition(downloadingFallbackRcml, ready));
    transitions.add(new Transition(downloadingFallbackRcml, hangingUp));
    transitions.add(new Transition(downloadingFallbackRcml, finished));
    transitions.add(new Transition(initializingCall, ready));
    transitions.add(new Transition(initializingCallMediaGroup, playingRejectionPrompt));
    transitions.add(new Transition(initializingCallMediaGroup, hangingUp));
    transitions.add(new Transition(initializingCallMediaGroup, finished));
    transitions.add(new Transition(ready, initializingCall));
    transitions.add(new Transition(ready, pausing));
    transitions.add(new Transition(ready, rejecting));
    transitions.add(new Transition(ready, redirecting));
    transitions.add(new Transition(ready, creatingSmsSession));
    transitions.add(new Transition(ready, hangingUp));
    transitions.add(new Transition(ready, finished));
    transitions.add(new Transition(pausing, ready));
    transitions.add(new Transition(rejecting, acquiringCallMediaGroup));
    transitions.add(new Transition(rejecting, finished));
    transitions.add(new Transition(playingRejectionPrompt, hangingUp));
    transitions.add(new Transition(redirecting, ready));
    transitions.add(new Transition(redirecting, pausing));
    transitions.add(new Transition(redirecting, creatingSmsSession));
    transitions.add(new Transition(redirecting, hangingUp));
    transitions.add(new Transition(redirecting, finished));
    transitions.add(new Transition(creatingSmsSession, sendingSms));
    transitions.add(new Transition(creatingSmsSession, hangingUp));
    transitions.add(new Transition(creatingSmsSession, finished));
    transitions.add(new Transition(sendingSms, ready));
    transitions.add(new Transition(sendingSms, pausing));
    transitions.add(new Transition(sendingSms, redirecting));
    transitions.add(new Transition(sendingSms, hangingUp));
    transitions.add(new Transition(sendingSms, finished));
    transitions.add(new Transition(hangingUp, waitingForOutstandingResponses));
    transitions.add(new Transition(hangingUp, finished));
    transitions.add(new Transition(waitingForOutstandingResponses, waitingForOutstandingResponses));
    transitions.add(new Transition(waitingForOutstandingResponses, finished));
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
    this.configuration = configuration;
    this.callManager = callManager;
    this.asrService = asr(configuration.subset("speech-recognizer"));
    this.faxService = fax(configuration.subset("fax-service"));
    this.smsService = sms;
    this.smsSessions = new HashMap<Sid, ActorRef>();
    this.storage = storage;
    this.synthesizer = tts(configuration.subset("speech-synthesizer"));
    final Configuration runtime = configuration.subset("runtime-settings");
    String path = runtime.getString("cache-path");
    if(!path.endsWith("/")) {
      path = path + "/";
    }
    path = path + accountId.toString();
    String uri = runtime.getString("cache-uri");
    if(!uri.endsWith("/")) {
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
	  @Override public Actor create() throws Exception {
		return new ISpeechAsr(configuration);
	  }
	}));
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
  
  protected String format(final String number) {
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
  
  protected Notification notification(final int log, final int error,
      final String message) {
    final Notification.Builder builder = Notification.builder();
    final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
    builder.setSid(sid);
    builder.setAccountSid(accountId);
    builder.setCallSid(callInfo.sid());
    builder.setApiVersion(version);
    builder.setLog(log);
    builder.setErrorCode(error);
    final String base = configuration.getString("error-dictionary-uri");
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
    if(StartInterpreter.class.equals(klass)) {
      fsm.transition(message, acquiringAsrInfo);
    } else if(AsrResponse.class.equals(klass)) {
      fsm.transition(message, acquiringSynthesizerInfo);
    } else if(SpeechSynthesizerResponse.class.equals(klass)) {
      fsm.transition(message, acquiringCallInfo);
    } else if(CallResponse.class.equals(klass)) {
      if(acquiringCallInfo.equals(state)) {
        final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
        callInfo = response.get();
        final String direction = callInfo.direction();
        if("inbound".equals(direction)) {
          fsm.transition(message, downloadingRcml);
        } else {
          fsm.transition(message, initializingCall);
        }
      } else if(acquiringCallMediaGroup.equals(state)) {
    	fsm.transition(message, initializingCallMediaGroup);
      }
    } else if(CallStateChanged.class.equals(klass)) {
      final CallStateChanged event = (CallStateChanged)message;
      if(CallStateChanged.State.RINGING == event.state()) {
        // update db and callback statusCallback url.
      } else if(CallStateChanged.State.IN_PROGRESS == event.state()) {
        final String direction = callInfo.direction();
        if("inbound".equals(direction)) {
          if(rejecting.equals(state)) {
            fsm.transition(message, acquiringCallMediaGroup);
          } else {
            fsm.transition(message, ready);
          }
        } else {
          fsm.transition(message, downloadingRcml);
        }
      } else if(CallStateChanged.State.NO_ANSWER == event.state() ||
          CallStateChanged.State.COMPLETED == event.state() ||
          CallStateChanged.State.FAILED == event.state()) {
    	if(smsSessions.size() > 0) {
          fsm.transition(message, waitingForOutstandingResponses);
        } else {
          fsm.transition(message, finished);
        }
      }
    } else if(DownloaderResponse.class.equals(klass)) {
	  final DownloaderResponse response = (DownloaderResponse)message;
      if(response.succeeded()) {
        final HttpResponseDescriptor descriptor = response.get();
        if(HttpStatus.SC_OK == descriptor.getStatusCode()) {
          fsm.transition(message, ready);
        } else {
  	      if(downloadingRcml.equals(state)) {
            if(fallbackUrl != null) {
              fsm.transition(message, downloadingFallbackRcml);
            }
          } else {
            fsm.transition(message, finished);
          }
        }
      } else {
        if(downloadingRcml.equals(state)) {
          if(fallbackUrl != null) {
            fsm.transition(message, downloadingFallbackRcml);
          }
        } else {
          fsm.transition(message, finished);
        }
      }
    } else if(Tag.class.equals(klass)) {
      final Tag verb = (Tag)message;
      if(CallStateChanged.State.RINGING == callState) {
        if(!pause.equals(verb.name()) && !reject.equals(verb.name())) {
          fsm.transition(message, initializingCall);
        } else if(reject.equals(verb.name())) {
          fsm.transition(message, rejecting);
        } else if(pause.equals(verb.name())) {
          fsm.transition(message, pausing);
        } else {
          invalidVerb(verb);
        }
      } else if(pause.equals(verb.name())) {
        fsm.transition(message, pausing);
      } else if(hangup.equals(verb.name())) {
        fsm.transition(message, hangingUp);
      } else if(redirect.equals(verb.name())) {
        fsm.transition(message, redirecting);
      } else if(sms.equals(verb.name())) {
        fsm.transition(message, creatingSmsSession);
      } else {
        invalidVerb(verb);
      }
    } else if(End.class.equals(klass)) {
      fsm.transition(message, hangingUp);
    } else if(MediaGroupStateChanged.class.equals(klass)) {
      final MediaGroupStateChanged event = (MediaGroupStateChanged)message;
      if(MediaGroupStateChanged.State.ACTIVE == event.state()) {
    	if(reject.equals(verb.name())) {
          fsm.transition(message, playingRejectionPrompt);
        }
      } else if(MediaGroupStateChanged.State.INACTIVE == event.state()) {
        fsm.transition(message, hangingUp);
      }
    } else if(MediaGroupResponse.class.equals(klass)) {
      final MediaGroupResponse<String> response = (MediaGroupResponse<String>)message;
      System.out.println("************************************* 4 ******************************");
      if(response.succeeded()) {
    	System.out.println("************************************* 5 ******************************");
        if(playingRejectionPrompt.equals(state)) {
          System.out.println("************************************* 6 ******************************");
          fsm.transition(message, hangingUp);
        }
      } else {
        fsm.transition(message, hangingUp);
      }
    } else if(SmsServiceResponse.class.equals(klass)) {
      final SmsServiceResponse<ActorRef> response = (SmsServiceResponse<ActorRef>)message;
      if(response.succeeded()) {
        if(creatingSmsSession.equals(state)) {
          fsm.transition(message, sendingSms);
        }
      } else {
        if(smsSessions.size() > 0) {
          fsm.transition(message, waitingForOutstandingResponses);
        } else {
          fsm.transition(message, hangingUp);
        }
      }
    } else if(SmsSessionResponse.class.equals(klass)) {
      smsResponse(message);
    } else if(StopInterpreter.class.equals(klass)) {
      if(smsSessions.size() > 0) {
        fsm.transition(message, waitingForOutstandingResponses);
      } else {
        if(CallStateChanged.State.IN_PROGRESS == callState) {
          fsm.transition(message, hangingUp);
        } else {
          fsm.transition(message, finished);
        }
      }
    } else if(message instanceof ReceiveTimeout) {
      if(pausing.equals(state)) {
        fsm.transition(message, ready);
      }
    }
  }
  
  protected List<NameValuePair> parameters() {
  	final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    final String callSid = callInfo.sid().toString();
	parameters.add(new BasicNameValuePair("CallSid", callSid));
	final String accountSid = accountId.toString();
	parameters.add(new BasicNameValuePair("AccountSid", accountSid));
	final String from = format(callInfo.from());
    parameters.add(new BasicNameValuePair("From", from));
    final String to = format(callInfo.to());
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
  
  protected URI resolve(final URI base, final URI uri) {
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
      final State state = fsm.state();
      if(waitingForOutstandingResponses.equals(state)) {
        final StopInterpreter stop = StopInterpreter.instance();
        self.tell(stop, self);
      }
    }
  }
  
  private ActorRef tts(final Configuration configuration) {
    final UntypedActorContext context = getContext();
    return context.actorOf(new Props(new UntypedActorFactory() {
	  private static final long serialVersionUID = 1L;
	  @Override public Actor create() throws Exception {
		return new AcapelaSpeechSynthesizer(configuration);
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

  private final class InitializingCall extends AbstractAction {
    public InitializingCall(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings("unchecked")
	@Override public void execute(final Object message) throws Exception {
	  final Class<?> klass = message.getClass();
	  if(CallResponse.class.equals(klass)) {
        final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
        callInfo = response.get();
        callState = callInfo.state();
        call.tell(new Dial(), source);
      } else if(Tag.class.equals(klass)) {
        verb = (Tag)message;
        call.tell(new Answer(), source);
      }
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
  
  private final class InitializingCallMediaGroup extends AbstractAction {
    public InitializingCallMediaGroup(final ActorRef source) {
      super(source);
    }

    @SuppressWarnings("unchecked")
	@Override public void execute(final Object message) throws Exception {
	  final CallResponse<ActorRef> response = (CallResponse<ActorRef>)message;
	  callMediaGroup = response.get();
	  callMediaGroup.tell(new Observe(source), source);
	  callMediaGroup.tell(new StartMediaGroup(), source);
	}
  }
  
  private final class DownloadingRcml extends AbstractAction {
    public DownloadingRcml(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings("unchecked")
	@Override public void execute(final Object message) throws Exception {
	  final Class<?> klass = message.getClass();
	  if(CallResponse.class.equals(klass)) {
	    final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
	    callInfo = response.get();
	    callState = callInfo.state();
	  }
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
	  final StringBuilder buffer = new StringBuilder();
	  buffer.append("/").append(version).append("/Accounts/");
	  buffer.append(accountId.toString()).append("/Calls/");
	  buffer.append(callInfo.sid().toString());
	  final URI uri = URI.create(buffer.toString());
	  builder.setUri(uri);
	  callRecord = builder.build();
	  final CallDetailRecordsDao cdrs = storage.getCallDetailRecordsDao();
	  cdrs.addCallDetailRecord(callRecord);
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

	@Override public void execute(final Object message) throws Exception {
	  final Class<?> klass = message.getClass();
      // Notify the account of the issue.
	  if(DownloaderResponse.class.equals(klass)) {
	    final DownloaderResponse result = (DownloaderResponse)message;
	    final Throwable cause = result.cause();
	    Notification notification = null;
	    if(cause instanceof ClientProtocolException) {
	      notification = notification(ERROR_NOTIFICATION, 11206, cause.getMessage());
	    } else if(cause instanceof IOException) {
	      notification = notification(ERROR_NOTIFICATION, 11205, cause.getMessage());
	    } else if(cause instanceof URISyntaxException) {
	      notification = notification(ERROR_NOTIFICATION, 11100, cause.getMessage());
	    }
	    if(notification != null) {
  	      final NotificationsDao notifications = storage.getNotificationsDao();
  	      notifications.addNotification(notification);
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

	@Override public void execute(final Object message) throws Exception {
	  final UntypedActorContext context = getContext();
	  final State state = fsm.state();
	  if(initializingCall.equals(state)) {
	    final CallStateChanged event = (CallStateChanged)message;
	    callState = event.state();
	    // Handle the pending verb.
	    source.tell(verb, source);
	    return;
	  } else if(downloadingRcml.equals(state) ||
	      downloadingFallbackRcml.equals(state) ||
	      redirecting.equals(state)) {
	    response = ((DownloaderResponse)message).get();
		if(parser != null) {
		  context.stop(parser);
		  parser = null;
		}
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
	  } else if(pausing.equals(state)) {
	    context.setReceiveTimeout(Duration.Undefined());
	  }
	  // Ask the parser for the next action to take.
	  final GetNextVerb next = GetNextVerb.instance();
	  parser.tell(next, source);
	}
  }
  
  private final class Rejecting extends AbstractAction {
    public Rejecting(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      verb = (Tag)message;
      String reason = "rejected";
      Attribute attribute = verb.attribute("reason");
      final String value = attribute.value();
      if(attribute != null) {
        if("rejected".equalsIgnoreCase(value)) {
          reason = "rejected";
        } else if("busy".equalsIgnoreCase(value)) {
          reason = "busy";
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
	  String url = configuration.subset("runtime-settings").getString("prompts-uri");
	  if(!url.endsWith("/")) {
	    url += "/";
	  }
	  url += "reject.wav";
	  URI uri = null;
	  try {
	    uri = URI.create(url);
	  } catch(final Exception exception) {
	    final Notification notification = notification(ERROR_NOTIFICATION, 12400,
	        exception.getMessage());
	    final NotificationsDao notifications = storage.getNotificationsDao();
	    notifications.addNotification(notification);
	    return;
	  }
	  final Play play = new Play(uri, 1);
	  callMediaGroup.tell(play, source);
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
	  // Clean up the call media group if necessary.
	  if(callMediaGroup != null) {
	    final DestroyMediaGroup destroy = new DestroyMediaGroup(callMediaGroup);
	    call.tell(destroy, source);
	    callMediaGroup = null;
	  }
	  // Hang up the call.
	  call.tell(new Hangup(), source);
	}
  }

  private final class Pausing extends AbstractAction {
    public Pausing(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      verb = (Tag)message;
      int length = 1;
      Attribute attribute = verb.attribute("length");
      if(attribute != null) {
        try {
          length = Integer.parseInt(attribute.value());
        } catch(final NumberFormatException exception) {
          final Notification notification = notification(WARNING_NOTIFICATION, 13910,
              "Invalid length value.");
          final NotificationsDao notifications = storage.getNotificationsDao();
          notifications.addNotification(notification);
          return;
        }
      }
      final UntypedActorContext context = getContext();
      context.setReceiveTimeout(Duration.create(length, TimeUnit.SECONDS));
	}
  }
  
  private final class Redirecting extends AbstractAction {
    public Redirecting(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      verb = (Tag)message;
      final NotificationsDao notifications = storage.getNotificationsDao();
      String method = null;
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
      } else {
        method = "POST";
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

  private final class CreatingSmsSession extends AbstractAction {
    public CreatingSmsSession(final ActorRef source) {
      super(source);
    }

	@Override public void execute(Object message) throws Exception {
	  // Save <Sms> verb.
	  verb = (Tag)message;
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
      String from = null;
      Attribute attribute = verb.attribute("from");
      if(attribute == null) {
        from = callInfo.to();
      } else {
	    from = attribute.value();
        if(from == null || from.isEmpty()) {
          from = callInfo.to();
        } else {
          from = format(from);
          if(from == null) {
            from = verb.attribute("from").value();
	        final Notification notification = notification(ERROR_NOTIFICATION, 14102,
                from + " is an invalid 'from' phone number.");
            notifications.addNotification(notification);
            smsService.tell(new DestroySmsSession(session), source);
            final StopInterpreter stop = StopInterpreter.instance();
            source.tell(stop, source);
            return;
          }
        }
      }
      // Parse "to".
      String to = null;
      attribute = verb.attribute("to");
      if(attribute == null) {
        to = callInfo.from();
      } else {
        to = attribute.value();
        if(to == null || to.isEmpty()) {
          to = callInfo.from();
        } else {
          to = format(to);
          if(to == null) {
            to = verb.attribute("to").value();
            final Notification notification = notification(ERROR_NOTIFICATION, 14101,
                to + " is an invalid 'to' phone number.");
            notifications.addNotification(notification);
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
        smsService.tell(new DestroySmsSession(session), source);
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
        builder.setStatus(Status.RECEIVED);
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
              final StopInterpreter stop = StopInterpreter.instance();
              source.tell(stop, source);
              return;
            }
            final URI base = request.getUri();
            final URI uri = resolve(base, target);
            // Parse "method".
            String method = null;
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
            } else {
              method = "POST";
            }
            // Redirect to the action url.
            final List<NameValuePair> parameters = parameters();
            final String status = Status.SENDING.toString();
            parameters.add(new BasicNameValuePair("SmsStatus", status));
            request = new HttpRequestDescriptor(uri, method, parameters);
          } else {
        	// Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
          }
      } else {
    	// Ask the parser for the next action to take.
      	final GetNextVerb next = GetNextVerb.instance();
      	parser.tell(next, source);
      }
	}
  }

  private final class WaitingForOutstandingResponses extends AbstractAction {
    public WaitingForOutstandingResponses(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  final Class<?> klass = message.getClass();
	  if(AsrResponse.class.equals(klass)) {
	    
	  } else if(FaxResponse.class.equals(klass)) {
	    
	  } else if(SmsSessionResponse.class.equals(klass)) {
	    smsResponse(message);
	  }
	}
  }  

  private final class Finished extends AbstractAction {
    public Finished(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      // Stop the interpreter.
      final UntypedActorContext context = getContext();
      context.stop(source);
	}
  }
}

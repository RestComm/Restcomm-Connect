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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.asr.AsrResponse;
import org.mobicents.servlet.restcomm.asr.GetAsrInfo;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.http.client.Downloader;
import org.mobicents.servlet.restcomm.http.client.DownloaderResponse;
import org.mobicents.servlet.restcomm.http.client.HttpRequestDescriptor;
import org.mobicents.servlet.restcomm.http.client.HttpResponseDescriptor;
import org.mobicents.servlet.restcomm.interpreter.rcml.GetNextVerb;
import org.mobicents.servlet.restcomm.interpreter.rcml.Parser;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.Dial;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.tts.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.SpeechSynthesizerResponse;

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
  private final State ready;
  private final State hangingUp;
  private final State redirecting;
  private final State finished;
  /*
  private final State dialing;
  private final State rejecting;
  private final State pausing;
  private final State caching;
  private final State synthesizing;
  private final State playing;
  private final State gathering;
  private final State recording;
  private final State texting;
  private final State bridging;
  private final State forking;
  private final State conferencing;
  */
  // FSM.
  private final FiniteStateMachine fsm;
  // The user specific configuration.
  private final Configuration configuration;
  // The downloader will fetch resources for us using HTTP.
  private final ActorRef downloader;
  // The automatic speech recognition service.
  private final ActorRef asr;
  // The fax service.
  private final ActorRef fax;
  // The SMS service.
  private final ActorRef sms;
  // The storage engine.
  private final DaoManager storage;
  // The text to speech synthesizer service.
  private final ActorRef synthesizer;
  // The languages supported by the automatic speech recognition service.
  private Set<String> asrLanguages;
  // The languages supported by the text to speech synthesizer service. 
  private Set<String> synthesizerLanguages;
  // The call being handled by this interpreter.
  private ActorRef call;
  // The information for this call.
  private CallInfo callInfo;
  // The call state.
  private CallStateChanged.State callState;
  // A call detail record.
  private CallDetailRecord callRecord;
  // Information to reach the application that will be executed
  // by this interpreter.
  private final Sid account;
  private final Sid phone;
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
	  final ActorRef asr, final ActorRef downloader, final ActorRef fax, final ActorRef sms,
      final DaoManager storage, final ActorRef synthesizer) {
    super();
    final ActorRef source = self();
    uninitialized = new State("uninitialized", null, null);
    acquiringAsrInfo = new State("acquiring asr info",
        new AcquiringAsrInfo(source), null);
    acquiringSynthesizerInfo = new State("acquiring tts info",
        new AcquiringSpeechSynthesizerInfo(source), null);
    acquiringCallInfo = new State("acquiring call info",
        new AcquiringCallInfo(source), null);
    downloadingRcml = new State("downloading rcml",
        new DownloadingRcml(source), null);
    downloadingFallbackRcml = new State("downloading fallback rcml",
        new DownloadingFallbackRcml(source), null);
    initializingCall = new State("initializing call",
            new InitializingCall(source), null);
    ready = new State("ready", new Ready(source), null);
    redirecting = new State("redirecting", null, null);
    hangingUp = new State("hanging up", null, null);
    finished = new State("finished", null, null);
    /*
    dialing = new State("dialing", null, null);
    rejecting = new State("rejecting", null, null);
    pausing = new State("pausing", null, null);
    synthesizing = new State("synthesizing", null, null);
    playing = new State("playing", null, null);
    gathering = new State("gathering", null, null);
    recording = new State("recording", null, null);
    texting = new State("texting", null, null);
    bridging = new State("bridging", null, null);
    conferencing = new State("conferencing", null, null);
    */
    // Initialize the transitions for the FSM.
    final Set<Transition> transitions = new HashSet<Transition>();
    // Initialize the FSM.
    this.fsm = new FiniteStateMachine(uninitialized, transitions);
    // Initialize the runtime stuff.
    this.configuration = configuration;
    this.asr = asr;
    this.fax = fax;
    this.sms = sms;
    this.storage = storage;
    this.synthesizer = synthesizer;
    this.downloader = downloader();
    this.account = account;
    this.phone = phone;
    this.version = version;
    this.url = url;
    this.method = method;
    this.fallbackUrl = fallbackUrl;
    this.fallbackMethod = fallbackMethod;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
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
  
  protected void invalidVerb(final Tag verb) {
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
    builder.setAccountSid(account);
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
    buffer.append(account.toString()).append("/Notifications/");
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
      final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
      final CallInfo info = response.get();
      if("inbound".equals(info.direction())) {
        fsm.transition(message, downloadingRcml);
      } else {
        fsm.transition(message, initializingCall);
      }
    } else if(Tag.class.equals(klass)) {
      
    }
  }
  
  protected List<NameValuePair> parameters() {
  	final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    final String callSid = callInfo.sid().toString();
	  parameters.add(new BasicNameValuePair("CallSid", callSid));
	  final String accountSid = account.toString();
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
	  asr.tell(new GetAsrInfo(), source);
	}
  }
  
  private final class AcquiringSpeechSynthesizerInfo extends AbstractAction {
    public AcquiringSpeechSynthesizerInfo(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override public void execute(final Object message) throws Exception {
	  final AsrResponse<Set> response = (AsrResponse<Set>)message;
	  asrLanguages = response.get();
	  synthesizer.tell(new GetSpeechSynthesizerInfo(), source);
	}
  }

  private final class AcquiringCallInfo extends AbstractAction {
    public AcquiringCallInfo(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override public void execute(final Object message) throws Exception {
      final SpeechSynthesizerResponse<Set> response = (SpeechSynthesizerResponse<Set>)message;
      synthesizerLanguages = response.get();
      call.tell(new GetCallInfo(), source);
	}
  }

  private final class InitializingCall extends AbstractAction {
    public InitializingCall(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      final State state = fsm.state();
      if(acquiringCallInfo.equals(state)) {
        call.tell(new Dial(), source);
      } else if(ready.equals(state)) {
        call.tell(new Answer(), source);
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
	  if(CallInfo.class.equals(klass)) {
	    final CallResponse<CallInfo> response = (CallResponse<CallInfo>)message;
	    callInfo = response.get();
	  }
	  // Create a call detail record for the call.
	  final CallDetailRecord.Builder builder = CallDetailRecord.builder();
	  builder.setSid(callInfo.sid());
	  builder.setDateCreated(callInfo.dateCreated());
	  builder.setAccountSid(account);
	  builder.setTo(callInfo.to());
	  builder.setCallerName(callInfo.fromName());
	  builder.setFrom(callInfo.from());
	  builder.setForwardedFrom(callInfo.forwardedFrom());
	  builder.setPhoneNumberSid(phone);
	  builder.setStatus(callState.toString());
	  final DateTime now = DateTime.now();
	  builder.setStartTime(now);
	  builder.setDirection(callInfo.direction());
	  builder.setApiVersion(version);
	  final StringBuilder buffer = new StringBuilder();
	  buffer.append("/").append(version).append("/Accounts/");
	  buffer.append(account.toString()).append("/Calls/");
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
	    if(cause instanceof ClientProtocolException) {
	      notification(ERROR_NOTIFICATION, 11206, cause.getMessage());
	    } else if(cause instanceof IOException) {
	      notification(ERROR_NOTIFICATION, 11205, cause.getMessage());
	    } else if(cause instanceof URISyntaxException) {
	      notification(ERROR_NOTIFICATION, 11100, cause.getMessage());
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
	  response = (HttpResponseDescriptor)message;
	  final UntypedActorContext context = getContext();
	  final State state = fsm.state();
	  if(downloadingRcml.equals(state) ||
	      downloadingFallbackRcml.equals(state) ||
	      redirecting.equals(state)) {
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
	      // Cache the file and then play it using the <Play> verb. FIX ME!
	      parser = parser("<Play>" + request.getUri() + "</Play>");
	    } else if(type.contains("text/plain")) {
	      parser = parser("<Say>" + response.getContentAsString() + "</Say>");
	    } else {
	      final StopInterpreter stop = StopInterpreter.instance();
	      source.tell(stop, source);
	    }
	  }
	  // Ask the parser for the next action to take.
	  final GetNextVerb next = GetNextVerb.instance();
	  parser.tell(next, source);
	}
  }
  
  private final class HangingUp extends AbstractAction {
    public HangingUp(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  
	}
  }
}

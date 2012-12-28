package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.entities.Registration;
import org.mobicents.servlet.sip.restcomm.interpreter.BridgeRcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterFactory;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.VoiceRcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallManagerException;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Client;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Number;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Uri;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.SendDigits;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Url;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class ForkSubStrategy extends VoiceRcmlTagStrategy implements CallObserver, ConferenceObserver {
  private static final Logger logger = Logger.getLogger(ForkSubStrategy.class);
  
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  private final InterpreterFactory interpreterFactory;
  private final PhoneNumberUtil phoneNumberUtil;
  
  private final URI action;
  private final String method;
  private final int timeout;
  private final int timeLimit;
  private final PhoneNumber callerId;
  private final URI ringbackTone;
  private final String ringbackToneMethod;
  private final boolean record;
  private Sid recordingSid;
  
  private Map<String, Map<String, String>> attributes;
  private List<Call> outboundCalls;
  private volatile boolean forking;
  private Call outboundCall;

  public ForkSubStrategy(final URI action, final String method, final int timeout, final int timeLimit, final PhoneNumber callerId,
      final URI ringbackTone, final boolean record) {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    this.callManager = services.get(CallManager.class);
    this.conferenceCenter = services.get(ConferenceCenter.class);
    this.interpreterFactory = services.get(InterpreterFactory.class);
    this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    this.action = action;
    this.method = method;
    this.timeout = timeout;
    this.timeLimit = timeLimit;
    this.callerId = callerId;
    this.ringbackTone = ringbackTone;
    this.ringbackToneMethod = "POST";
    this.record = record;
    if(record) { recordingSid = Sid.generate(Sid.Type.RECORDING); }
    this.attributes = new HashMap<String, Map<String, String>>();
    this.forking = false;
  }

  @Override public synchronized void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final RcmlTag tag) throws TagStrategyException {
	final VoiceRcmlInterpreterContext voiceContext = (VoiceRcmlInterpreterContext)context;
    final Call call = voiceContext.getCall();
    final StringBuilder buffer = new StringBuilder();
	buffer.append(context.getAccountSid().toString()).append(":").append(call.getSid().toString());
	final String room = buffer.toString();
	final Conference bridge = conferenceCenter.getConference(room);
	bridge.addObserver(this);
	if(ringbackTone != null) {
	  interpreterFactory.create(context.getAccountSid(), context.getApiVersion(), ringbackTone,
	      ringbackToneMethod, bridge);
	}
	call.addObserver(this);
    try {
      bridge.addParticipant(call);
      final DateTime start = DateTime.now();
	  outboundCalls = getCalls(tag.getChildren());
	  fork(outboundCalls);
	  try { wait(TimeUtils.SECOND_IN_MILLIS * timeout); }
      catch(final InterruptedException ignored) {
    	  cleanup(outboundCalls);
      }
	  if(Call.Status.IN_PROGRESS == call.getStatus() && (outboundCall != null &&
	      Call.Status.IN_PROGRESS == outboundCall.getStatus())) {
		final Map<String, String> callAttributes = attributes.get(outboundCall.getSid().toString());
		final String digits = callAttributes.get("sendDigits");
		if(digits != null && !digits.isEmpty()) {
		  sendDigits(outboundCall, digits);
		}
		final String url = callAttributes.get("url");
		if(url != null && !url.isEmpty()) {
		  final String method = callAttributes.get("method");
		  try {
			  final URI base = interpreter.getCurrentResourceUri();
			  final BridgeRcmlInterpreter bridgeInterpreter = interpreterFactory.create(context.getAccountSid(),context.getApiVersion(), resolveIfNotAbsolute(base, url),
			      method, outboundCall);
			  bridgeInterpreter.join();
		  } catch(final InterruptedException exception) { }
		}
		if(Call.Status.IN_PROGRESS == call.getStatus() &&
	        Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
		  // Stop the interpreter
		  final RcmlInterpreter conferenceInterpreter = interpreterFactory.remove(bridge.getSid());
		  // Wait for the interpreter to finish before continuing.
		  if(conferenceInterpreter != null) {
		    try { conferenceInterpreter.join(); }
		    catch(final InterruptedException ignored) { }
		  }
          bridge.addParticipant(outboundCall);
          if(record) {
            recordingSid = Sid.generate(Sid.Type.RECORDING);
            final URI destination = toRecordingPath(recordingSid);
            outboundCall.playAndRecord(new ArrayList<URI>(0), destination, -1, TimeUtils.SECOND_IN_MILLIS * timeLimit, null);
          } else {
            try { wait(TimeUtils.SECOND_IN_MILLIS * timeLimit); }
            catch(final InterruptedException exception) { }
          }
		}
        if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
          outboundCall.removeObserver(this);
          outboundCall.hangup();
        }
      } else if(outboundCall != null) {
        outboundCall.removeObserver(this);
	    if(Call.Status.QUEUED == outboundCall.getStatus() ||
	        Call.Status.RINGING == outboundCall.getStatus()) {
	      outboundCall.cancel();
	    } else if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
	      outboundCall.hangup();
	    }
      }
      call.removeObserver(this);
      bridge.removeObserver(this);
      conferenceCenter.removeConference(room);
      final DateTime finish = DateTime.now();
      if(Call.Status.IN_PROGRESS == call.getStatus() && action != null) {
	    final List<NameValuePair> parameters = context.getRcmlRequestParameters();
	    parameters.add(new BasicNameValuePair("DialCallStatus", outboundCall.getStatus().toString()));
	    parameters.add(new BasicNameValuePair("DialCallSid", outboundCall.getSid().toString()));
	    parameters.add(new BasicNameValuePair("DialCallDuration",
	        Long.toString(finish.minus(start.getMillis()).getMillis() / TimeUtils.SECOND_IN_MILLIS)));
	    if(record) {
	      parameters.add(new BasicNameValuePair("RecordingUrl", toRecordingPath(recordingSid).toString()));
	    }
	    interpreter.load(action, method, parameters);
        interpreter.redirect();
	  }
	} catch(final Exception exception) {
	  interpreter.failed();
	  interpreter.notify(context, Notification.ERROR, 12400);
      logger.error(exception);
      throw new TagStrategyException(exception);
	}
  }
  
  private void fork(final List<Call> calls) throws CallException {
	forking = true;
    for(final Call call : calls) {
      if(Call.Status.QUEUED == call.getStatus()) {
    	call.addObserver(this);
        call.dial();
      }
    }
  }
  
  private void cleanup(final List<Call> calls) throws CallException {
    for(final Call call : calls) {
      if(call != outboundCall) {
        call.removeObserver(this);
        if(Call.Status.QUEUED == call.getStatus()) {
          call.cancel();
        } else if(Call.Status.IN_PROGRESS == call.getStatus()) {
          call.hangup();
        }
      }
    }
    forking = false;
  }
  
  private List<Call> getCalls(final List<Tag> tags) throws CallManagerException {
    final String caller = phoneNumberUtil.format(callerId, PhoneNumberFormat.E164);
    final List<Call> calls = new ArrayList<Call>();
    for(final Tag tag : tags) {
      if(Client.NAME.equals(tag.getName())) {
        calls.addAll(getClients(caller, tag));
      } else if(Uri.NAME.equals(tag.getName())) {
        final String uri = tag.getText();
        if(uri != null) {
          final Call call = callManager.createCall(caller, uri);
          calls.add(call);
          saveAttributes(call.getSid(), tag);
        }
      } else if(Number.NAME.equals(tag.getName())) {
        final String number = tag.getText();
        if(number != null) {
          try { 
			final PhoneNumber callee = phoneNumberUtil.parse(number, "US");
			final Call call = callManager.createExternalCall(caller,
		        phoneNumberUtil.format(callee, PhoneNumberFormat.E164));
			calls.add(call);
			saveAttributes(call.getSid(), tag);
		  } catch (NumberParseException ignored) { }
        }
      }
    }
    return calls;
  }
  
  private void saveAttributes(final Sid callSid, Tag tag) {
    Map<String, String> attributes = this.attributes.get(callSid.toString());
    if(attributes == null) {
      attributes = new HashMap<String, String>();
      this.attributes.put(callSid.toString(), attributes);
    }
    Attribute attribute = tag.getAttribute(SendDigits.NAME);
    if(attribute != null) { attributes.put("sendDigits", attribute.getValue()); }
    attribute = tag.getAttribute(Url.NAME);
    if(attribute != null) { attributes.put("url", attribute.getValue()); }
    attribute = tag.getAttribute(Method.NAME);
    if(attribute != null) { attributes.put("method", attribute.getValue()); }
    else { attributes.put("method", "POST"); }
  }
  
  private List<Call> getClients(final String caller, final Tag client) throws CallManagerException {
    final List<Call> calls = new ArrayList<Call>();
    final String user = client.getText();
    if(user != null) {
      final RegistrationsDao dao = daos.getRegistrationsDao();
      final List<Registration> registrations = dao.getRegistrationsByUser(user);
      for(final Registration registration : registrations) {
    	final Call call = callManager.createUserAgentCall(caller, registration.getLocation()); 
        calls.add(call);
        saveAttributes(call.getSid(), client);
      }
    }
    return calls;
  }
  
  @Override public synchronized void onStatusChanged(final Call call) {
    final Call.Status status = call.getStatus();
    if(forking) {
      if(Call.Status.IN_PROGRESS == call.getStatus() &&
          Call.Direction.OUTBOUND_DIAL == call.getDirection()) {
    	if(outboundCall == null) {
          outboundCall = call;
          notify();
    	}
      } else {
        for(final Call outboundCall : outboundCalls) {
          final Call.Status outboundCallStatus = outboundCall.getStatus();
          if(Call.Status.QUEUED == outboundCallStatus ||
              Call.Status.RINGING == outboundCallStatus ||
              Call.Status.IN_PROGRESS == outboundCallStatus) { return; }
        }
        notify();
      }
    } else {
      if(Call.Status.COMPLETED == status || Call.Status.FAILED == status) {
        notify();
      }
    }
  }

  @Override public synchronized void onStatusChanged(final Conference conference) {
    final Conference.Status status = conference.getStatus();
    if(Conference.Status.FAILED == status) {
      notify();
    }
  }
  
  private void sendDigits(final Call call, final String digits) throws CallException {
    final char[] characters = digits.toCharArray();
    for(final char character : characters) {
      if('*' == character || '#' == character ||
          Character.isDigit(character)) {
    	URI tone = null;
        switch(character) {
          case '0': {
        	tone = URI.create("0.tone");
            break;
          } case '1': {
        	tone = URI.create("1.tone");
            break;
          } case '2': {
        	tone = URI.create("2.tone");
            break;
          } case '3': {
        	tone = URI.create("3.tone");
            break;
          } case '4': {
        	tone = URI.create("4.tone");
            break;
          } case '5': {
        	tone = URI.create("5.tone");
            break;
          } case '6': {
        	tone = URI.create("6.tone");
            break;
          } case '7': {
        	tone = URI.create("7.tone");
            break;
          } case '8': {
        	tone = URI.create("8.tone");
            break;
          } case '9': {
        	tone = URI.create("9.tone");
            break;
          } case '*': {
        	tone = URI.create("*.tone");
            break;
          } case '#': {
        	tone = URI.create("#.tone");
            break;
          }
        }
        final List<URI> tones = new ArrayList<URI>();
        tones.add(tone);
        play(call, tones, 1);
      } else if('w' == character) {
    	try { wait(500); }
    	catch(final InterruptedException exception) {
    	  return;
    	}
      }
    }
  }
}

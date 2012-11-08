package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

public final class BridgeSubStrategy extends RcmlTagStrategy implements CallObserver, ConferenceObserver {
  private static final Logger logger = Logger.getLogger(BridgeSubStrategy.class);
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  private final PhoneNumberUtil phoneNumberUtil;
  
  private final URI action;
  private final String method;
  private final int timeout;
  private final int timeLimit;
  private final PhoneNumber callerId;
  private final URI ringbackTone;
  private final boolean record;
  private Sid recordingSid;
  private Call outboundCall;
  
  public BridgeSubStrategy(final URI action, final String method, final int timeout, final int timeLimit, final PhoneNumber callerId,
      final URI ringbackTone, final boolean record) {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    this.callManager = services.get(CallManager.class);
    this.conferenceCenter = services.get(ConferenceCenter.class);
    this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    this.action = action;
    this.method = method;
    this.timeout = timeout;
    this.timeLimit = timeLimit;
    this.callerId = callerId;
    this.ringbackTone = ringbackTone;
    this.record = record;
    if(record) { recordingSid = Sid.generate(Sid.Type.RECORDING); }
  }

  @Override public synchronized void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
	final PhoneNumber to = getTo(interpreter, context, tag);
	final String caller = phoneNumberUtil.format(callerId, PhoneNumberFormat.E164);
	final String callee = phoneNumberUtil.format(to, PhoneNumberFormat.E164);
	final StringBuilder buffer = new StringBuilder();
	buffer.append(context.getAccountSid().toString()).append(":").append(call.getSid().toString());
	final String room = buffer.toString();
	final Conference bridge = conferenceCenter.getConference(room);
	bridge.addObserver(this);
	final List<URI> ringbackAudioFiles = new ArrayList<URI>();
	ringbackAudioFiles.add(ringbackTone);
	bridge.setBackgroundMusic(ringbackAudioFiles);
	bridge.playBackgroundMusic();
	call.addObserver(this);
	bridge.addParticipant(call);
	try {
	  final DateTime start = DateTime.now();
	  outboundCall = callManager.createExternalCall(caller, callee);
	  outboundCall.addObserver(this);
	  outboundCall.dial();
      try { 
        wait();
        if(Call.Status.RINGING == outboundCall.getStatus()) {
          wait(TimeUtils.SECOND_IN_MILLIS * timeout);
        }
      } catch(final InterruptedException ignored) { }
      if(Call.Status.IN_PROGRESS == call.getStatus() && Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
        bridge.stopBackgroundMusic();
        bridge.addParticipant(outboundCall);
        if(record) {
          recordingSid = Sid.generate(Sid.Type.RECORDING);
          final URI destination = toRecordingPath(recordingSid);
          bridge.recordAudio(destination, TimeUtils.SECOND_IN_MILLIS * timeLimit);
        }
        try { wait(TimeUtils.SECOND_IN_MILLIS * timeLimit); }
        catch(final InterruptedException ignored) { }
        if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
          outboundCall.removeObserver(this);
          outboundCall.hangup();
        }
      } else if(Call.Status.QUEUED == outboundCall.getStatus()) {
        outboundCall.removeObserver(this);
        outboundCall.cancel();
      } else if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
        outboundCall.removeObserver(this);
        outboundCall.hangup();
      }
      call.removeObserver(this);
      bridge.removeObserver(this);
      conferenceCenter.removeConference(room);
      final DateTime finish = DateTime.now();
      if(Call.Status.IN_PROGRESS == call.getStatus() && action != null) {
  	    final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
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
      interpreter.notify(context, Notification.ERROR, 12400);
      logger.error(exception);
      throw new TagStrategyException(exception);
    }
  }
  
  private PhoneNumber getTo(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
	 return phoneNumberUtil.parse(tag.getText(), "US");
	} catch(final NumberParseException exception) {
	  interpreter.notify(context, Notification.WARNING, 13223);
	  logger.error(exception);
	  throw new TagStrategyException(exception);
	}
  }
  
  @Override public synchronized void onStatusChanged(final Call call) {
    final Call.Status status = call.getStatus();
    if((Call.Status.IN_PROGRESS == call.getStatus() && Call.Direction.OUTBOUND_DIAL == call.getDirection()) ||
        Call.Status.CANCELLED == status || Call.Status.COMPLETED == status || Call.Status.FAILED == status) {
	  notify();
	}
  }

  @Override public synchronized void onStatusChanged(final Conference conference) {
    final Conference.Status status = conference.getStatus();
    if(Conference.Status.FAILED == status) {
      notify();
    }
  }
}

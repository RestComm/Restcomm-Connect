package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.dao.PresenceRecordsDao;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.entities.PresenceRecord;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallManagerException;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Client;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Number;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Uri;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class ForkSubStrategy extends RcmlTagStrategy implements CallObserver, ConferenceObserver {
  private static final Logger logger = Logger.getLogger(ForkSubStrategy.class);
  
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
  
  private volatile boolean forking;
  private Call outboundCall;

  public ForkSubStrategy(final URI action, final String method, final int timeout, final int timeLimit, final PhoneNumber callerId,
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
    this.forking = false;
  }

  @Override public synchronized void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
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
	  final List<Call> calls = getCalls(tag.getChildren());
	  logger.info("************** 1 **************");
	  fork(calls);
	  logger.info("************** 2 **************");
	  try { wait(TimeUtils.SECOND_IN_MILLIS * timeout); }
      catch(final InterruptedException ignored) { }
	  logger.info("************** 3 **************");
	  select(calls);
	  logger.info("************** 4 **************");
	  if(Call.Status.IN_PROGRESS == call.getStatus() && (outboundCall != null &&
	      Call.Status.IN_PROGRESS == outboundCall.getStatus())) {
		logger.info("************** 5 **************");
        bridge.addParticipant(outboundCall);
        bridge.stopBackgroundMusic();
        logger.info("************** 6 **************");
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
      } else if(outboundCall != null) {
        outboundCall.removeObserver(this);
	    if(Call.Status.QUEUED == outboundCall.getStatus()) {
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
  
  private void fork(final List<Call> calls) throws CallException {
	forking = true;
    for(final Call call : calls) {
      if(Call.Status.QUEUED == call.getStatus()) {
    	call.addObserver(this);
        call.dial();
      }
    }
  }
  
  private void select(final List<Call> calls) throws CallException {
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
          calls.add(callManager.createCall(caller, uri));
        }
      } else if(Number.NAME.equals(tag.getName())) {
        final String number = tag.getText();
        if(number != null) {
          try { 
			final PhoneNumber callee = phoneNumberUtil.parse(number, "US");
			calls.add(callManager.createExternalCall(caller,
			    phoneNumberUtil.format(callee, PhoneNumberFormat.E164)));
		  } catch (NumberParseException ignored) { }
        }
      }
    }
    return calls;
  }
  
  private List<Call> getClients(final String caller, final Tag client) throws CallManagerException {
    final List<Call> calls = new ArrayList<Call>();
    final String user = client.getText();
    if(user != null) {
      final PresenceRecordsDao dao = daos.getPresenceRecordsDao();
      final List<PresenceRecord> records = dao.getPresenceRecordsByUser(user);
      for(final PresenceRecord record : records) {
        calls.add(callManager.createUserAgentCall(caller, record.getUri()));
      }
    }
    return calls;
  }
  
  @Override public synchronized void onStatusChanged(final Call call) {
    final Call.Status status = call.getStatus();
    if(forking) {
      if(Call.Status.IN_PROGRESS == call.getStatus()) {
        outboundCall = call;
        notify();
      }
    } else {
      if((Call.Status.IN_PROGRESS == call.getStatus() && Call.Direction.OUTBOUND_DIAL == call.getDirection()) ||
          Call.Status.CANCELLED == status || Call.Status.COMPLETED == status || Call.Status.FAILED == status) {
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
}

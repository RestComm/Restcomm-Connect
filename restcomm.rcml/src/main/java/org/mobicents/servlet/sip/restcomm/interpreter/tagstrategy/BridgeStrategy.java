package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
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
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class BridgeStrategy extends RcmlTagStrategy implements CallObserver, ConferenceObserver {
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  private final PhoneNumberUtil phoneNumberUtil;
  
  private int timeout;
  private int timeLimit;
  private PhoneNumber callerId;
  private URI ringbackTone;
  private boolean record;
  private Sid recordingSid;
  
  private Call outboundCall;

  public BridgeStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    callManager = services.get(CallManager.class);
    conferenceCenter = services.get(ConferenceCenter.class);
    phoneNumberUtil = PhoneNumberUtil.getInstance();
    ringbackTone = URI.create("file://" + configuration.getString("ringback-audio-file"));
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    
  }
  
  private synchronized void bridge(final Call call, final PhoneNumber to) throws CallManagerException, CallException {
    final String caller = phoneNumberUtil.format(callerId, PhoneNumberFormat.E164);
	final String callee = phoneNumberUtil.format(to, PhoneNumberFormat.E164);
	final Conference bridge = conferenceCenter.getConference(call.getSid().toString());
	final List<URI> ringbackAudioFiles = new ArrayList<URI>();
	ringbackAudioFiles.add(ringbackTone);
	bridge.setBackgroundMusic(ringbackAudioFiles);
	bridge.playBackgroundMusic();
	call.addObserver(this);
	bridge.addParticipant(call);
	outboundCall = callManager.createExternalCall(caller, callee);
    outboundCall.addObserver(this);
    outboundCall.dial();
    try { wait(TimeUtils.SECOND_IN_MILLIS * timeout); }
    catch(final InterruptedException ignored) { }
    if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
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
    }
    call.removeObserver(this);
    conferenceCenter.removeConference(call.getSid().toString());
  }
  
  @Override public void onStatusChanged(final Call call) {
    
  }

  @Override public void onStatusChanged(final Conference conference) {
    
  }
}

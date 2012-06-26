package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
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

public final class ConferenceStrategy extends RcmlTagStrategy implements CallObserver, ConferenceObserver {
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  private final PhoneNumberUtil phoneNumberUtil;
  
  private int timeLimit;
  private boolean record;
  private Sid recordingSid;

  public ConferenceStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    conferenceCenter = services.get(ConferenceCenter.class);
    phoneNumberUtil = PhoneNumberUtil.getInstance();
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    
  }
  
  private synchronized void join(final String name, final boolean muted, final boolean beep,
	  final boolean startConferenceOnEnter, final boolean endConferenceOnExit, final URI waitUrl,
	  final String waitMethod, final int maxParticipant, final Call call) {
    final Conference conference = conferenceCenter.getConference(name);
    if(!startConferenceOnEnter) {
      if(!call.isMuted()) {
        call.mute();
      }
      if(conference.getNumberOfParticipants() == 0) {
        if(waitUrl != null) {
    	  final List<URI> music = new ArrayList<URI>();
    	  music.add(waitUrl);
    	  conference.setBackgroundMusic(music);
          conference.playBackgroundMusic();
        }
      }
    } else {
      conference.stopBackgroundMusic();
      if(beep) { conference.alert(); }
      if(muted) { call.mute(); }
    }
    call.addObserver(this);
    conference.addObserver(this);
    conference.addParticipant(call);
    try { wait(TimeUtils.SECOND_IN_MILLIS * timeLimit); }
    catch(final InterruptedException ignored) { }
    conference.removeObserver(this);
    call.removeObserver(this);
    if(endConferenceOnExit || conference.getNumberOfParticipants() == 0) {
      conferenceCenter.removeConference(name);
    } else {
      if(Call.Status.IN_PROGRESS == call.getStatus()) {
        conference.removeParticipant(call);
      }
    }
  }
  
  @Override public void onStatusChanged(final Call call) {
    
  }

  @Override public void onStatusChanged(final Conference conference) {
    
  }
}

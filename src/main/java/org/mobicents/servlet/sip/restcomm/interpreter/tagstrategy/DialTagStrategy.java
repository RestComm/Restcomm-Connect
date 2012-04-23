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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallObserver;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Beep;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.CallerId;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.EndConferenceOnExit;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.HangupOnStar;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.MaxParticipants;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Muted;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Record;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.RingbackTone;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.StartConferenceOnEnter;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.TimeLimit;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.WaitMethod;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.WaitUrl;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DialTagStrategy extends RcmlTagStrategy implements CallObserver, ConferenceObserver {
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  private final PhoneNumberUtil phoneNumberUtil;
  
  private Call outboundCall;
  
  private URI action;
  private String method;
  private int timeout;
  private boolean hangupOnStar;
  private int timeLimit;
  private PhoneNumber callerId;
  private URI ringbackTone;
  private boolean record;
  
  public DialTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    callManager = services.get(CallManager.class);
    conferenceCenter = services.get(ConferenceCenter.class);
    phoneNumberUtil = PhoneNumberUtil.getInstance();
    ringbackTone = URI.create("file://" + configuration.getString("ringback-audio-file"));
  }
  
  private void bridge(final Call call, final PhoneNumber to) throws CallManagerException, CallException {
    final String sender = phoneNumberUtil.format(callerId, PhoneNumberFormat.E164);
	final String recipient = phoneNumberUtil.format(to, PhoneNumberFormat.E164);
	final String name = new StringBuilder().append(sender).append(":").append(recipient).toString();
	final Conference bridge = conferenceCenter.getConference(name);
	final List<URI> ringbackAudioFiles = new ArrayList<URI>();
	ringbackAudioFiles.add(ringbackTone);
	bridge.setBackgroundMusic(ringbackAudioFiles);
	bridge.playBackgroundMusic();
	call.addObserver(this);
	bridge.addParticipant(call);
	outboundCall = callManager.createCall(sender, recipient);
    outboundCall.addObserver(this);
    outboundCall.dial(TimeUtils.SECOND_IN_MILLIS * timeout);
    if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
      bridge.stopBackgroundMusic();
      bridge.addParticipant(outboundCall);
      if(record) {
        final Sid sid = Sid.generate(Sid.Type.RECORDING);
        final URI destination = toRecordingPath(sid);
        bridge.recordAudio(destination, TimeUtils.SECOND_IN_MILLIS * timeLimit);
      }
      try { synchronized(this) { wait(TimeUtils.SECOND_IN_MILLIS * timeLimit); } }
      catch(final InterruptedException ignored) { }
      if(Call.Status.IN_PROGRESS == outboundCall.getStatus()) {
        outboundCall.removeObserver(this);
        outboundCall.hangup();
      }
    }
    call.removeObserver(this);
    conferenceCenter.removeConference(name);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	try {
	  final Call call = context.getCall();
	  final String text = tag.getText();
	  if(text != null && !text.isEmpty()) {
	    try {
		  final PhoneNumber to = phoneNumberUtil.parse(text, "US");
		  bridge(call, to);
		} catch(final NumberParseException exception) {
		  interpreter.notify(context, Notification.WARNING, 13223);
		}
	  } else {
	    final List<Tag> children = tag.getChildren();
	    if(hasConferenceTag(children)) {
	      final RcmlTag conference = (RcmlTag)getConferenceTag(children);
	      final String name = conference.getText();
	      final boolean muted = getMuted(interpreter, context, conference);
	      final boolean beep = getBeep(interpreter, context, conference);
	      final boolean startConferenceOnEnter = getStartConferenceOnEnter(interpreter, context, conference);
	      final boolean endConferenceOnExit = getEndConferenceOnExit(interpreter, context, conference);
	      final URI waitUrl = getWaitUrl(interpreter, context, conference);
	      final String waitMethod = getWaitMethod(interpreter, context, conference);
	      final int maxParticipants = getMaxParticipants(interpreter, context, conference);
	      join(name, muted, beep, startConferenceOnEnter, endConferenceOnExit, waitUrl,
	          waitMethod, maxParticipants, call);
	    } else {
	      
	    }
	  }
    } catch(final Exception exception) {
      interpreter.failed();
  	  interpreter.notify(context, Notification.ERROR, 12400);
      throw new TagStrategyException(exception);
    }
  }
  
  private boolean getBeep(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(Beep.NAME);
    if(attribute == null) {
      return true;
    }
    final String value = attribute.getValue();
    if("true".equalsIgnoreCase(value)) {
      return true;
    } else if("false".equalsIgnoreCase(value)) {
      return false;
    } else {
      return true;
    }
  }
  
  private Tag getConferenceTag(final List<Tag> tags) {
    final String name = org.mobicents.servlet.sip.restcomm.xml.rcml.Conference.NAME;
    for(final Tag tag : tags) {
      if(name.equals(tag.getName())) {
        return tag;
      }
    }
    return null;
  }
  
  private boolean getEndConferenceOnExit(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(EndConferenceOnExit.NAME);
    if(attribute == null) {
      return false;
    }
    final String value = attribute.getValue();
    if("true".equalsIgnoreCase(value)) {
      return true;
    } else if("false".equalsIgnoreCase(value)) {
      return false;
    } else {
      interpreter.notify(context, Notification.WARNING, 13231);
      return false;
    }
  }
  
  private int getMaxParticipants(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(MaxParticipants.NAME);
    if(attribute == null) {
      return 40;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result > 0) {
        return result;
      }
    }
    return 40;
  }
  
  private boolean getMuted(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(Muted.NAME);
    if(attribute == null) {
      return false;
    }
    final String value = attribute.getValue();
    if("true".equalsIgnoreCase(value)) {
      return true;
    } else if("false".equalsIgnoreCase(value)) {
      return false;
    } else {
      interpreter.notify(context, Notification.WARNING, 13230);
      return false;
    }
  }
  
  private boolean getStartConferenceOnEnter(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(StartConferenceOnEnter.NAME);
    if(attribute == null) {
      return true;
    }
    final String value = attribute.getValue();
    if("true".equalsIgnoreCase(value)) {
      return true;
    } else if("false".equalsIgnoreCase(value)) {
      return false;
    } else {
      interpreter.notify(context, Notification.WARNING, 13232);
      return true;
    }
  }
  
  private URI getWaitUrl(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(WaitUrl.NAME);
    if(attribute != null) {
      try {
        final URI base = interpreter.getCurrentResourceUri();
	    return resolveIfNotAbsolute(base, attribute.getValue());
      } catch(final IllegalArgumentException exception) {
        interpreter.notify(context, Notification.ERROR, 13233);
        throw new TagStrategyException(exception);
      }
    }
    return null;
  }
  
  private String getWaitMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(WaitMethod.NAME);
    if(attribute == null) {
      return "POST";
    }
    final String value = attribute.getValue();
    if("GET".equalsIgnoreCase(value)) {
      return "GET";
    } else if("POST".equalsIgnoreCase(value)) {
      return "POST";
    } else {
    	interpreter.notify(context, Notification.WARNING, 13234);
      return "POST";
    }
  }
  
  private boolean hasConferenceTag(final List<Tag> tags) {
    return getConferenceTag(tags) != null;
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    action = getAction(interpreter, context, tag);
    initCallerId(interpreter, context, tag);
    initHangupOnStar(interpreter, context, tag);
    initMethod(interpreter, context, tag);
    initRecord(interpreter, context, tag);
    initRingbackTone(interpreter, context, tag);
    initTimeout(interpreter, context, tag);
    initTimeLimit(interpreter, context, tag);
  }
  
  private void initCallerId(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(CallerId.NAME);
    String value = null;
    if(attribute != null) {
      value = attribute.getValue();
    } else {
      value = context.getCall().getOriginator();
    }
    try { 
      callerId = phoneNumberUtil.parse(value, "US");
    } catch(final NumberParseException ignored) {
      interpreter.notify(context, Notification.WARNING, 13214);
    }
  }
  
  private void initHangupOnStar(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
    final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(HangupOnStar.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if("true".equalsIgnoreCase(value)) {
        hangupOnStar = true;
      } else if("false".equalsIgnoreCase(value)) {
        hangupOnStar = false;
      } else {
        interpreter.notify(context, Notification.WARNING, 13213);
        hangupOnStar = false;
      }
    } else {
      hangupOnStar = false;
    }
  }
  
  private void initMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    method = getMethod(interpreter, context, tag);
    if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      interpreter.notify(context, Notification.WARNING, 13210);
      method = "POST";
    }
  }
  
  private void initRingbackTone(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(RingbackTone.NAME);
    if(attribute != null) {
      final URI base = interpreter.getCurrentResourceUri();
      ringbackTone = resolveIfNotAbsolute(base, attribute.getValue());
    }
  }
  
  private void initTimeout(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	final Object object = getTimeout(interpreter, context, tag);
	if(object == null) {
	  timeout = 30;
	} else {
	  timeout = (Integer)object;
	  if(timeout == -1) {
	    interpreter.notify(context, Notification.WARNING, 13212);
	    timeout = 30;
	  }
	}
  }
  
  private void initTimeLimit(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(TimeLimit.NAME);
    if(attribute == null) { 
      timeLimit = 14400;
      return;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result > 0) {
        timeLimit = result;
      }
    }
    interpreter.notify(context, Notification.WARNING, 13216);
    timeLimit = 14400;
  }
  
  private void initRecord(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(Record.NAME);
    if(attribute == null) {
      record = false;
      return;
    }
    final String value = attribute.getValue();
    if("true".equalsIgnoreCase(value)) {
      record = true;
    } else {
      record = false;
    }
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
    conference.addParticipant(call);
    try { wait(TimeUtils.SECOND_IN_MILLIS * timeLimit); }
    catch(final InterruptedException ignored) { }
    call.removeObserver(this);
    if(endConferenceOnExit) {
      conferenceCenter.removeConference(name);
    } else {
      if(Call.Status.IN_PROGRESS == call.getStatus()) {
        conference.removeParticipant(call);
      }
    }
  }
  
  @Override synchronized public void onStatusChanged(final Call call) {
    final Call.Status status = call.getStatus();
    if(Call.Status.COMPLETED == status || Call.Status.FAILED == status) {
      notify();
    }
  }
  
  @Override synchronized public void onStatusChanged(final Conference conference) {
    final Conference.Status status = conference.getStatus();
    if(Conference.Status.COMPLETED == status || Conference.Status.FAILED == status) {
      notify();
    }
  }
}

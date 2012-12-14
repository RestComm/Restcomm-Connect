package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice;

import java.net.URI;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterFactory;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.VoiceRcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceException;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceObserver;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Beep;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.EndConferenceOnExit;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.MaxParticipants;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Muted;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.StartConferenceOnEnter;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.WaitMethod;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.WaitUrl;

public final class ConferenceSubStrategy extends VoiceRcmlTagStrategy implements CallObserver, ConferenceObserver {
  private static final Logger logger = Logger.getLogger(ConferenceSubStrategy.class);

  private final ConferenceCenter conferenceCenter;
  private final InterpreterFactory interpreterFactory;
  
  private final URI action;
  private final String method;
  private final int timeLimit;
  private String name;
  private boolean muted;
  private boolean beep;
  private boolean startConferenceOnEnter;
  private boolean endConferenceOnExit;
  private URI waitUrl;
  private String waitMethod;
  private int maxParticipants;
  
  private URI alertOnEnterAudioFile;
  private URI alertOnExitAudioFile;

  public ConferenceSubStrategy(final URI action, final String method, final int timeLimit, final boolean record) {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    this.conferenceCenter = services.get(ConferenceCenter.class);
    this.interpreterFactory = services.get(InterpreterFactory.class);
    this.action = action;
    this.method = method;
    this.timeLimit = timeLimit;
    waitUrl = URI.create("file://" + configuration.getString("conference-music-file"));
    alertOnEnterAudioFile = URI.create("file://" + configuration.getString("alert-on-enter-file"));
    alertOnExitAudioFile = URI.create("file://" + configuration.getString("alert-on-exit-file"));
  }

  @Override public synchronized void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	final VoiceRcmlInterpreterContext voiceContext = (VoiceRcmlInterpreterContext)context;
    final Call call = voiceContext.getCall();
    final StringBuilder buffer = new StringBuilder();
    buffer.append(context.getAccountSid().toString()).append(":").append(name);
    final String room = buffer.toString();
    final Conference conference = conferenceCenter.getConference(room);
    if(conference.getNumberOfParticipants() <= maxParticipants) {
      if(!startConferenceOnEnter) {
        if(Call.Status.IN_PROGRESS == call.getStatus() && !call.isMuted()) {
          call.mute();
        }
        if(conference.getNumberOfParticipants() == 0) {
          if(waitUrl != null) {
            interpreterFactory.create(context.getAccountSid(), context.getApiVersion(), waitUrl,
                waitMethod, conference);
          }
        }
      } else {
        // Stop the interpreter
        final RcmlInterpreter conferenceInterpreter = interpreterFactory.remove(conference.getSid());
        // Wait for the interpreter to finish before continuing.
        try { conferenceInterpreter.join(); }
        catch(final InterruptedException ignored) { }
        if(beep) { conference.play(alertOnEnterAudioFile); }
        if(Call.Status.IN_PROGRESS == call.getStatus() && muted) { call.mute(); }
      }
      if(Call.Status.IN_PROGRESS == call.getStatus()) {
        call.addObserver(this);
        conference.addObserver(this);
        try {
          conference.addParticipant(call);
          wait(TimeUtils.SECOND_IN_MILLIS * timeLimit);
        } catch(final ConferenceException exception) {
    	  interpreter.failed();
          interpreter.notify(context, Notification.ERROR, 12400);
          logger.error(exception);
          throw new TagStrategyException(exception);
        } catch(final InterruptedException ignored) { }
        conference.removeObserver(this);
        call.removeObserver(this);
      }
      if(endConferenceOnExit || (conference.getNumberOfParticipants() == 0)) {
        conferenceCenter.removeConference(room);
      } else {
        if(Call.Status.IN_PROGRESS == call.getStatus() && Conference.Status.IN_PROGRESS == conference.getStatus()) {
          conference.removeParticipant(call);
        }
        if(beep) { conference.play(alertOnExitAudioFile); }
      }
    }
    if(Call.Status.IN_PROGRESS == call.getStatus() && action != null) {
      final List<NameValuePair> parameters = context.getRcmlRequestParameters();
      parameters.add(new BasicNameValuePair("DialCallStatus", "completed"));
      interpreter.load(action, method, parameters);
      interpreter.redirect();
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
  
  @Override public synchronized void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	final RcmlTag conference = (RcmlTag)getConferenceTag(tag.getChildren());
    name = conference.getText();
    muted = getMuted(interpreter, context, conference);
    beep = getBeep(interpreter, context, conference);
    startConferenceOnEnter = getStartConferenceOnEnter(interpreter, context, conference);
    endConferenceOnExit = getEndConferenceOnExit(interpreter, context, conference);
    waitUrl = getWaitUrl(interpreter, context, conference);
    waitMethod = getWaitMethod(interpreter, context, conference);
    maxParticipants = getMaxParticipants(interpreter, context, conference);
  }

  @Override public synchronized void onStatusChanged(final Call call) {
    final Call.Status status = call.getStatus();
    if(Call.Status.COMPLETED == status || Call.Status.FAILED == status) {
      notify();
    }
  }

  @Override public synchronized void onStatusChanged(final Conference conference) {
    final Conference.Status status = conference.getStatus();
    if(Conference.Status.COMPLETED == status || Conference.Status.FAILED == status) {
      notify();
    }
  }
}

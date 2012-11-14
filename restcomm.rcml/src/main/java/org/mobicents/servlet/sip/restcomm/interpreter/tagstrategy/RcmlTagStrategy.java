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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.CallObserver;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Action;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.FinishOnKey;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Length;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Loop;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.StatusCallback;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Timeout;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Voice;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class RcmlTagStrategy implements CallObserver, TagStrategy {
  protected final Configuration configuration;
  protected final DaoManager daos;
  protected final String homeDirectory;
  protected final String rootUri;
  private final URI silenceAudioFile;
  private final String baseRecordingsPath;

  public RcmlTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    daos = services.get(DaoManager.class);
    configuration = services.get(Configuration.class);
    homeDirectory = StringUtils.addSuffixIfNotPresent(configuration.getString("home-directory"), "/");
    rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    silenceAudioFile = URI.create("file://" + configuration.getString("silence-audio-file"));
    baseRecordingsPath = StringUtils.addSuffixIfNotPresent(configuration.getString("recordings-path"), "/");
  }
  
  protected URI getAction(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Action.NAME);
    if(attribute != null) {
      final URI base = interpreter.getCurrentResourceUri();
  	  return resolveIfNotAbsolute(base, attribute.getValue());
    } 
    return null;
  }
  
  protected String getFinishOnKey(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(FinishOnKey.NAME);
    if(attribute != null) {
      return attribute.getValue();
    }
    return null;
  }
  
  protected String getGender(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Voice.NAME);
    if(attribute == null) {
      return "man";
    }
    final String gender = attribute.getValue();
    if("man".equals(gender) || "woman".equals(gender)) {
      return gender;
    }
    return null;
  }
  
  protected String getLanguage(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Language.NAME);
    if(attribute == null) {
      return "en";
    }
    final ServiceLocator services = ServiceLocator.getInstance();
    final SpeechSynthesizer synthesizer = services.get(SpeechSynthesizer.class);
    final String language = attribute.getValue();
    if(synthesizer.isSupported(language)) {
      return language;
    }
    return null;
  }
  
  protected Integer getLength(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Length.NAME);
    if(attribute == null) {
      return 1;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result > 0) {
        return result;
      }
    }
    return -1;
  }
  
  protected Integer getLoop(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Loop.NAME);
    if(attribute == null) {
      return 1;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result >= 0) {
        return result;
      }
    }
    return -1;
  }
  
  protected String getMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Method.NAME);
    if(attribute == null) {
      return "POST";
    }
    return attribute.getValue();
  }
  
  protected PhoneNumber getPhoneNumber(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag, final String attributeName) {
    final Attribute attribute = tag.getAttribute(attributeName);
    String value = null;
    if(attribute != null) {
      value = attribute.getValue();
    } else {
      value = context.getCall().getOriginator();
    }
    try { return PhoneNumberUtil.getInstance().parse(value, "US"); }
    catch(final NumberParseException ignored) { }
    return null;
  }
  
  protected URI getStatusCallback(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(StatusCallback.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      return URI.create(value);
    }
    return null;
  }
  
  protected Integer getTimeout(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Timeout.NAME);
    if(attribute == null) {
      return null;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result > 0) {
        return result;
      }
    }
    return -1;
  }
  
  protected URI getUri(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
	final String text = tag.getText();
	if(text != null && !text.isEmpty()) {
	  final URI base = interpreter.getCurrentResourceUri();
	  return resolveIfNotAbsolute(base, text);
	} else {
	  return null;
	}
  }
  
  @Override public synchronized void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
    Call.Status status = call.getStatus();
    try {
      if(Call.Status.RINGING == status && Call.Direction.INBOUND == call.getDirection()) {
        call.answer();
      } else if(Call.Status.QUEUED == status) {
        call.addObserver(this);
        call.dial();
        //Issue 95: http://code.google.com/p/restcomm/issues/detail?id=95
        try {
          // Wait for state change to ringing before starting the timeout period.
          wait();
          // If the call is ringing on the remote side then wait for timeout period.
          status = call.getStatus();
          if(Call.Status.RINGING.equals(status)) {
            interpreter.sendStatusCallback();
            wait(context.getTimeout() * 1000);
          }
        }
        catch(final InterruptedException ignored) { }
        call.removeObserver(this);
        status = call.getStatus();
        if(Call.Status.IN_PROGRESS != status && Call.Status.FAILED != status) {
          call.cancel();
        }
      }
    } catch(final CallException exception) {
	  interpreter.failed();
	  interpreter.notify(context, Notification.ERROR, 12400);
	  throw new TagStrategyException(exception);
	}
  }
  
  protected List<URI> pause(final int seconds) {
    final List<URI> silence = new ArrayList<URI>();
    for(int count = 1; count <= seconds; count++) {
      silence.add(silenceAudioFile);
    }
    return silence;
  }
  
  @Override public synchronized void onStatusChanged(final Call call) {
    notify();
  }
  
  protected void play(final Call call, final List<URI> announcements, final int iterations)
      throws CallException {
    if(Call.Status.IN_PROGRESS == call.getStatus()) {
      call.play(announcements, iterations);
    }
  }
  
  protected URI resolveIfNotAbsolute(final URI base, final String uri) {
    return resolveIfNotAbsolute(base, URI.create(uri));
  }
  
  protected URI resolveIfNotAbsolute(final URI base, final URI uri) {
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
  
  protected URI say(final String gender, final String language, final String text) {
    final ServiceLocator services = ServiceLocator.getInstance();
    final SpeechSynthesizer synthesizer = services.get(SpeechSynthesizer.class);
    return synthesizer.synthesize(text, gender, language);
  }
  
  public void precache(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
	      final RcmlTag tag){
	  String gender = this.getGender(interpreter, context, tag);
	  String language = this.getLanguage(interpreter, context, tag);
	  String text = tag.getText();
	  say(gender, language, text);
  }
  
  protected URI toRecordingPath(final Sid sid) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("file://").append(baseRecordingsPath).append(sid.toString()).append(".wav");
    return URI.create(buffer.toString());
  }
}

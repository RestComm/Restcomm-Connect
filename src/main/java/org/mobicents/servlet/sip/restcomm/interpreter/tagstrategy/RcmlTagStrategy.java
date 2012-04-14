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
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Action;
import org.mobicents.servlet.sip.restcomm.xml.rcml.FinishOnKey;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Length;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Loop;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Timeout;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Voice;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class RcmlTagStrategy implements TagStrategy {
  private static final Pattern finishOnKeyPattern = Pattern.compile("[\\*#0-9]{1,12}");
  
  protected final Configuration configuration;
  protected final DaoManager daos;
  protected final String homeDirectory;
  protected final String rootUri;
  private final String errorDictionary;
  private final URI silenceAudioFile;

  public RcmlTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    daos = services.get(DaoManager.class);
    configuration = services.get(Configuration.class);
    homeDirectory = StringUtils.addSuffixIfNotPresent(configuration.getString("home-directory"), "/");
    rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    errorDictionary = StringUtils.addSuffixIfNotPresent(configuration.getString("error-dictionary-uri"), "/");
    silenceAudioFile = URI.create("file://" + configuration.getString("silence-audio-file"));
  }
  
  protected URI getAction(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Action.NAME);
    if(attribute != null) {
      final URI base = interpreter.getCurrentResourceUri();
  	  return resolveIfNotAbsolute(base, attribute.getValue());
    } else {
      return interpreter.getCurrentResourceUri();
    }
  }
  
  protected String getFinishOnKey(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(FinishOnKey.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(finishOnKeyPattern.matcher(value).matches()) {
        return value;
      }
    }
    return null;
  }
  
  protected String getGender(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Voice.NAME);
    if(attribute != null) {
      final String gender = attribute.getValue();
      if("man".equals(gender) || "woman".equals(gender)) {
        return gender;
      }
    }
    return null;
  }
  
  protected String getLanguage(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Language.NAME);
    if(attribute != null) {
      final ServiceLocator services = ServiceLocator.getInstance();
      final SpeechSynthesizer synthesizer = services.get(SpeechSynthesizer.class);
      final String language = attribute.getValue();
      if(synthesizer.isSupported(language)) {
        return language;
      }
    }
    return null;
  }
  
  protected int getLength(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Length.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
        final int result = Integer.parseInt(value);
        if(result > 0) {
          return result;
        }
      }
    }
    return -1;
  }
  
  protected int getLoop(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Loop.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
        final int result = Integer.parseInt(value);
        if(result >= 0) {
          return result;
        }
      }
    }
    return -1;
  }
  
  protected String getMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Method.NAME);
    if(attribute != null) {
      return attribute.getValue();
    } else {
      return null;
    }
  }
  
  protected int getTimeout(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Timeout.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
        final int result = Integer.parseInt(value);
        if(result > 0) {
          return result;
        }
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
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
    if(Call.Status.RINGING == call.getStatus()) {
      try {
		call.answer();
	  } catch(final CallException exception) {
	    throw new TagStrategyException(exception);
	  } catch(final InterruptedException exception) {
	    interpreter.notify(context, Notification.ERROR, 21220);
	  }
    }
  }
  
  protected List<URI> pause(final int seconds) {
    final List<URI> silence = new ArrayList<URI>();
    for(int count = 1; count <= seconds; count++) {
      silence.add(silenceAudioFile);
    }
    return silence;
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
}

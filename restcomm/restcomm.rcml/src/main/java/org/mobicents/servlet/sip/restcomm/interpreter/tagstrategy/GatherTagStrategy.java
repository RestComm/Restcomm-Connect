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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Play;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Say;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.NumDigits;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class GatherTagStrategy extends RcmlTagStrategy {
  private static final Logger logger = Logger.getLogger(GatherTagStrategy.class);
  private static final Pattern finishOnKeyPattern = Pattern.compile("[\\*#0-9]{1}");
  
  private URI action;
  private String method;
  private int timeout;
  private String finishOnKey;
  private int numDigits;

  public GatherTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
	  // Collect some digits.
	  final List<URI> announcements = getAnnouncements(interpreter, context, tag);
	  final Call call = context.getCall();
	  try {
	    if(Call.Status.IN_PROGRESS == call.getStatus()) {
          call.playAndCollect(announcements, numDigits, 1,timeout, timeout, finishOnKey);
	    }
	  } catch(final CallException exception) {
	    exception.printStackTrace();
	  }
      // Redirect to action URI.;
      final String digits = call.getDigits();
      if(digits != null && digits.length() > 0) {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("Digits", digits));
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
  
  private List<URI> getAnnouncements(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	final List<Tag> children = tag.getChildren();
	final List<URI> announcements = new ArrayList<URI>();
    for(final Tag child : children) {
      final String name = child.getName();
      if(Say.NAME.equals(name)) {
        announcements.addAll(getSay(interpreter, context, (RcmlTag)child));
      } else if(Play.NAME.equals(name)) {
        announcements.addAll(getPlay(interpreter, context, (RcmlTag)child));
      } else if(Pause.NAME.equals(name)) {
        announcements.addAll(getPause(interpreter, context, (RcmlTag)child));
      }
 	  tag.setHasBeenVisited(true);
 	}
    return announcements;
  }
  
  private int getNumDigits(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(NumDigits.NAME);
    if(attribute == null) {
      return Short.MAX_VALUE;
    }
    final String value = attribute.getValue();
    if(StringUtils.isPositiveInteger(value)) {
      final int result = Integer.parseInt(value);
      if(result >= 1) {
        return result; 
      }
    }
    interpreter.notify(context, Notification.WARNING, 13314);
    return Short.MAX_VALUE;
  }
  
  private List<URI> getPause(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    int length = getLength(interpreter, context, tag);
    if(length == -1) {
      length = 1;
    }
    return pause(length);
  }
  
  private List<URI> getPlay(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    int loop = getLoop(interpreter, context, tag);
    if(loop == -1) {
      loop = 1;
    }
    final URI uri = getUri(interpreter, context, tag);
    if(uri == null) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 13325);
      throw new TagStrategyException("There is no resource to play.");
    }
    final List<URI> announcements = new ArrayList<URI>();
    if(uri != null) {
      for(int counter = 0; counter < loop; counter++) {
        announcements.add(uri);
      }
    }
    return announcements;
  }
  
  private List<URI> getSay(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    String gender = getGender(interpreter, context, tag);
    if(gender == null) {
      interpreter.notify(context, Notification.WARNING, 13321);
      gender = "man";
    }
    String language = getLanguage(interpreter, context, tag);
    if(language == null) {
      language = "en";
    }
    int loop = getLoop(interpreter, context, tag);
    if(loop == -1) {
      loop = 1;
    }
    final String text = tag.getText();
    if(text == null || text.isEmpty()) {
  	  interpreter.notify(context, Notification.WARNING, 13322);
  	}
    final List<URI> announcements = new ArrayList<URI>();
    if(text != null) {
      final URI uri = say(gender, language, text);
      for(int counter = 0; counter < loop; counter++) {
        announcements.add(uri);
      }
    }
    return announcements;
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    initAction(interpreter, context, tag);
    initMethod(interpreter, context, tag);
    initTimeout(interpreter, context, tag);
    initFinishOnKey(interpreter, context, tag);
    numDigits = getNumDigits(interpreter, context, tag);
  }
  
  private void initAction(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      action = getAction(interpreter, context, tag);
      if(action == null) {
        action = interpreter.getCurrentResourceUri();
      }
    } catch(final IllegalArgumentException exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 11100);
      throw new TagStrategyException(exception);
    }
  }
  
  private void initMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    method = getMethod(interpreter, context, tag);
    if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      interpreter.notify(context, Notification.WARNING, 13312);
      method = "POST";
    }
  }
  
  private void initTimeout(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Object object = getTimeout(interpreter, context, tag);
    if(object == null) {
      timeout = 5;
    } else {
      timeout = (Integer)object;
      if(timeout == -1) {
        interpreter.notify(context, Notification.WARNING, 13313);
        timeout = 5;
      }
    }
  }
  
  private void initFinishOnKey(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    finishOnKey = getFinishOnKey(interpreter, context, tag);
    if(finishOnKey == null) {
      finishOnKey = "#";
    } else {
      if(!finishOnKeyPattern.matcher(finishOnKey).matches()) {
    	interpreter.notify(context, Notification.WARNING, 13310);
    	finishOnKey = "#";
      }
    }
  }
}

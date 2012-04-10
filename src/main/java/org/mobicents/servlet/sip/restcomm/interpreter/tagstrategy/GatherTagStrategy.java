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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.IntegerAttribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Length;
import org.mobicents.servlet.sip.restcomm.xml.rcml.NumDigits;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Play;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Say;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Voice;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class GatherTagStrategy extends RcmlTagStrategy {
  private URI action;
  private String method;
  private int timeout;
  private String finishOnKey;
  private int numDigits;

  public GatherTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    try {
	  // Collect some digits.
      final Call call = context.getCall();
	  final List<URI> announcements = getAnnouncements(tag.getChildren());
      call.playAndCollect(announcements, numDigits, 1,timeout, timeout, finishOnKey);
      // Redirect to action URI.;
      final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
      parameters.add(new BasicNameValuePair("Digits", call.getDigits()));
      interpreter.loadResource(action, method, parameters);
      interpreter.redirect();
    } catch(final Exception exception) {
      interpreter.failed();
      notify(interpreter, context, tag, Notification.ERROR, 12400);
      throw new TagStrategyException(exception);
    }
  }
  
  private List<URI> getAnnouncements(final List<Tag> children) {
	final List<URI> announcements = new ArrayList<URI>();
    for(final Tag child : children) {
      final RcmlTag tag = (RcmlTag)child;
      final String name = tag.getName();
      if(Say.NAME.equals(name)) {
    	final String gender = tag.getAttribute(Voice.NAME).getValue();
        final String language = tag.getAttribute(Language.NAME).getValue();
        final String text = tag.getText();
        announcements.addAll(say(gender, language, text));
      } else if(Play.NAME.equals(name)) {
        final String text = tag.getText();
        if(text != null) {
          final URI uri = URI.create(text);
          announcements.add(uri);
        }
      } else if(Pause.NAME.equals(name)) {
        final int length = ((IntegerAttribute)tag
            .getAttribute(Length.NAME)).getIntegerValue();
        announcements.addAll(pause(length));
      }
 	  tag.setHasBeenVisited(true);
 	}
    return announcements;
  }
  
  private int getNumDigits(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) {
    final Attribute attribute = tag.getAttribute(NumDigits.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
    	final int result = Integer.parseInt(value);
    	if(result >= 1) {
          return result; 
    	}
      }
    }
    notify(interpreter, context, tag, Notification.WARNING, 13314);
    return Short.MAX_VALUE;
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    try {
      action = getAction(interpreter, context, tag);
      method = getMethod(interpreter, context, tag);
      if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
        notify(interpreter, context, tag, Notification.WARNING, 13312);
        method = "POST";
      }
      timeout = getTimeout(interpreter, context, tag);
      if(timeout == -1 || timeout == 0) {
        notify(interpreter, context, tag, Notification.WARNING, 13313);
        timeout = 5;
      }
      finishOnKey = getFinishOnKey(interpreter, context, tag);
      if(finishOnKey == null) {
        notify(interpreter, context, tag, Notification.WARNING, 13310);
        finishOnKey = "#";
      }
      numDigits = getNumDigits(interpreter, context, tag);
    } catch(final IllegalArgumentException exception) {
      notify(interpreter, context, tag, Notification.ERROR, 11100);
    }
  }
}

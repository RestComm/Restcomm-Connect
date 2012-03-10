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

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.IntegerAttribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.UriAttribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Action;
import org.mobicents.servlet.sip.restcomm.xml.rcml.FinishOnKey;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Length;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.NumDigits;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Play;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Say;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Timeout;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Voice;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class GatherTagStrategy extends RcmlTagStrategy {
  public GatherTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    try {
      answer(call);
    } catch(final InterruptedException ignored) { return; }
    try {
	  // Collect some digits.
	  final List<URI> announcements = getAnnouncements(tag.getChildren());
      final String finishOnKey = tag.getAttribute(FinishOnKey.NAME).getValue();
      final int numDigits = ((IntegerAttribute)tag.getAttribute(NumDigits.NAME)).getIntegerValue();
      final int timeout = ((IntegerAttribute)tag.getAttribute(Timeout.NAME)).getIntegerValue();
      call.playAndCollect(announcements, numDigits, 1,timeout, timeout, finishOnKey);
      final String digits = call.getDigits();
      // Redirect to action URI.
      URI action = null;
      final URI base = interpreter.getCurrentUri();
      final Attribute attribute = tag.getAttribute(Action.NAME);
      if(attribute == null) {
        action = base;
      } else {
        action = ((UriAttribute)attribute).getUriValue();
      }
      final URI uri = resolveIfNotAbsolute(base, action);
      final String method = tag.getAttribute(Method.NAME).getValue();
      final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
      parameters.add(new BasicNameValuePair("Digits", digits));
      interpreter.loadResource(uri, method, parameters);
      interpreter.redirect();
    } catch(final Exception exception) {
      interpreter.failed();
      final StringBuilder buffer = new StringBuilder();
      buffer.append("There was an error while gathering digits.");
      throw new TagStrategyException(buffer.toString(), exception);
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
}

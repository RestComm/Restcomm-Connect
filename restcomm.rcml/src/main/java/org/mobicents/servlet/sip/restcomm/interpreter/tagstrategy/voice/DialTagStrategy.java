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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.net.URI;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.CallerId;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.HangupOnStar;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Record;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.RingbackTone;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.TimeLimit;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class DialTagStrategy extends RcmlTagStrategy {
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
    ringbackTone = URI.create("file://" + configuration.getString("ringback-audio-file"));
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    RcmlTagStrategy strategy = null;
    final String text = tag.getText();
	if(text != null && !text.isEmpty()) {
	  strategy = new BridgeSubStrategy(action, method, timeout, timeLimit, callerId, ringbackTone, record);
	} else {
	  final List<Tag> children = tag.getChildren();
	  if(hasConferenceTag(children)) {
	    strategy = new ConferenceSubStrategy(action, method, timeLimit, record);
	  } else {
	    strategy = new ForkSubStrategy(action, method, timeout, timeLimit, callerId, ringbackTone, record);
	  }
	}
	strategy.initialize(interpreter, context, tag);
	strategy.execute(interpreter, context, tag);
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
      callerId = PhoneNumberUtil.getInstance().parse(value, "US");
    } catch(final NumberParseException exception) {
      interpreter.notify(context, Notification.WARNING, 13214);
      throw new TagStrategyException(exception);
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
}

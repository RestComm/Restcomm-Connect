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
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.DialTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.FaxTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.GatherTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.HangupTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.PauseTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.PlayTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.RecordTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.RedirectTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.RejectTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.SayTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.SmsTagStrategy;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Dial;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Fax;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Gather;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Hangup;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Play;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Record;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Redirect;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Reject;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Say;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Sms;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class TagStrategyFactory {
  private static final Map<String, Class<? extends TagStrategy>> STRATEGIES;
  static {
    STRATEGIES = new HashMap<String, Class<? extends TagStrategy>>();
    STRATEGIES.put(Say.NAME, SayTagStrategy.class);
    STRATEGIES.put(Play.NAME, PlayTagStrategy.class);
    STRATEGIES.put(Gather.NAME, GatherTagStrategy.class);
    STRATEGIES.put(Record.NAME, RecordTagStrategy.class);
    STRATEGIES.put(Sms.NAME, SmsTagStrategy.class);
    STRATEGIES.put(Dial.NAME, DialTagStrategy.class);
    STRATEGIES.put(Hangup.NAME, HangupTagStrategy.class);
    STRATEGIES.put(Redirect.NAME, RedirectTagStrategy.class);
    STRATEGIES.put(Reject.NAME, RejectTagStrategy.class);
    STRATEGIES.put(Pause.NAME, PauseTagStrategy.class);
    STRATEGIES.put(Fax.NAME, FaxTagStrategy.class);
  }
  
  public TagStrategyFactory() {
    super();
  }
  
  public TagStrategy getTagStrategyInstance(String name) throws TagStrategyInstantiationException {
    if(name == null) {
      throw new NullPointerException("Can not instantiate a strategy for a null tag name.");
	} else if(!STRATEGIES.containsKey(name)) {
      throw new TagStrategyInstantiationException("The <" + name + "> tag does not have a suitable strategy.");
	} else {
	  try {
		return STRATEGIES.get(name).newInstance();
	  } catch(final InstantiationException exception) {
		throw new TagStrategyInstantiationException(exception);
	  } catch(final IllegalAccessException exception) {
		throw new TagStrategyInstantiationException(exception);
	  }
	}
  }
}

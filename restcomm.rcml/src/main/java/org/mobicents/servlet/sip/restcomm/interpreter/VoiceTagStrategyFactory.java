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

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.DialTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.FaxTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.GatherTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.HangupTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.PauseTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.PlayTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.RecordTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.RedirectTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.RejectTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.SayTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice.SmsTagStrategy;
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
@ThreadSafe public final class VoiceTagStrategyFactory extends TagStrategyFactory {
  private static final Map<String, Class<? extends TagStrategy>> strategies;
  static {
    strategies = new HashMap<String, Class<? extends TagStrategy>>();
    strategies.put(Say.NAME, SayTagStrategy.class);
    strategies.put(Play.NAME, PlayTagStrategy.class);
    strategies.put(Gather.NAME, GatherTagStrategy.class);
    strategies.put(Record.NAME, RecordTagStrategy.class);
    strategies.put(Sms.NAME, SmsTagStrategy.class);
    strategies.put(Dial.NAME, DialTagStrategy.class);
    strategies.put(Hangup.NAME, HangupTagStrategy.class);
    strategies.put(Redirect.NAME, RedirectTagStrategy.class);
    strategies.put(Reject.NAME, RejectTagStrategy.class);
    strategies.put(Pause.NAME, PauseTagStrategy.class);
    strategies.put(Fax.NAME, FaxTagStrategy.class);
  }
	
  public VoiceTagStrategyFactory() {
    super(strategies);
  }
}

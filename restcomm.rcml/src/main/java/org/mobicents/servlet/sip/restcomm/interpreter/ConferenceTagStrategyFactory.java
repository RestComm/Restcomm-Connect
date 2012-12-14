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

import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.conference.PauseTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.conference.PlayTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.conference.RedirectTagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.conference.SayTagStrategy;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Play;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Redirect;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Say;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ConferenceTagStrategyFactory extends TagStrategyFactory {
  private static final Map<String, Class<? extends TagStrategy>> strategies;
  static {
    strategies = new HashMap<String, Class<? extends TagStrategy>>();
    strategies.put(Say.NAME, SayTagStrategy.class);
    strategies.put(Play.NAME, PlayTagStrategy.class);
    strategies.put(Redirect.NAME, RedirectTagStrategy.class);
    strategies.put(Pause.NAME, PauseTagStrategy.class);
  }
	  
  public ConferenceTagStrategyFactory() {
	super(strategies);
  }
}

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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.conference;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.ConferenceRcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class PauseTagStrategy extends ConferenceRcmlTagStrategy {
  private int length;
  
  public PauseTagStrategy() {
    super();
  }
  
  @Override public synchronized void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	final ConferenceRcmlInterpreterContext conferenceContext = (ConferenceRcmlInterpreterContext)context;
	final Conference.Status status = conferenceContext.getConference().getStatus();
    if(Conference.Status.IN_PROGRESS.equals(status)) {
      try { wait(length * TimeUtils.SECOND_IN_MILLIS); }
      catch(final InterruptedException ignored) { return; }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
	      final RcmlTag tag) throws TagStrategyException {
    length = getLength(interpreter, context, tag);
    if(length == -1) {
      interpreter.notify(context, Notification.WARNING, 13910);
      length = 1;
    }
  }
}

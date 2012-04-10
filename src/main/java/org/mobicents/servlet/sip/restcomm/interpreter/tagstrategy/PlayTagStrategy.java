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

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class PlayTagStrategy extends RcmlTagStrategy {
  private int loop;
  private URI uri;

  public PlayTagStrategy() {
    super();
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
    final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    if(uri != null) {
      final List<URI> announcement = new ArrayList<URI>();
      announcement.add(uri);
      try {
        final Call call = context.getCall();
    	if(loop == 0) {
    	  while(Call.Status.IN_PROGRESS == call.getStatus()) {
    	    call.play(announcement, 1);
    	  }
    	} else {
          call.play(announcement, loop);
    	}
      } catch(final CallException exception) {
        interpreter.failed();
        notify(interpreter, context, tag, Notification.ERROR, 12400);
        throw new TagStrategyException(exception);
      } catch(final InterruptedException ignored) { return; }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    try {
      loop = getLoop(interpreter, context, tag);
      if(loop == -1) {
        notify(interpreter, context, tag, Notification.WARNING, 13410);
        loop = 1;
      }
      uri = getUri(interpreter, context, tag);
      if(uri == null) {
        notify(interpreter, context, tag, Notification.ERROR, 13420);
      }
    } catch(final IllegalArgumentException exception) {
      notify(interpreter, context, tag, Notification.ERROR, 11100);
    }
  }
}

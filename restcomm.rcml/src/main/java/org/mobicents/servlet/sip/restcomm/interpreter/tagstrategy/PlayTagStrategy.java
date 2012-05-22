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

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class PlayTagStrategy extends RcmlTagStrategy {
  private static final Logger logger = Logger.getLogger(PlayTagStrategy.class);

  private int loop;
  private URI uri;

  public PlayTagStrategy() {
    super();
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final List<URI> announcement = new ArrayList<URI>();
    announcement.add(uri);
    try {
      final Call call = context.getCall();
      if(loop == 0) {
    	while(Call.Status.IN_PROGRESS == call.getStatus()) {
    	  call.play(announcement, 1);
    	}
    	interpreter.finish();
      } else {
        call.play(announcement, loop);
      }
    } catch(final CallException exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 12400);
      logger.error(exception);
      throw new TagStrategyException(exception);
    }
  }
  
  private void initLoop(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    loop = getLoop(interpreter, context, tag);
    if(loop == -1) {
      interpreter.notify(context, Notification.WARNING, 13410);
      loop = 1;
    }
  }
  
  private void initUri(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      uri = getUri(interpreter, context, tag);
      if(uri == null) {
    	interpreter.failed();
        interpreter.notify(context, Notification.ERROR, 13420);
        throw new TagStrategyException("There is no resource to play.");
      }
    } catch(final IllegalArgumentException exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 11100);
      throw new TagStrategyException(tag.getText() + " is an invalid URI.");
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    initLoop(interpreter, context, tag);
    initUri(interpreter, context, tag);
  }
}

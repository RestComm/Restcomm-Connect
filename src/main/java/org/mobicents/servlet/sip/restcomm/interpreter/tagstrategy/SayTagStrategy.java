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
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SayTagStrategy extends RcmlTagStrategy  {
  private String gender;
  private String language;
  private int loop;
  private String text;
  
  public SayTagStrategy() {
    super();
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    if(text != null) {
      final List<URI> announcement = new ArrayList<URI>();
      announcement.add(say(gender, language, text));
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
		interpreter.notify(context, Notification.ERROR, 12400);
		throw new TagStrategyException(exception);
	  } catch(final InterruptedException ignored) { return; }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	super.initialize(interpreter, context, tag);
    gender = getGender(interpreter, context, tag);
    if(gender == null) {
      interpreter.notify(context, Notification.WARNING, 13511);
      gender = "man";
    }
    language = getLanguage(interpreter, context, tag);
    if(language == null) {
      language = "en";
    }
    loop = getLoop(interpreter, context, tag);
    if(loop == -1) {
      interpreter.notify(context, Notification.WARNING, 13510);
      loop = 1;
    }
    text = tag.getText();
    if(text == null || text.isEmpty()) {
  	  interpreter.notify(context, Notification.WARNING, 13520);
  	}
  }
}

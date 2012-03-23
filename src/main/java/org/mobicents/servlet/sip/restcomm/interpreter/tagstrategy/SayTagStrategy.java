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
import java.util.List;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.IntegerAttribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Loop;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Voice;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SayTagStrategy extends RcmlTagStrategy  {
  private final DaoManager daos;
  
  public SayTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    daos = services.get(DaoManager.class);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
	final Call call = context.getCall();
	try {
      answer(call);
	} catch(final InterruptedException ignored) { return; }
    final String text = tag.getText();
    if(text != null) {
      final String gender = tag.getAttribute(Voice.NAME).getValue();
      final String language = tag.getAttribute(Language.NAME).getValue();
      final List<URI> announcement = say(gender, language, text);
      final int iterations = ((IntegerAttribute)tag.getAttribute(Loop.NAME)).getIntegerValue();
      try {
    	if(iterations == 0) {
    	  while(Call.Status.IN_PROGRESS == call.getStatus()) {
    	    call.play(announcement, 1);
    	  }
    	} else {
		  call.play(announcement, iterations);
    	}
	  } catch(final CallException exception) {
		interpreter.failed();
		final StringBuilder buffer = new StringBuilder();
		buffer.append("There was an error while saying ").append(text).append(" by a ");
		buffer.append(language).append(" speaking ").append(gender).append(".");
		throw new TagStrategyException(buffer.toString(), exception);
	  } catch(final InterruptedException ignored) { return; }
    }
  }
}

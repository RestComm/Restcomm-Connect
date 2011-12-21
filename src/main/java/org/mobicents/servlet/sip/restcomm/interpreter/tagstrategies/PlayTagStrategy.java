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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategies;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.Interpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Loop;

public final class PlayTagStrategy extends TwiMLTagStrategy {
  
  public PlayTagStrategy() {
    super();
  }
  
  @Override public void execute(final Interpreter interpreter,
    final InterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
	// Try to answer the call if it hasn't been done so already.
    answer(call);
    // Play something.
    final String text = tag.getText();
    if(text != null) {
      final int loop = Integer.parseInt(tag.getAttribute(Loop.NAME).getValue());
      final URI uri = URI.create(text);
      final List<URI> announcements = new ArrayList<URI>();
      announcements.add(uri);
      try {
        call.play(announcements, loop);
      } catch(final CallException exception) {
        interpreter.failed();
        throw new TagStrategyException(exception);
      }
    }
  }
}

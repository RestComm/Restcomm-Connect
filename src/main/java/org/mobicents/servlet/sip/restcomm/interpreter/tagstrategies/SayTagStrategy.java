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

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaException;
import org.mobicents.servlet.sip.restcomm.callmanager.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Loop;

public final class SayTagStrategy extends TwiMLTagStrategy  {
  
  public SayTagStrategy() {
    super();
  }
  
  @Override public void execute(final TwiMLInterpreter interpreter,
      final TwiMLInterpreterContext context, final Tag tag) throws TagStrategyException {
	final Call call = context.getCall();
	// Try to answer the call if it hasn't been done so already.
    answer(call);
    // Say something.
    final SpeechSynthesizer synthesizer = call.getSpeechSynthesizer();
    final String text = tag.getText();
    if(text != null) {
      final int loop = Integer.parseInt(tag.getAttribute(Loop.NAME).getValue());
      try {
        for(int counter = 1; counter <= loop; counter++) {
          synthesizer.speak(text);
        }
      } catch(final MediaException exception) {
        interpreter.failed();
        throw new TagStrategyException(exception);
      }
    }
  }
}

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
import org.mobicents.servlet.sip.restcomm.callmanager.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventType;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEventType;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Language;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Loop;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Voice;

public final class SayTagStrategy extends TwiMLTagStrategy  {
  private final EventListener<SpeechSynthesizerEvent> listener;
  
  public SayTagStrategy() {
    super();
    listener = new SpeechSynthesizerEventListener(this);
  }
  
  @Override public void execute(final TwiMLInterpreter interpreter,
      final TwiMLInterpreterContext context, final Tag tag) throws TagStrategyException {
	final Call call = context.getCall();
	// Try to answer the call if it hasn't been done so already.
    answer(call);
    // Setup the speech synthesizer and say something.
    final SpeechSynthesizer synthesizer = call.getSpeechSynthesizer();
    final String text = tag.getText();
    if(text != null) {
      final String voice = tag.getAttribute(Voice.NAME).getValue();
      final String language = tag.getAttribute(Language.NAME).getValue();
      final int loop = Integer.parseInt(tag.getAttribute(Loop.NAME).getValue());
      synthesizer.setVoice(voice);
      synthesizer.setLanguage(language);
      synthesizer.addListener(listener);
      for(int counter = 1; counter <= loop; counter++) {
        synthesizer.speak(text);
        synchronized(this) {
          try {
            wait();
          } catch(final InterruptedException exception) {
            throw new TagStrategyException(exception);
          }
        }
      }
      synthesizer.removeListener(listener);
    }
  }
  
  private final class SpeechSynthesizerEventListener implements EventListener<SpeechSynthesizerEvent> {
	private final Object sleeper;
	
    private SpeechSynthesizerEventListener(final Object sleeper) {
      super();
      this.sleeper = sleeper;
    }

	public void onEvent(final SpeechSynthesizerEvent event) {
	  // Handle the event.
      final EventType type = event.getType();
      if(type.equals(SpeechSynthesizerEventType.DONE_SPEAKING)) {
        synchronized(sleeper) {
          sleeper.notify();
        }
      }
	}
  }
}

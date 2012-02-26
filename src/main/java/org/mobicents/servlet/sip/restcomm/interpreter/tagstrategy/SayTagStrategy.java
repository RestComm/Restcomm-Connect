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

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Language;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Loop;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Voice;

public final class SayTagStrategy extends TwiMLTagStrategy  {
  
  public SayTagStrategy() {
    super();
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
	final Call call = context.getCall();
	// Try to answer the call if it hasn't been done so already.
    // answer(call);
    // Say something.
    final String text = tag.getText();
    final String gender = tag.getAttribute(Voice.NAME).getValue();
    final String language = tag.getAttribute(Language.NAME).getValue();
    if(text != null) {
      final ServiceLocator services = ServiceLocator.getInstance();
      final SpeechSynthesizer synthesizer = services.get(SpeechSynthesizer.class);
      final URI uri = synthesizer.synthesize(text, gender, language);
      System.out.println(uri.toString());
    }
  }
}

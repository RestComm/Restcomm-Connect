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

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class RcmlTagStrategy implements TagStrategy {
  private final URI silenceAudioFile;

  public RcmlTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    silenceAudioFile = URI.create("file://" + configuration.getString("silence-audio-file"));
  }
  
  protected void answer(final Call call) throws TagStrategyException, InterruptedException {
	try {
      if(Call.Status.RINGING == call.getStatus()) {
        call.answer();
      }
	} catch(final CallException exception) {
	  throw new TagStrategyException("There was a problem answering the phone call.", exception);
	}
  }
  
  protected List<URI> pause(final int seconds) {
    final List<URI> silence = new ArrayList<URI>();
    for(int count = 1; count <= seconds; count++) {
      silence.add(silenceAudioFile);
    }
    return silence;
  }
  
  protected List<URI> say(final String gender, final String language, final String text) {
    final ServiceLocator services = ServiceLocator.getInstance();
    final SpeechSynthesizer synthesizer = services.get(SpeechSynthesizer.class);
    final URI uri = synthesizer.synthesize(text, gender, language);
    final List<URI> announcements = new ArrayList<URI>();
    announcements.add(uri);
    return announcements;
  }
}

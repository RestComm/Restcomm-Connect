package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategy;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;

public abstract class RcmlTagStrategy implements TagStrategy {
  public RcmlTagStrategy() {
    super();
  }
  
  protected void answer(final Call call) throws TagStrategyException {
	try {
      if(Call.Status.RINGING == call.getStatus()) {
        call.answer();
      }
	} catch(final CallException exception) {
	  throw new TagStrategyException("There was a problem answering the phone call.", exception);
	}
  }
  
  protected List<URI> pause(final int seconds) {
    final List<URI> announcements = new ArrayList<URI>();
    // Add empty audio in 1 second increments.
    return announcements;
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

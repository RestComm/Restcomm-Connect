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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaPlayer;
import org.mobicents.servlet.sip.restcomm.callmanager.DtmfDetector;
import org.mobicents.servlet.sip.restcomm.callmanager.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.EventType;
import org.mobicents.servlet.sip.restcomm.callmanager.events.PlayerEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.PlayerEventType;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalDetectorEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SignalDetectorEventType;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEventType;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;
import org.mobicents.servlet.sip.restcomm.resourceserver.ResourceDescriptor;
import org.mobicents.servlet.sip.restcomm.util.UrlUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Action;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Language;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Length;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Loop;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Method;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Pause;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Play;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Say;
import org.mobicents.servlet.sip.restcomm.xml.twiml.TwiMLTag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.NumDigits;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Voice;

public final class GatherTagStrategy extends TwiMLTagStrategy {
  private static final Logger logger = Logger.getLogger(GatherTagStrategy.class);
  private static final int ONE_SECOND = 1000;
  
  private final EventListener<SignalDetectorEvent> detectorListener;
  private final EventListener<PlayerEvent> playerListener;
  private final EventListener<SpeechSynthesizerEvent> synthesizerListener;
  
  private TwiMLInterpreterContext context;
  private int childIndex;
  private String buffer;
  private Tag tag;

  public GatherTagStrategy() {
    super();
    this.detectorListener = new SignalDetectorEventListener(this);
    this.playerListener = new PlayerEventListener(this);
    this.synthesizerListener = new SpeechSynthesizerEventListener(this);
    this.context = null;
    this.childIndex = 0;
    this.buffer = null;
    this.tag = null;
  }

  @Override public void execute(final TwiMLInterpreter interpreter,
      final TwiMLInterpreterContext context, final Tag tag) throws TagStrategyException {
	init(tag, context);
    // Try to answer the call if it hasn't been done so already.
    final Call call = context.getCall();
	answer(call);
	// Make sure any children don't get visited by the interpreter.
    visitChildren(tag.getChildren());
    // Initialize the signal detector.
    // final String finishOnKey = tag.getAttribute(FinishOnKey.NAME).getValue();
    final int numberOfDigits = Integer.parseInt(tag.getAttribute(NumDigits.NAME).getValue());
    // final long timeout = Integer.parseInt(tag.getAttribute(Timeout.NAME).getValue()) * ONE_SECOND;
    final DtmfDetector detector = call.getSignalDetector();
    detector.setNumberOfDigits(numberOfDigits);
    // Start gathering digits.
    detector.addListener(detectorListener);
    detector.detect();
    // Execute first child tag.
    executeNextChild();
    synchronized(this) {
      try {
        wait();
      } catch(final InterruptedException exception) {
        throw new TagStrategyException(exception);
      }
    }
    detector.removeListener(detectorListener);
    // See if we got some digits
    // Do something with the digits.
    if(!buffer.isEmpty()) {
      final Attribute action = tag.getAttribute(Action.NAME);
      final URI base = interpreter.getDescriptor().getUri();
      URI uri = null;
      if(action != null) {
    	uri = base.resolve(action.getValue());
      } else {
    	uri = base;
      }
      final ResourceDescriptor descriptor = getResourceDescriptor(uri, buffer);
      try {
	    interpreter.loadResource(descriptor);
	  } catch(final InterpreterException exception) {
	    throw new TagStrategyException(exception);
	  }
      interpreter.redirect();
    }
  }
  
  private void executeNextChild() {
	final List<Tag> children = tag.getChildren();
    if(!children.isEmpty()) {
      if(childIndex < children.size()) {
        final Tag child = children.get(childIndex);
        childIndex++;
        // Execute the next child in the front of the list.
        if(child.getName().equals(Play.NAME)) {
          executePlay(child);
        } else if(child.getName().equals(Pause.NAME)) {
          executePause(child);
        } else if(child.getName().equals(Say.NAME)) {
          executeSay(child);
        }
      }
    }
  }
  
  private void executePlay(final Tag tag) {
    final String text = tag.getText();
    if(text != null) {
      final Call call = context.getCall();
      final URI uri = URI.create(text);
      final int loop = Integer.parseInt(tag.getAttribute(Loop.NAME).getValue());
      final MediaPlayer player = call.getPlayer();
      player.addListener(playerListener);
      for(int counter = 1; counter <= loop; counter++) {
        player.play(uri);
        synchronized(this) {
          try {
            wait();
          } catch(final InterruptedException exception) {
            logger.error(exception);
          }
        }
      }
      player.removeListener(playerListener);
    }
    executeNextChild();
  }
  
  private void executePause(final Tag tag) {
    final int length = Integer.parseInt(tag.getAttribute(Length.NAME).getValue());
    synchronized(this) {
      try {
        wait(length * ONE_SECOND);
      } catch(final InterruptedException exception) {
        logger.error(exception);
      }
    }
    executeNextChild();
  }
  
  private void executeSay(final Tag tag) {
    final String text = tag.getText();
    if(text != null) {
      final Call call = context.getCall();
      final SpeechSynthesizer synthesizer = call.getSpeechSynthesizer();
      final String voice = tag.getAttribute(Voice.NAME).getValue();
      final String language = tag.getAttribute(Language.NAME).getValue();
      final int loop = Integer.parseInt(tag.getAttribute(Loop.NAME).getValue());
      synthesizer.setVoice(voice);
      synthesizer.setLanguage(language);
      synthesizer.addListener(synthesizerListener);
      for(int counter = 1; counter <= loop; counter++) {
        synthesizer.speak(text);
        synchronized(this) {
          try {
            wait();
          } catch(final InterruptedException exception) {
            logger.error(exception);
          }
        }
      }
      synthesizer.removeListener(synthesizerListener);
    }
    executeNextChild();
  }
  
  private ResourceDescriptor getResourceDescriptor(final URI uri, final String digits) {
	final ResourceDescriptor descriptor = new ResourceDescriptor(uri);
    final String method = tag.getAttribute(Method.NAME).getValue();
    descriptor.setMethod(method);
    final Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("Digits", digits);
    if(method.equals(RequestMethod.GET)) {
      descriptor.setAttributes(attributes);
    } else if(method.equals(RequestMethod.POST)) {
      final String message = UrlUtils.toQueryString(attributes);
      descriptor.setMessage(message.getBytes());
    }
    return descriptor;
  }
  
  private void init(final Tag tag, final TwiMLInterpreterContext context) {
	this.context = context;
	this.tag = tag;
  }
  
  private void visitChildren(final List<Tag> children) {
    for(final Tag child : children) {
 	  ((TwiMLTag)child).setHasBeenVisited(true);
 	}
  }
  
  private final class PlayerEventListener implements EventListener<PlayerEvent> {
	private final Object sleeper;
    private PlayerEventListener(final Object sleeper) {
      super();
      this.sleeper = sleeper;
    }
    
	public void onEvent(PlayerEvent event) {
	  // Handle event
	  final EventType type = event.getType();
	  if(type.equals(PlayerEventType.DONE_PLAYING)) {
	    synchronized(sleeper) {
	      sleeper.notify();
	    }
	  }
	}
  }
  
  private final class SignalDetectorEventListener implements EventListener<SignalDetectorEvent> {
    private final Object sleeper;
    
    public SignalDetectorEventListener(final Object sleeper) {
      super();
      this.sleeper = sleeper;
    }
    
	public void onEvent(final SignalDetectorEvent event) {
	  // Handle the event.
      final EventType type = event.getType();
      if(type.equals(SignalDetectorEventType.DONE_DETECTING)) {
        buffer = event.getDigits();
      }
      synchronized(sleeper) {
        sleeper.notify();
      }
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

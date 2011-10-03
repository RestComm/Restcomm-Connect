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
package org.mobicents.servlet.sip.restcomm.callmanager;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEvent;
import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public abstract class SpeechSynthesizer extends FSM {
  // Synthesizer states.
  public static final State IDLE = new State("idle");
  public static final State SPEAKING = new State("speaking");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(SPEAKING);
    IDLE.addTransition(FAILED);
    SPEAKING.addTransition(IDLE);
    SPEAKING.addTransition(FAILED);
  }
  
  protected String language;
  protected String voice;
  
  private List<EventListener<SpeechSynthesizerEvent>> listeners;
  
  public SpeechSynthesizer() {
    super(IDLE);
    addState(IDLE);
    addState(SPEAKING);
    addState(FAILED);
    this.listeners = new ArrayList<EventListener<SpeechSynthesizerEvent>>();
  }
  
  public synchronized void addListener(final EventListener<SpeechSynthesizerEvent> listener) {
    listeners.add(listener);
  }
  
  public synchronized void removeListener(final EventListener<SpeechSynthesizerEvent> listener) {
    listeners.remove(listener);
  }
  
  protected synchronized void fire(final SpeechSynthesizerEvent event) {
	for(final EventListener<SpeechSynthesizerEvent> listener : listeners) {
	  listener.onEvent(event);
	}
  }

  public String getLanguage() {
    return language;
  }

  public String getVoice() {
    return voice;
  }
  
  public void setLanguage(final String language) {
    this.language = language;
  }

  public void setVoice(final String voice) {
    this.voice = voice;
  }
  
  public abstract void speak(String text);
}

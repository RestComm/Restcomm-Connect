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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class Jsr309SpeechSynthesizer extends FSM implements SpeechSynthesizer, MediaEventListener<PlayerEvent> {
  //Synthesizer states.
  private static final State IDLE = new State("idle");
  private static final State SPEAKING = new State("speaking");
  private static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(SPEAKING);
    IDLE.addTransition(FAILED);
    SPEAKING.addTransition(IDLE);
    SPEAKING.addTransition(FAILED);
  }

  // JSR-309 player.
  private final Player player;
  
  public Jsr309SpeechSynthesizer(final Player player) {
    super(IDLE);
    addState(IDLE);
    addState(SPEAKING);
    addState(FAILED);
    this.player = player;
  }
  
  @Override public synchronized void onEvent(final PlayerEvent event) {
    player.removeListener(this);
    setState(IDLE);
    if(event.isSuccessful()) {
      if(event.getEventType() == PlayerEvent.PLAY_COMPLETED) {
        notify();
      }
    }
  }
  
  @Override public synchronized void speak(final String text) throws MediaException {
    assertState(IDLE);
	final Parameters options = player.getMediaSession().createParameters();
	options.put(Player.BEHAVIOUR_IF_BUSY, Player.STOP_IF_BUSY);
	player.addListener(this);
	try {
	  player.play(createSpeechUri(text), null, options);
	  setState(SPEAKING);
	  wait();
	} catch(final Exception exception) {
	  setState(FAILED);
	  throw new MediaException(exception);
	}
  }
  
  public static URI createSpeechUri(final String text) {
    String encodedText = null;
    try {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("ts(").append(text).append(")");
      encodedText = URLEncoder.encode(buffer.toString(), "UTF-8");
    } catch(final UnsupportedEncodingException ignored) {
      // Should never happen.
    }
    return URI.create("data:" + encodedText);
  }
}

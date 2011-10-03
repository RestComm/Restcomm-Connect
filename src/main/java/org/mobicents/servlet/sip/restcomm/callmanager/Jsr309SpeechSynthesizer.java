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
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.SpeechSynthesizerEventType;

public final class Jsr309SpeechSynthesizer extends SpeechSynthesizer implements MediaEventListener<PlayerEvent> {
  // Logger.
  private static final Logger logger = Logger.getLogger(Jsr309SpeechSynthesizer.class);
  // JSR-309 player.
  private final Player player;
  
  public Jsr309SpeechSynthesizer(final Player player) {
    super();
    this.player = player;
  }
  
  public void onEvent(final PlayerEvent event) {
    player.removeListener(this);
    setState(IDLE);
    if(event.isSuccessful()) {
      if(event.getEventType() == PlayerEvent.PLAY_COMPLETED) {
        fire(new SpeechSynthesizerEvent(this, SpeechSynthesizerEventType.DONE_SPEAKING));
      }
    }
  }
  
  @Override public void speak(final String text) {
    assertState(IDLE);
	final Parameters options = player.getMediaSession().createParameters();
	options.put(Player.BEHAVIOUR_IF_BUSY, Player.STOP_IF_BUSY);
	player.addListener(this);
	try {
	  player.play(createSpeechUri(text), null, options);
	  setState(SPEAKING);
	} catch(final MsControlException exception) {
	  setState(FAILED);
	  logger.error(exception);
	  fire(new SpeechSynthesizerEvent(this, SpeechSynthesizerEventType.FAILED));
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

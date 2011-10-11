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

import java.net.URI;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;

import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class Jsr309MediaPlayer extends FSM implements MediaPlayer, MediaEventListener<PlayerEvent> {
  //Player states.
  public static final State IDLE = new State("idle");
  public static final State PLAYING = new State("playing");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(PLAYING);
    IDLE.addTransition(FAILED);
    PLAYING.addTransition(IDLE);
    PLAYING.addTransition(FAILED);
  }

  private final Player player;
  
  public Jsr309MediaPlayer(final Player player) {
    super(IDLE);
    addState(IDLE);
    addState(PLAYING);
    addState(FAILED);
    this.player = player;
  }
  
  @Override public synchronized void play(final URI uri) throws MediaException {
    assertState(IDLE);
	final Parameters options = player.getMediaSession().createParameters();
	options.put(Player.BEHAVIOUR_IF_BUSY, Player.STOP_IF_BUSY);
	player.addListener(this);
	try {
	  player.play(uri, null, options);
	  setState(PLAYING);
	  wait();
	} catch(final Exception exception) {
	  setState(FAILED);
	  throw new MediaException(exception);
	}
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
}

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
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.callmanager.events.PlayerEvent;
import org.mobicents.servlet.sip.restcomm.callmanager.events.PlayerEventType;

public final class Jsr309Player extends Player implements MediaEventListener<javax.media.mscontrol.mediagroup.PlayerEvent> {
  private static final Logger logger = Logger.getLogger(Jsr309Player.class);
  
  private final javax.media.mscontrol.mediagroup.Player player;
  
  public Jsr309Player(final javax.media.mscontrol.mediagroup.Player player) {
    super();
    this.player = player;
  }
  
  @Override public void play(final URI uri) {
    assertState(IDLE);
	final Parameters options = player.getMediaSession().createParameters();
	options.put(javax.media.mscontrol.mediagroup.Player.BEHAVIOUR_IF_BUSY,
    javax.media.mscontrol.mediagroup.Player.STOP_IF_BUSY);
	player.addListener(this);
	try {
	  player.play(uri, null, options);
	  setState(PLAYING);
	} catch(final MsControlException exception) {
	  setState(FAILED);
	  logger.error(exception);
	  fire(new PlayerEvent(this, PlayerEventType.FAILED));
	}
  }

  public void onEvent(final javax.media.mscontrol.mediagroup.PlayerEvent event) {
    player.removeListener(this);
    setState(IDLE);
	if(event.isSuccessful()) {
      if(event.getEventType() == javax.media.mscontrol.mediagroup.PlayerEvent.PLAY_COMPLETED) {
        fire(new PlayerEvent(this, PlayerEventType.DONE_PLAYING));
      }
    }
  }
}

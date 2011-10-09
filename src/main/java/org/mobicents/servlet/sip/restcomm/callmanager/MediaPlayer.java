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
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.callmanager.events.EventListener;
import org.mobicents.servlet.sip.restcomm.callmanager.events.PlayerEvent;
import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public abstract class MediaPlayer extends FSM {
  // Player states.
  public static final State IDLE = new State("idle");
  public static final State PLAYING = new State("playing");
  public static final State FAILED = new State("failed");
  static {
    IDLE.addTransition(PLAYING);
    IDLE.addTransition(FAILED);
    PLAYING.addTransition(IDLE);
    PLAYING.addTransition(FAILED);
  }
  
  private final List<EventListener<PlayerEvent>> listeners;
  
  public MediaPlayer() {
    super(IDLE);
    addState(IDLE);
    addState(PLAYING);
    addState(FAILED);
    this.listeners = new ArrayList<EventListener<PlayerEvent>>();
  }
  
  public synchronized void addListener(final EventListener<PlayerEvent> listener) {
    listeners.add(listener);
  }
  
  public synchronized void removeListener(final EventListener<PlayerEvent> listener) {
    listeners.remove(listener);
  }
  
  protected synchronized void fire(final PlayerEvent event) {
    for(final EventListener<PlayerEvent> listener : listeners) {
      listener.onEvent(event);
    }
  }
  
  public abstract void play(URI uri);
}

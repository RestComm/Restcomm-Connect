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

import javax.media.mscontrol.mixer.MediaMixer;

public final class Jsr309Conference implements Conference {
  private final MediaMixer mixer;
  
  public Jsr309Conference(final MediaMixer mixer) {
    super();
    this.mixer = mixer;
  }
  
  public MediaMixer getMixer() {
    return mixer;
  }

  @Override public String getName() {
    return null;
  }
}

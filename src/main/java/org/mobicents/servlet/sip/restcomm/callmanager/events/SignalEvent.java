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
package org.mobicents.servlet.sip.restcomm.callmanager.events;

import javax.media.mscontrol.MsControlFactory;
import javax.servlet.sip.SipServletRequest;

public final class SignalEvent extends Event<SignalEventType> {
  private MsControlFactory msControlFactory;
  private SipServletRequest request;
  
  public SignalEvent(final Object source, final SignalEventType type) {
    super(source, type);
  }
  
  public MsControlFactory getMsControlFactory() {
    return msControlFactory;
  }
  
  public SipServletRequest getRequest() {
    return request;
  }
  
  public void setMsControlFactory(final MsControlFactory factory) {
    this.msControlFactory = factory;
  }
  
  public void setRequest(final SipServletRequest request) {
    this.request = request;
  }
}

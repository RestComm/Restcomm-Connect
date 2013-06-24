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
package org.mobicents.servlet.restcomm.telephony;

import javax.servlet.sip.SipURI;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class InitializeOutbound {
  private final SipURI from;
  private final SipURI to;
  private final long timeout;
  private final boolean internal;
  
  public InitializeOutbound(final SipURI from, final SipURI to,
      final long timeout, final boolean internal) {
    super();
    this.from = from;
    this.to = to;
    this.timeout = timeout;
    this.internal = internal;
  }
  
  public SipURI from() {
    return from;
  }
  
  public SipURI to() {
    return to;
  }
  
  public long timeout() {
    return timeout;
  }
  
  public boolean internal() {
    return internal;
  }
}

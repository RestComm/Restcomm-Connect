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
package org.mobicents.servlet.restcomm.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class OpenConnection {
  private final ConnectionDescriptor descriptor;
  private final ConnectionMode mode;
  
  public OpenConnection(final ConnectionDescriptor descriptor,
      final ConnectionMode mode) {
    super();
    this.descriptor = descriptor;
    this.mode = mode;
  }
  
  public OpenConnection(final ConnectionMode mode) {
    this(null, mode);
  }
  
  public ConnectionDescriptor descriptor() {
    return descriptor;
  }
  
  public ConnectionMode mode() {
    return mode;
  }
}

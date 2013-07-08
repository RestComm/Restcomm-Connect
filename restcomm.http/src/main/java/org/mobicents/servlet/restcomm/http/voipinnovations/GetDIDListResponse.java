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
package org.mobicents.servlet.restcomm.http.voipinnovations;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class GetDIDListResponse {
  private final String name;
  private final String status;
  private final int code;
  private final State state;

  public GetDIDListResponse(final String name, final String status, final int code,
      final State state) {
    super();
    this.name = name;
    this.status = status;
    this.code = code;
    this.state = state;
  }
  
  public String name() {
    return name;
  }
  
  public String status() {
    return status;
  }
  
  public int code() {
    return code;
  }
  
  public State state() {
    return state;
  }
}

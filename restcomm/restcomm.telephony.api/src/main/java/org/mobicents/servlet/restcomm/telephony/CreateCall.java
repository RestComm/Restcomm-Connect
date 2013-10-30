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

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@Immutable public final class CreateCall {
  public static enum Type { CLIENT, PSTN, SIP };

  private final String from;
  private final String to;
  private final String username;
  private final String password;
  private final boolean isFromApi;
  private final int timeout;
  private final Type type;
  private final Sid accountId;
  
  public CreateCall(final String from, final String to, final String username, final String password,
      final boolean isFromApi, final int timeout, final Type type, final Sid accountId) {
    super();
    this.from = from;
    this.to = to;
    this.username = username;
    this.password = password;
    this.isFromApi = isFromApi;
    this.timeout = timeout;
    this.type = type;
    this.accountId = accountId;
  }
  
  public String from() {
    return from;
  }
  
  public String to() {
    return to;
  }
  
  public boolean isFromApi() {
    return isFromApi;
  }
  
  public int timeout() {
    return timeout;
  }
  
  public Type type() {
    return type;
  }

  public Sid accountId() {
      return accountId;
  }

  public String username() {
      return username;
  }

  public String password() {
      return password;
  }
}

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
package org.mobicents.servlet.sip.restcomm.entities;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class PresenceRecord {
  private final String aor;
  private final String name;
  private final String user;
  private final String uri;
  private final String ua;
  private final int ttl;
  private final DateTime expires;
  
  public PresenceRecord(final String aor, final String name, final String user, final String uri,
      final String ua, final int ttl) {
    this(aor, name, user, uri, ua, ttl, DateTime.now().plusSeconds(ttl));
  }

  public PresenceRecord(final String aor, final String name, final String user, final String uri,
      final String ua,final int ttl, final DateTime expires) {
    super();
    this.aor = aor;
    this.name = name;
    this.user = user;
    this.uri = uri;
    this.ua = ua;
    this.ttl = ttl;
    this.expires = expires;
  }

  public String getAddressOfRecord() {
    return aor;
  }

  public String getDisplayName() {
    return name;
  }
  
  public String getUser() {
    return user;
  }

  public String getUri() {
    return uri;
  }
  
  public String getUserAgent() {
    return ua;
  }

  public int getTimeToLive() {
    return ttl;
  }
  
  public DateTime getExpires() {
    return expires;
  }
  
  public PresenceRecord setTimeToLive(final int ttl) {
    return new PresenceRecord(aor, name, user, uri, ua, ttl);
  }
}

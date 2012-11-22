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

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Registration {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final DateTime dateExpires;
  private final String addressOfRecord;
  private final String displayName;
  private final String userName;
  private final int timeToLive;
  private final String location;
  private final String userAgent;
  
  public Registration(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String addressOfRecord,
      final String displayName, final String userName, final String userAgent, final int timeToLive, final String location) {
    this(sid, dateCreated, dateUpdated, DateTime.now().plusSeconds(timeToLive), addressOfRecord, displayName, userName,
        userAgent, timeToLive, location);
  }

  public Registration(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final DateTime dateExpires,
      final String addressOfRecord, final String displayName, final String userName, final String userAgent, final int timeToLive,
      final String location) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.dateExpires = dateExpires;
    this.addressOfRecord = addressOfRecord;
    this.displayName = displayName;
    this.userName = userName;
    this.location = location;
    this.userAgent = userAgent;
    this.timeToLive = timeToLive;
  }
  
  public Sid getSid() {
    return sid;
  }
  
  public DateTime getDateCreated() {
    return dateCreated;
  }
  
  public DateTime getDateUpdated() {
    return dateUpdated;
  }
  
  public DateTime getDateExpires() {
    return dateExpires;
  }

  public String getAddressOfRecord() {
    return addressOfRecord;
  }

  public String getDisplayName() {
    return displayName;
  }
  
  public String getUserName() {
    return userName;
  }

  public String getLocation() {
    return location;
  }
  
  public String getUserAgent() {
    return userAgent;
  }

  public int getTimeToLive() {
    return timeToLive;
  }
  
  public Registration setTimeToLive(final int timeToLive) {
    return new Registration(sid, dateCreated, DateTime.now(), DateTime.now().plusSeconds(timeToLive), addressOfRecord,
        displayName, userName, userAgent, timeToLive, location);
  }
}

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
package org.mobicents.servlet.sip.restcomm;

import java.math.BigDecimal;
import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class CallDetailRecord {
  private final Sid sid;
  private final Sid parentCallSid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final String to;
  private final String from;
  private final Sid phoneNumberSid;
  private final String status;
  private final DateTime startTime;
  private final DateTime endTime;
  private final Integer duration;
  private final BigDecimal price;
  private final String answeredBy;
  private final String forwardedFrom;
  private final String callerName;
  private final URI uri;

  public CallDetailRecord(final Sid sid, final Sid parentCallSid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final String to, final String from , final Sid phoneNumberSid, final String status, final DateTime startTime, final DateTime endTime,
      final Integer duration, final BigDecimal price, final String answeredBy, final String forwardedFrom, final String callerName, final URI uri) {
    super();
    this.sid = sid;
    this.parentCallSid = parentCallSid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.to = to;
    this.from = from;
    this.phoneNumberSid = phoneNumberSid;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.price = price;
    this.answeredBy = answeredBy;
    this.forwardedFrom = forwardedFrom;
    this.callerName = callerName;
    this.uri = uri;
  }

  public Sid getSid() {
    return sid;
  }

  public Sid getParentCallSid() {
    return parentCallSid;
  }

  public DateTime getDateCreated() {
    return dateCreated;
  }

  public DateTime getDateUpdated() {
    return dateUpdated;
  }

  public Sid getAccountSid() {
    return accountSid;
  }

  public String getTo() {
    return to;
  }

  public String getFrom() {
    return from;
  }

  public Sid getPhoneNumberSid() {
    return phoneNumberSid;
  }

  public String getStatus() {
    return status;
  }

  public DateTime getStartTime() {
    return startTime;
  }

  public DateTime getEndTime() {
    return endTime;
  }

  public Integer getDuration() {
    return duration;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getAnsweredBy() {
    return answeredBy;
  }

  public String getForwardedFrom() {
    return forwardedFrom;
  }

  public String getCallerName() {
    return callerName;
  }

  public URI getUri() {
    return uri;
  }
  
  public CallDetailRecord setStatus(final String status) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  public CallDetailRecord setStartTime(final DateTime startTime) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  public CallDetailRecord setEndTime(final DateTime endTime) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  public CallDetailRecord setDuration(final Integer duration) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  public CallDetailRecord setPrice(final BigDecimal price) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  public CallDetailRecord setAnsweredBy(final String answeredBy) {
    final DateTime dateUpdated = DateTime.now();
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
}

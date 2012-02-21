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
@Immutable public final class SmsMessage {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final DateTime dateSent;
  private final Sid accountSid;
  private final String sender;
  private final String recipient;
  private final String body;
  private final String status;
  private final String direction;
  private final BigDecimal price;
  private final String apiVersion;
  private final URI uri;
  
  public SmsMessage(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final DateTime dateSent,
      final Sid accountSid, final String sender, final String recipient, final String body, final String status,
      final String direction, final BigDecimal price, final String apiVersion, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.dateSent = dateSent;
    this.accountSid = accountSid;
    this.sender = sender;
    this.recipient = recipient;
    this.body = body;
    this.status = status;
    this.direction = direction;
    this.price = price;
    this.apiVersion = apiVersion;
    this.uri = uri;
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

  public DateTime getDateSent() {
    return dateSent;
  }

  public Sid getAccountSid() {
    return accountSid;
  }

  public String getSender() {
    return sender;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getBody() {
    return body;
  }

  public String getStatus() {
    return status;
  }

  public String getDirection() {
    return direction;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public URI getUri() {
    return uri;
  }
}

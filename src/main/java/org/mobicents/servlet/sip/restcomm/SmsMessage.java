package org.mobicents.servlet.sip.restcomm;

import java.math.BigDecimal;
import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

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

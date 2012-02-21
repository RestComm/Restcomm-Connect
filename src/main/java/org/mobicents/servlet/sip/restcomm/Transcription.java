package org.mobicents.servlet.sip.restcomm;

import java.math.BigDecimal;
import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

@Immutable public final class Transcription {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final String status;
  private final Sid recordingSid;
  private final Integer duration;
  private final String transcriptionText;
  private final BigDecimal price;
  private final URI uri;

  public Transcription(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final String status, final Sid recordingSid, final Integer duration, final String transcriptionText, final BigDecimal price,
      final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.status = status;
    this.recordingSid = recordingSid;
    this.duration = duration;
    this.transcriptionText = transcriptionText;
    this.price = price;
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

  public Sid getAccountSid() {
    return accountSid;
  }

  public String getStatus() {
    return status;
  }

  public Sid getRecordingSid() {
    return recordingSid;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getTranscriptionText() {
    return transcriptionText;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public URI getUri() {
    return uri;
  }
}

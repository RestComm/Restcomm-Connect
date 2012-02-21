package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

@Immutable public final class Recording {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final Sid callSid;
  private final Integer duration;
  private final String apiVersion;
  private final URI uri;

  public Recording(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final Sid callSid, final Integer duration, final String apiVersion, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.callSid = callSid;
    this.duration = duration;
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

  public Sid getAccountSid() {
    return accountSid;
  }

  public Sid getCallSid() {
    return callSid;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public URI getUri() {
    return uri;
  }
}

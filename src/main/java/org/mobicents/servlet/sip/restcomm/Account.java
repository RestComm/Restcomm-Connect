package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

public final class Account {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final Type type;
  private final Status status;
  private final String authToken;
  private final URI uri;
  
  public Account(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final Type type, final Status status, final String authToken, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.friendlyName = friendlyName;
    this.type = type;
    this.status = status;
    this.authToken = authToken;
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
  
  public String getFriendlyName() {
    return friendlyName;
  }
  
  public Type getType() {
    return type;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public String getAuthToken() {
    return authToken;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public enum Status {
    ACTIVE, CLOSED, SUSPENDED
  };
  
  public enum Type {
    FULL, TRIAL
  };
}

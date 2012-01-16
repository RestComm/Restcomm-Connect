package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

@Immutable public final class Account {
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
  
  public Account setDateUpdated(final DateTime dateUpdated) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public Account setFriendlyName(final String friendlyName) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public Account setType(final Type type) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public Account setStatus(final Status status) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public Account setAuthToken(final String authToken) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public Account setUri(final URI uri) {
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  public enum Status {
    ACTIVE("active"),
    CLOSED("closed"),
    SUSPENDED("suspended");
    
    private final String text;
    
    private Status(final String text) {
      this.text = text;
    }
    
    public static Status getValueOf(final String text) {
      Status[] values = values();
      for(final Status value : values) {
        if(value.text.equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid account status.");
    }
    
    @Override public String toString() {
      return text;
    }
  };
  
  public enum Type {
    FULL("Full"),
    TRIAL("Trial");
    
    private final String text;
    
    private Type(final String text) {
      this.text = text;
    }
    
    public static Type getValueOf(final String text) {
      Type[] values = values();
      for(final Type value : values) {
        if(value.text.equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid account type.");
    }
    
    @Override public String toString() {
      return text;
    }
  };
}

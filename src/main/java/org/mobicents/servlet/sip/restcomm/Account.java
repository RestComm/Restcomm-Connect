package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

public interface Account {
  public enum Status {ACTIVE, CLOSED, SUSPENDED};
  public enum Type {FULL, TRIAL};
  public String getSid();
  public DateTime getDateCreated();
  public DateTime getDateUpdated();
  public String getFriendlyName();
  public Type getType();
  public Status getStatus();
  public String getAuthToken();
  public URI getUri();
}

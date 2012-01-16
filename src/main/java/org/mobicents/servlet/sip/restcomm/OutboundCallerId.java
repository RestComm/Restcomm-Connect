package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

public interface OutboundCallerId {
  public String getSid();
  public DateTime getDateCreated();
  public DateTime getDateUpdated();
  public String getFriendlyName();
  public String getAccountSid();
  public String getPhoneNumber();
  public URI getUri();
}

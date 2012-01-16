package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.http.RequestMethod;

public interface IncomingPhoneNumber {
  public String getSid();
  public DateTime getDateCreated();
  public DateTime getDateUpdated();
  public String getFriendlyName();
  public String getAccountSid();
  public String getPhoneNumber();
  public String getApiVersion();
  public boolean hasVoiceCallerIdLookup();
  public URI getVoiceUrl();
  public RequestMethod getVoiceMethod();
  public URI getVoiceFallbackUrl();
  public RequestMethod getVoiceFallbackMethod();
  public URI getStatusCallback();
  public RequestMethod getStatusCallbackMethod();
  public String getVoiceApplicationSid();
  public URI getSmsUrl();
  public RequestMethod getSmsMethod();
  public URI getSmsFallbackUrl();
  public RequestMethod getSmsFallbackMethod();
  public String getSmsApplicationSid();
  public URI getUri();
}

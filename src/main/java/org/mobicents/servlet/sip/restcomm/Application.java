package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;

@Immutable public final class Application {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final Sid accountSid;
  private final String apiVersion;
  private final Boolean hasVoiceCallerIdLookup;
  private final URI voiceUrl;
  private final RequestMethod voiceMethod;
  private final URI voiceFallbackUrl;
  private final RequestMethod voiceFallbackMethod;
  private final URI statusCallback;
  private final RequestMethod statusCallbackMethod;
  private final URI smsUrl;
  private final RequestMethod smsMethod;
  private final URI smsFallbackUrl;
  private final RequestMethod smsFallbackMethod;
  private final URI uri;
  
  public Application(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final Sid accountSid, final String apiVersion, final Boolean hasVoiceCallerIdLookup, final URI voiceUrl,
      final RequestMethod voiceMethod, final URI voiceFallbackUrl, final RequestMethod voiceFallbackMethod,
      final URI statusCallback, final RequestMethod statusCallbackMethod, final URI smsUrl, final RequestMethod smsMethod,
      final URI smsFallbackUrl, final RequestMethod smsFallbackMethod, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.friendlyName = friendlyName;
    this.accountSid = accountSid;
    this.apiVersion = apiVersion;
    this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.voiceFallbackUrl = voiceFallbackUrl;
    this.voiceFallbackMethod = voiceFallbackMethod;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
    this.smsUrl = smsUrl;
    this.smsMethod = smsMethod;
    this.smsFallbackUrl = smsFallbackUrl;
    this.smsFallbackMethod = smsFallbackMethod;
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
  
  public Sid getAccountSid() {
    return accountSid;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }
  
  public Boolean hasVoiceCallerIdLookup() {
    return hasVoiceCallerIdLookup;
  }
  
  public URI getVoiceUrl() {
    return voiceUrl;
  }
  
  public RequestMethod getVoiceMethod() {
    return voiceMethod;
  }
  
  public URI getVoiceFallbackUrl() {
    return voiceFallbackUrl;
  }
  
  public RequestMethod getVoiceFallbackMethod() {
    return voiceFallbackMethod;
  }
  
  public URI getStatusCallback() {
    return statusCallback;
  }
  
  public RequestMethod getStatusCallbackMethod() {
    return statusCallbackMethod;
  }
  
  public URI getSmsUrl() {
    return smsUrl;
  }
  
  public RequestMethod getSmsMethod() {
    return smsMethod;
  }
  
  public URI getSmsFallbackUrl() {
    return smsFallbackUrl;
  }
  
  public RequestMethod getSmsFallbackMethod() {
    return smsFallbackMethod;
  }
  
  public URI getUri() {
    return uri;
  }
}

package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

@Immutable public final class ShortCode {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final Sid accountSid;
  private final Integer shortCode;
  private final String apiVersion;
  private final URI smsUrl;
  private final String smsMethod;
  private final URI smsFallbackUrl;
  private final String smsFallbackMethod;
  private final URI uri;
  
  public ShortCode(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final Sid accountSid, final Integer shortCode, final String apiVersion, final URI smsUrl, final String smsMethod,
      final URI smsFallbackUrl, final String smsFallbackMethod, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.friendlyName = friendlyName;
    this.accountSid = accountSid;
    this.shortCode = shortCode;
    this.apiVersion = apiVersion;
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

  public Integer getShortCode() {
    return shortCode;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public URI getSmsUrl() {
    return smsUrl;
  }

  public String getSmsMethod() {
    return smsMethod;
  }

  public URI getSmsFallbackUrl() {
    return smsFallbackUrl;
  }

  public String getSmsFallbackMethod() {
    return smsFallbackMethod;
  }

  public URI getUri() {
    return uri;
  }
  
  public ShortCode setApiVersion(final String apiVersion) {
	final DateTime dateUpdated = DateTime.now();
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
        smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsUrl(final URI smsUrl) {
    final DateTime dateUpdated = DateTime.now();
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsMethod(final String smsMethod) {
    final DateTime dateUpdated = DateTime.now();
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsFallbackUrl(final URI smsFallbackUrl) {
    final DateTime dateUpdated = DateTime.now();
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsFallbackMethod(final String smsFallbackMethod) {
    final DateTime dateUpdated = DateTime.now();
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
}

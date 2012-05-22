/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class IncomingPhoneNumber {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final Sid accountSid;
  private final String phoneNumber;
  private final String apiVersion;
  private final Boolean hasVoiceCallerIdLookup;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI voiceFallbackUrl;
  private final String voiceFallbackMethod;
  private final URI statusCallback;
  private final String statusCallbackMethod;
  private final Sid voiceApplicationSid;
  private final URI smsUrl;
  private final String smsMethod;
  private final URI smsFallbackUrl;
  private final String smsFallbackMethod;
  private final Sid smsApplicationSid;
  private final URI uri;
  
  public IncomingPhoneNumber(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final Sid accountSid, final String phoneNumber, final String apiVersion, final Boolean hasVoiceCallerIdLookup, 
      final URI voiceUrl, final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod,
      final URI statusCallback, final String statusCallbackMethod, final Sid voiceApplicationSid, final URI smsUrl,
      final String smsMethod, final URI smsFallbackUrl, final String smsFallbackMethod, final Sid smsApplicationSid,
      final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.friendlyName = friendlyName;
    this.accountSid = accountSid;
    this.phoneNumber = phoneNumber;
    this.apiVersion = apiVersion;
    this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.voiceFallbackUrl = voiceFallbackUrl;
    this.voiceFallbackMethod = voiceFallbackMethod;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
    this.voiceApplicationSid = voiceApplicationSid;
    this.smsUrl = smsUrl;
    this.smsMethod = smsMethod;
    this.smsFallbackUrl = smsFallbackUrl;
    this.smsFallbackMethod = smsFallbackMethod;
    this.smsApplicationSid = smsApplicationSid;
    this.uri = uri;
  }
  
  public static Builder builder() {
    return new Builder();
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
  
  public String getPhoneNumber() {
    return phoneNumber;
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
  
  public String getVoiceMethod() {
    return voiceMethod;
  }
  
  public URI getVoiceFallbackUrl() {
    return voiceFallbackUrl;
  }
  
  public String getVoiceFallbackMethod() {
    return voiceFallbackMethod;
  }
  
  public URI getStatusCallback() {
    return statusCallback;
  }
  
  public String getStatusCallbackMethod() {
    return statusCallbackMethod;
  }
  
  public Sid getVoiceApplicationSid() {
    return voiceApplicationSid;
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
  
  public Sid getSmsApplicationSid() {
    return smsApplicationSid;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public IncomingPhoneNumber setApiVersion(final String apiVersion) {
    return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setFriendlyName(final String friendlyName) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceUrl(final URI voiceUrl) {
    return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceMethod(final String voiceMethod) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceFallbackUrl(final URI voiceFallbackUrl) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceFallbackMethod(final String voiceFallbackMethod) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setStatusCallback(final URI statusCallback) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setStatusCallbackMethod(final String statusCallbackMethod) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setVoiceApplicationSid(final Sid voiceApplicationSid) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setSmsUrl(final URI smsUrl) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setSmsMethod(final String smsMethod) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setSmsFallbackUrl(final URI smsFallbackUrl) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setSmsFallbackMethod(final String smsFallbackMethod) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  public IncomingPhoneNumber setSmsApplicationSid(final Sid smsApplicationSid) {
	return new IncomingPhoneNumber(sid, dateCreated, DateTime.now(), friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  @NotThreadSafe public static final class Builder {
    private Sid sid;
    private String friendlyName;
    private Sid accountSid;
    private String phoneNumber;
    private String apiVersion;
    private Boolean hasVoiceCallerIdLookup;
    private URI voiceUrl;
    private String voiceMethod;
    private URI voiceFallbackUrl;
    private String voiceFallbackMethod;
    private URI statusCallback;
    private String statusCallbackMethod;
    private Sid voiceApplicationSid;
    private URI smsUrl;
    private String smsMethod;
    private URI smsFallbackUrl;
    private String smsFallbackMethod;
    private Sid smsApplicationSid;
    private URI uri;
  
    private Builder() {
      super();
    }
    
    public IncomingPhoneNumber build() {
      final DateTime now = DateTime.now();
      return new IncomingPhoneNumber(sid, now, now, friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
          voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid, smsUrl,
          smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
    }
    
    public void setSid(final Sid sid) {
      this.sid = sid;
    }
    
    public void setFriendlyName(final String friendlyName) {
      this.friendlyName = friendlyName;
    }
    
    public void setAccountSid(final Sid accountSid) {
      this.accountSid = accountSid;
    }
    
    public void setPhoneNumber(final String phoneNumber) {
      this.phoneNumber = phoneNumber;
    }
    
    public void setApiVersion(final String apiVersion) {
      this.apiVersion = apiVersion;
    }
    
    public void setHasVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
      this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
    }
    
    public void setVoiceUrl(final URI voiceUrl) {
      this.voiceUrl = voiceUrl;
    }
    
    public void setVoiceMethod(final String voiceMethod) {
      this.voiceMethod = voiceMethod;
    }
    
    public void setVoiceFallbackUrl(final URI voiceFallbackUrl) {
      this.voiceFallbackUrl = voiceFallbackUrl;
    }
    
    public void setVoiceFallbackMethod(final String voiceFallbackMethod) {
      this.voiceFallbackMethod = voiceFallbackMethod;
    }
    
    public void setStatusCallback(final URI statusCallback) {
      this.statusCallback = statusCallback;
    }
    
    public void setStatusCallbackMethod(final String statusCallbackMethod) {
      this.statusCallbackMethod = statusCallbackMethod;
    }
    
    public void setVoiceApplicationSid(final Sid voiceApplicationSid) {
      this.voiceApplicationSid = voiceApplicationSid;
    }
    
    public void setSmsUrl(final URI smsUrl) {
      this.smsUrl = smsUrl;
    }
    
    public void setSmsMethod(final String smsMethod) {
      this.smsMethod = smsMethod;
    }
    
    public void setSmsFallbackUrl(final URI smsFallbackUrl) {
      this.smsFallbackUrl = smsFallbackUrl;
    }
    
    public void setSmsFallbackMethod(final String smsFallbackMethod) {
      this.smsFallbackMethod = smsFallbackMethod;
    }
    
    public void setSmsApplicationSid(final Sid smsApplicationSid) {
      this.smsApplicationSid = smsApplicationSid;
    }
    
    public void setUri(final URI uri) {
      this.uri = uri;
    }
  }
}

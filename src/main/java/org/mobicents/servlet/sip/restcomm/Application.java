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
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */

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
  
  public Application setDateUpdated(final DateTime dateUpdated) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
        voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
        smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setFriendlyName(final String friendlyName) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setVoiceUrl(final URI voiceUrl) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setVoiceMethod(final RequestMethod voiceMethod) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setVoiceFallbackUrl(final URI voiceFallbackUrl) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setVoiceFallbackMethod(final RequestMethod voiceFallbackMethod) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setStatusCallback(final URI statusCallback) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setStatusCallbackMethod(final RequestMethod statusCallbackMethod) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setSmsUrl(final URI smsUrl) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setSmsMethod(final RequestMethod smsMethod) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setSmsFallbackUrl(final URI smsFallbackUrl) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public Application setSmsFallbackMethod(final RequestMethod smsFallbackMethod) {
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
}

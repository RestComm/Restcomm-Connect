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
package org.mobicents.servlet.sip.restcomm.entities;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * Represents a RestComm application
 * 
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */

@Immutable public final class Application {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final Sid accountSid;
  private final String apiVersion;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI voiceFallbackUrl;
  private final String voiceFallbackMethod;
  private final URI statusCallback;
  private final String statusCallbackMethod;
  private final Boolean hasVoiceCallerIdLookup;
  private final URI smsUrl;
  private final String smsMethod;
  private final URI smsFallbackUrl;
  private final String smsFallbackMethod;
  private final URI smsStatusCallback;
  private final URI uri;
  
  public Application(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final Sid accountSid, final String apiVersion, final URI voiceUrl, final String voiceMethod, final URI voiceFallbackUrl,
      final String voiceFallbackMethod, final URI statusCallback, final String statusCallbackMethod, final Boolean hasVoiceCallerIdLookup,
      final URI smsUrl, final String smsMethod, final URI smsFallbackUrl, final String smsFallbackMethod, final URI smsStatusCallback,
      final URI uri) {
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
    this.smsStatusCallback = smsStatusCallback;
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
  
  public URI getSmsStatusCallback() {
    return smsStatusCallback;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public Application setFriendlyName(final String friendlyName) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setVoiceUrl(final URI voiceUrl) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setVoiceMethod(final String voiceMethod) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setVoiceFallbackUrl(final URI voiceFallbackUrl) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setVoiceFallbackMethod(final String voiceFallbackMethod) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setStatusCallback(final URI statusCallback) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setStatusCallbackMethod(final String statusCallbackMethod) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setSmsUrl(final URI smsUrl) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setSmsMethod(final String smsMethod) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setSmsFallbackUrl(final URI smsFallbackUrl) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setSmsFallbackMethod(final String smsFallbackMethod) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
  
  public Application setSmsStatusCallback(final URI smsStatusCallback) {
    return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
    	voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
  }
}

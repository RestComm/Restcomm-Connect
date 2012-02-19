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
@Immutable public final class IncomingPhoneNumber {
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
  
  public IncomingPhoneNumber(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
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
  
  public IncomingPhoneNumber setFriendlyName(final String friendlyName) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setVoiceUrl(final URI voiceUrl) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setVoiceMethod(final RequestMethod voiceMethod) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setVoiceFallbackUrl(final URI voiceFallbackUrl) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setVoiceFallbackMethod(final RequestMethod voiceFallbackMethod) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setStatusCallback(final URI statusCallback) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setStatusCallbackMethod(final RequestMethod statusCallbackMethod) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setSmsUrl(final URI smsUrl) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setSmsMethod(final RequestMethod smsMethod) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setSmsFallbackUrl(final URI smsFallbackUrl) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public IncomingPhoneNumber setSmsFallbackMethod(final RequestMethod smsFallbackMethod) {
	final DateTime dateUpdated = DateTime.now();
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
	    voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, smsUrl,
	    smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
}

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

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class SandBox {
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String pin;
  private final Sid accountSid;
  private final String phoneNumber;
  private final Sid applicationSid;
  private final String apiVersion;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI smsUrl;
  private final String smsMethod;
  private final URI statusCallback;
  private final String statusCallbackMethod;
  private final URI uri;

  public SandBox(final DateTime dateCreated, final DateTime dateUpdated, final String pin, final Sid accountSid,
      final String phoneNumber, final Sid applicationSid, final String apiVersion, final URI voiceUrl, final String voiceMethod,
      final URI smsUrl, final String smsMethod, final URI statusCallback, final String statusCallbackMethod, final URI uri) {
    super();
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.pin = pin;
    this.accountSid = accountSid;
    this.phoneNumber = phoneNumber;
    this.applicationSid = applicationSid;
    this.apiVersion = apiVersion;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.smsUrl = smsUrl;
    this.smsMethod = smsMethod;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
    this.uri = uri;
  }

  public DateTime getDateCreated() {
    return dateCreated;
  }

  public DateTime getDateUpdated() {
    return dateUpdated;
  }

  public String getPin() {
    return pin;
  }

  public Sid getAccountSid() {
    return accountSid;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public Sid getApplicationSid() {
    return applicationSid;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public URI getVoiceUrl() {
    return voiceUrl;
  }

  public String getVoiceMethod() {
    return voiceMethod;
  }

  public URI getSmsUrl() {
    return smsUrl;
  }

  public String getSmsMethod() {
    return smsMethod;
  }

  public URI getStatusCallback() {
    return statusCallback;
  }

  public String getStatusCallbackMethod() {
    return statusCallbackMethod;
  }

  public URI getUri() {
    return uri;
  }
  
  public SandBox setVoiceUrl(final URI voiceUrl) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
  
  public SandBox setVoiceMethod(final String voiceMethod) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
  
  public SandBox setSmsUrl(final URI smsUrl) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
  
  public SandBox setSmsMethod(final String smsMethod) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
  
  public SandBox setStatusCallback(final URI statusCallback) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
  
  public SandBox setStatusCallbackMethod(final String statusCallbackMethod) {
    return new SandBox(dateCreated, DateTime.now(), pin, accountSid, phoneNumber, applicationSid, apiVersion, voiceUrl, voiceMethod,
        smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
}

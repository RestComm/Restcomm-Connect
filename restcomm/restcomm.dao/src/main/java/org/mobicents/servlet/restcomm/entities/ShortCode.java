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
package org.mobicents.servlet.restcomm.entities;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
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
    return new ShortCode(sid, dateCreated, DateTime.now(), friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
        smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsUrl(final URI smsUrl) {
    return new ShortCode(sid, dateCreated, DateTime.now(), friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsMethod(final String smsMethod) {
    return new ShortCode(sid, dateCreated, DateTime.now(), friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsFallbackUrl(final URI smsFallbackUrl) {
    return new ShortCode(sid, dateCreated, DateTime.now(), friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
  
  public ShortCode setSmsFallbackMethod(final String smsFallbackMethod) {
    return new ShortCode(sid, dateCreated, DateTime.now(), friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
	    smsFallbackUrl, smsFallbackMethod, uri);
  }
}

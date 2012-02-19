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
import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Notification {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final Sid callSid;
  private final String apiVersion;
  private final Integer log;
  private final Integer errorCode;
  private final URI moreInfo;
  private final String messageText;
  private final DateTime messageDate;
  private final URI requestUrl;
  private final RequestMethod requestMethod;
  private final String requestVariables;
  private final List<String> requestHeaders;
  private final String responseBody;
  private final URI uri;
  
  public Notification(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final Sid callSid, final String apiVersion, final Integer log, final Integer errorCode, final URI moreInfo, String messageText,
      final DateTime messageDate, final URI requestUrl, final RequestMethod requestMethod, final String requestVariables,
      final List<String> requestHeaders, final String responseBody, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.callSid = callSid;
    this.apiVersion = apiVersion;
    this.log = log;
    this.errorCode = errorCode;
    this.moreInfo = moreInfo;
    this.messageText = messageText;
    this.messageDate = messageDate;
    this.requestUrl = requestUrl;
    this.requestMethod = requestMethod;
    this.requestVariables = requestVariables;
    this.requestHeaders = requestHeaders;
    this.responseBody = responseBody;
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
  
  public Sid getAccountSid() {
    return accountSid;
  }
  
  public Sid getCallSid() {
    return callSid;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }
  
  public Integer getLog() {
    return log;
  }
  
  public Integer getErrorCode() {
    return errorCode;
  }
  
  public URI getMoreInfo() {
    return moreInfo;
  }
  
  public String getMessageText() {
    return messageText;
  }
  
  public DateTime getMessageDate() {
    return messageDate;
  }
  
  public URI getRequestUrl() {
    return requestUrl;
  }
  
  public RequestMethod getRequestMethod() {
    return requestMethod;
  }
  
  public String getRequestVariables() {
    return requestVariables;
  }
  
  public List<String> getRequestHeaders() {
    return requestHeaders;
  }
  
  public String getResponseBody() {
    return responseBody;
  }
  
  public URI getUri() {
    return uri;
  }
}

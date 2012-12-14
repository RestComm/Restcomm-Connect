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
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.net.URI;
import java.util.List;

import org.apache.http.NameValuePair;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class RcmlInterpreterContext {
  protected final Sid accountSid;
  protected final String apiVersion;
  protected final URI statusCallback;
  protected final String statusCallbackMethod;
  
  public RcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI statusCallback,
      final String statusCallbackMethod) {
    super();
    this.accountSid = accountSid;
    this.apiVersion = apiVersion;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
  }
  
  public Sid getAccountSid() {
    return accountSid;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }
  
  public abstract String getFrom();
  
  public abstract List<NameValuePair> getRcmlRequestParameters();
  
  public URI getStatusCallback() {
    return statusCallback;
  }
  
  public String getStatusCallbackMethod() {
    return statusCallbackMethod;
  }
  
  public abstract String getTo();
}

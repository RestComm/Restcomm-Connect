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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.media.api.Call;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RcmlInterpreterContext {
  private final Call call;
  
  private final Sid accountSid;
  private final String apiVersion;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI voiceFallbackUrl;
  private final String voiceFallbackMethod;
  private final URI statusCallback;
  private final String statusCallbackMethod;
  private final Integer timeout;
  
  public RcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
      final URI statusCallback, final String statusCallbackMethod, final Call call) {
	  this(accountSid, apiVersion, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod,
	      statusCallback, statusCallbackMethod, null, call);
  }
  
  public RcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
	  final URI statusCallback, final String statusCallbackMethod, final Integer timeout, final Call call) {
    super();
    this.call = call;
    this.accountSid = accountSid;
    this.apiVersion = apiVersion;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.voiceFallbackUrl = voiceFallbackUrl;
    this.voiceFallbackMethod = voiceFallbackMethod;
    this.statusCallback = statusCallback;
    this.statusCallbackMethod = statusCallbackMethod;
    this.timeout = timeout;
  }
  
  public Sid getAccountSid() {
    return accountSid;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }
  
  public Call getCall() {
	return call;
  }
  
  public List<NameValuePair> getRcmlRequestParameters() {
    final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    parameters.add(new BasicNameValuePair("CallSid", call.getSid().toString()));
    parameters.add(new BasicNameValuePair("AccountSid", accountSid.toString()));
    parameters.add(new BasicNameValuePair("From", call.getOriginator()));
    parameters.add(new BasicNameValuePair("To", call.getRecipient()));
    parameters.add(new BasicNameValuePair("CallStatus", call.getStatus().toString()));
    parameters.add(new BasicNameValuePair("ApiVersion", apiVersion));
    parameters.add(new BasicNameValuePair("Direction", call.getDirection().toString()));
    parameters.add(new BasicNameValuePair("ForwardedFrom", call.getForwardedFrom()));
    parameters.add(new BasicNameValuePair("CallerName", call.getOriginatorName()));
    return parameters;
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
  
  public Integer getTimeout() {
    return timeout;
  }
}

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
import org.mobicents.servlet.sip.restcomm.entities.Application;
import org.mobicents.servlet.sip.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.media.api.Call;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RcmlInterpreterContext {
  private final Application application;
  private final IncomingPhoneNumber incomingPhoneNumber;
  private final Call call;
  
  private final Sid accountSid;
  private final String apiVersion;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI voiceFallbackUrl;
  private final String voiceFallbackMethod;
  
  public RcmlInterpreterContext(final Application application, final IncomingPhoneNumber incomingPhoneNumber,
      final Call call) {
    super();
    this.application = application;
    this.incomingPhoneNumber = incomingPhoneNumber;
    this.call = call;
    if(application != null) {
      this.accountSid = application.getAccountSid();
      this.apiVersion = application.getApiVersion();
      this.voiceUrl = application.getVoiceUrl();
      this.voiceMethod = application.getVoiceMethod();
      this.voiceFallbackUrl = application.getVoiceFallbackUrl();
      this.voiceFallbackMethod = application.getVoiceFallbackMethod();
    } else {
      this.accountSid = incomingPhoneNumber.getAccountSid();
      this.apiVersion = incomingPhoneNumber.getApiVersion();
      this.voiceUrl = incomingPhoneNumber.getVoiceUrl();
      this.voiceMethod = incomingPhoneNumber.getVoiceMethod();
      this.voiceFallbackUrl = incomingPhoneNumber.getVoiceFallbackUrl();
      this.voiceFallbackMethod = incomingPhoneNumber.getVoiceFallbackMethod();
    }
  }
  
  public RcmlInterpreterContext(final Application application, final Call call) {
    super();
    this.application = application;
    this.incomingPhoneNumber = null;
    this.call = call;
    this.accountSid = application.getAccountSid();
    this.apiVersion = application.getApiVersion();
    this.voiceUrl = application.getVoiceUrl();
    this.voiceMethod = application.getVoiceMethod();
    this.voiceFallbackUrl = application.getVoiceFallbackUrl();
    this.voiceFallbackMethod = application.getVoiceFallbackMethod();
  }
  
  public RcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
      final Call call) {
    super();
    this.application = null;
    this.incomingPhoneNumber = null;
    this.call = call;
    this.accountSid = accountSid;
    this.apiVersion = apiVersion;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.voiceFallbackUrl = voiceFallbackUrl;
    this.voiceFallbackMethod = voiceFallbackMethod;
  }
  
  public Sid getAccountSid() {
    return accountSid;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }
  
  public Application getApplication() {
    return application;
  }
  
  public IncomingPhoneNumber getIncomingPhoneNumber() {
    return incomingPhoneNumber;
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
}

package org.mobicents.servlet.sip.restcomm.interpreter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.media.api.Call;

public class VoiceRcmlInterpreterContext extends RcmlInterpreterContext {
  private final Call call;
  private final URI voiceUrl;
  private final String voiceMethod;
  private final URI voiceFallbackUrl;
  private final String voiceFallbackMethod;
  private final Integer timeout;

  public VoiceRcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
      final URI statusCallback, final String statusCallbackMethod, final Call call) {
	this(accountSid, apiVersion, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod,
        statusCallback, statusCallbackMethod, null, call);
  }

  public VoiceRcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
	  final URI statusCallback, final String statusCallbackMethod, final Integer timeout, final Call call) {
    super(accountSid, apiVersion, statusCallback, statusCallbackMethod);
    this.call = call;
    this.voiceUrl = voiceUrl;
    this.voiceMethod = voiceMethod;
    this.voiceFallbackUrl = voiceFallbackUrl;
    this.voiceFallbackMethod = voiceFallbackMethod;
    this.timeout = timeout;
  }
  
  public Call getCall() {
	return call;
  }
  
  public String getFrom() {
    return call.getOriginator();
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
  
  public Integer getTimeout() {
    return timeout;
  }
  
  public String getTo() {
    return call.getRecipient();
  }
}

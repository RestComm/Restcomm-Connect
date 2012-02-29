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

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;

public final class RcmlInterpreterContext {
  private final Call call;
  private URI voiceUrl;
  private String voiceMethod;
  private URI voiceFallbackUrl;
  private String voiceFallbackMethod;
  
  public RcmlInterpreterContext(final Call call) {
    super();
    this.call = call;
    if("inbound".equals(call.getDirection())); {
      final ServiceLocator services = ServiceLocator.getInstance();
      final DaoManager daos = services.get(DaoManager.class);
      final IncomingPhoneNumbersDao incomingPhoneNumbersDao = daos.getIncomingPhoneNumbersDao();
      final IncomingPhoneNumber incomingPhoneNumber = incomingPhoneNumbersDao.getIncomingPhoneNumber(call.getRecipient());
      final Sid voiceApplicationSid = incomingPhoneNumber.getVoiceApplicationSid();
      if(voiceApplicationSid != null) {
        final ApplicationsDao applicationsDao = daos.getApplicationsDao();
        final Application application = applicationsDao.getApplication(voiceApplicationSid);
        voiceUrl = application.getVoiceUrl();
        voiceMethod = application.getVoiceMethod();
        voiceFallbackUrl = application.getVoiceFallbackUrl();
        voiceFallbackMethod = application.getVoiceFallbackMethod();
      } else {
        voiceUrl = incomingPhoneNumber.getVoiceUrl();
        voiceMethod = incomingPhoneNumber.getVoiceMethod();
        voiceFallbackUrl = incomingPhoneNumber.getVoiceFallbackUrl();
        voiceFallbackMethod = incomingPhoneNumber.getVoiceFallbackMethod();
      }
    }
  }
  
  public IncomingPhoneNumber getIncomingPhoneNumber() {
    return null;
  }
  
  public Call getCall() {
	return call;
  }
  
  public Application getSmsApplication() {
    return null;
  }
  
  public Application getVoiceApplication() {
    return null;
  }
  
  public List<NameValuePair> getRcmlRequestParameters() {
    final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
    parameters.add(new BasicNameValuePair("CallSid", call.getSid().toString()));
    //parameters.add(new BasicNameValuePair("AccountSid", call.getAccountSid().toString()));
    parameters.add(new BasicNameValuePair("From", call.getOriginator()));
    parameters.add(new BasicNameValuePair("To", call.getRecipient()));
    parameters.add(new BasicNameValuePair("CallStatus", call.getStatus().toString()));
    //parameters.add(new BasicNameValuePair("ApiVersion", call.getApiVersion()));
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

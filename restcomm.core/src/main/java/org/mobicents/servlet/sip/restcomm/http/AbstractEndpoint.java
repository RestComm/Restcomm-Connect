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
package org.mobicents.servlet.sip.restcomm.http;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class AbstractEndpoint {
  protected final String baseRecordingsPath;
  
  public AbstractEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    baseRecordingsPath = StringUtils.addSuffixIfNotPresent(configuration.getString("recordings-path"), "/");
  }
  
  protected String getApiVersion(final MultivaluedMap<String, String> data) {
    String apiVersion = "2012-04-24";
    if(data != null && data.containsKey("ApiVersion")) {
     apiVersion = data.getFirst("ApiVersion");
    }
    return apiVersion;
  }
  
  protected PhoneNumber getPhoneNumber(final MultivaluedMap<String, String> data) {
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    PhoneNumber phoneNumber = null;
    try { phoneNumber = phoneNumberUtil.parse(data.getFirst("PhoneNumber"), "US"); }
    catch(final NumberParseException ignored) { }
    return phoneNumber;
  }
  
  protected String getMethod(final String name, final MultivaluedMap<String, String> data) {
    String method = "POST";
    if(data.containsKey(name)) {
      method = data.getFirst(name);
    }
    return method;
  }
  
  protected Sid getSid(final String name, final MultivaluedMap<String, String> data) {
    Sid sid = null;
    if(data.containsKey(name)) {
      sid = new Sid(data.getFirst(name));
    }
    return sid;
  }
  
  protected URI getUrl(final String name, final MultivaluedMap<String, String> data) {
    URI uri = null;
    if(data.containsKey(name)) {
      uri = URI.create(data.getFirst(name));
    }
    return uri;
  }
  
  protected boolean getHasVoiceCallerIdLookup(final MultivaluedMap<String, String> data) {
    boolean hasVoiceCallerIdLookup = false;
    if(data.containsKey("VoiceCallerIdLookup")) {
      final String value = data.getFirst("VoiceCallerIdLookup");
      if("true".equalsIgnoreCase(value)) {
        return true;
      }
    }
    return hasVoiceCallerIdLookup;
  }
  
  protected void secure(final Sid accountSid, final String permission) throws AuthorizationException {
    final Subject subject = SecurityUtils.getSubject();
    if(subject.hasRole("Administrator") || (subject.getPrincipal().equals(accountSid) &&
	    subject.isPermitted(permission))) {
	  return;
    } else {
      throw new AuthorizationException();
    }
  }
}

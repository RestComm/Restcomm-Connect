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
package org.mobicents.servlet.restcomm.sms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class IPSMService extends UntypedActor {
  private final ActorSystem system;
  private final SipFactory factory;
  private final DaoManager storage;
  
  private final Map<String, ActorRef> sessions;
  
  public IPSMService(final ActorSystem system, final SipFactory factory,
      final DaoManager storage) {
    super();
    this.system = system;
    this.factory = factory;
    this.storage = storage;
    this.sessions = new HashMap<String, ActorRef>();
  }
  
  private void message(final Object message) throws IOException {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    // Tell the sender we received the message okay.
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_ACCEPTED);
    response.send();
    // Handle the SMS message.
    SipURI uri = (SipURI)request.getFrom().getURI();
    final String from = uri.getUser();
    uri = (SipURI)request.getRequestURI();
    final String to = uri.getUser();
    // There is no existing session so create a new one.
    try {
      // Format the destination to an E.164 phone number.
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      final String phone = phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"),
          PhoneNumberFormat.E164);
      // Try to find an application defined for the phone number.
      final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
      final IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(phone);
      if(number != null) {
        final Sid sid = number.getVoiceApplicationSid();
        if(sid != null) {
          final ApplicationsDao applications = storage.getApplicationsDao();
          final Application application = applications.getApplication(sid);
        }
        final ActorRef session = null;
        session.tell(request, self);
        return;
      }
    } catch(final NumberParseException ignored) { }
  }
  
  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    if(SipServletRequest.class.equals(klass)) {
      final SipServletRequest request = (SipServletRequest)message;
      final String method = request.getMethod();
      if("MESSAGE".equalsIgnoreCase(method)) {
        message(message);
      }
    }
  }
  
  private ActorRef session(final SipFactory factory, final String service) {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new IPSMSession(factory, service, null);
		}
    }));
  }
}
